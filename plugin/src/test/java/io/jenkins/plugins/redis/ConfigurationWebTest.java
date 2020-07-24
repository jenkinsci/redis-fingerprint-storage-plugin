/*
 * The MIT License
 *
 * Copyright (c) 2020, Sumit Sarin and Jenkins project contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.redis;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.testcontainers.containers.GenericContainer;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ConfigurationWebTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public GenericContainer redis = new GenericContainer<>("redis:6.0.4-alpine").withExposedPorts(6379);

    @Test
    public void changingRedisConfigAndRefreshWebUITest() throws Exception {

        final String initialHost = "initialHost";
        final Integer initialPort = 1000;
        final Integer initialConnectionTimeout = 1000;
        final Integer initialSocketTimeout = 1000;
        final Integer initialDatabase = 1;
        final boolean initialSsl = false;

        final String finalHost = "finalHost";
        final Integer finalPort = 3000;
        final Integer finalConnectionTimeout = 3000;
        final Integer finalSocketTimeout = 3000;
        final Integer finalDatabase = 3;
        final Boolean finalSsl = true;

        RedisConfiguration.setConfiguration(initialHost, initialPort, initialConnectionTimeout, initialSocketTimeout,
                "", initialDatabase, initialSsl);

        JenkinsRule.WebClient webClient = j.createWebClient();
        webClient.getOptions().setCssEnabled(false);

        HtmlPage configPage = webClient.goTo("configure");
        HtmlForm form = configPage.getFormByName("config");

        assertThat(form.getInputsByName("_.host").size(), is(1));
        form.getInputsByName("_.host").get(0).setValueAttribute(finalHost);
        assertThat(form.getInputsByName("_.port").size(), is(1));
        form.getInputsByName("_.port").get(0).setValueAttribute(finalPort.toString());
        assertThat(form.getInputsByName("_.ssl").size(), is(1));
        form.getInputsByName("_.ssl").get(0).setChecked(finalSsl);
        assertThat(form.getInputsByName("_.database").size(), is(1));
        form.getInputsByName("_.database").get(0).setValueAttribute(finalDatabase.toString());
        assertThat(form.getInputsByName("_.connectionTimeout").size(), is(1));
        form.getInputsByName("_.connectionTimeout").get(0).setValueAttribute(finalConnectionTimeout.toString());
        assertThat(form.getInputsByName("_.socketTimeout").size(), is(1));
        form.getInputsByName("_.socketTimeout").get(0).setValueAttribute(finalSocketTimeout.toString());

        j.submit(form);

        configPage = webClient.goTo("configure");
        form = configPage.getFormByName("config");

        assertThat(form.getInputsByName("_.host").size(), is(1));
        assertThat(form.getInputsByName("_.host").get(0).getValueAttribute(), is(finalHost));
        assertThat(form.getInputsByName("_.port").size(), is(1));
        assertThat(form.getInputsByName("_.port").get(0).getValueAttribute(), is(finalPort.toString()));
        assertThat(form.getInputsByName("_.ssl").size(), is(1));
        assertThat(form.getInputsByName("_.ssl").get(0).getCheckedAttribute(), is(finalSsl.toString()));
        assertThat(form.getInputsByName("_.database").size(), is(1));
        assertThat(form.getInputsByName("_.database").get(0).getValueAttribute(), is(finalDatabase.toString()));
        assertThat(form.getInputsByName("_.connectionTimeout").size(), is(1));
        assertThat(form.getInputsByName("_.connectionTimeout").get(0).getValueAttribute(),
                is(finalConnectionTimeout.toString()));
        assertThat(form.getInputsByName("_.socketTimeout").size(), is(1));
        assertThat(form.getInputsByName("_.socketTimeout").get(0).getValueAttribute(),
                is(finalSocketTimeout.toString()));
    }

    @Test
    public void fileStorageToRedisStorageWebUITest() throws Exception {
        final String host = redis.getHost();
        final Integer port = redis.getFirstMappedPort();
        final Integer connectionTimeout = 3000;
        final Integer socketTimeout = 3000;
        final String credentialsId = "";
        final Integer database = 3;
        final boolean ssl = false;

        JenkinsRule.WebClient web = j.createWebClient();
        HtmlForm form = web.goTo("configure").getFormByName("config");

        assertThat(form.getInputsByName("_.host").size(), is(0));
        assertThat(form.getInputsByName("_.port").size(), is(0));
        assertThat(form.getInputsByName("_.ssl").size(), is(0));
        assertThat(form.getInputsByName("_.database").size(), is(0));
        assertThat(form.getInputsByName("_.connectionTimeout").size(), is(0));
        assertThat(form.getInputsByName("_.socketTimeout").size(), is(0));
        assertThat(form.getInputsByName("_.credentialsId").size(), is(0));

        RedisConfiguration.setConfiguration(host, port, connectionTimeout, socketTimeout, credentialsId, database, ssl);

        form = web.goTo("configure").getFormByName("config");

        assertThat(form.getInputsByName("_.host").size(), is(1));
        assertThat(form.getInputsByName("_.host").get(0).getValueAttribute(), is(host));
        assertThat(form.getInputsByName("_.port").size(), is(1));
        assertThat(form.getInputsByName("_.port").get(0).getValueAttribute(), is(port.toString()));
        assertThat(form.getInputsByName("_.ssl").size(), is(1));
        assertThat(form.getInputsByName("_.ssl").get(0).getCheckedAttribute(), is(""));
        assertThat(form.getInputsByName("_.database").size(), is(1));
        assertThat(form.getInputsByName("_.database").get(0).getValueAttribute(), is(database.toString()));
        assertThat(form.getInputsByName("_.connectionTimeout").size(), is(1));
        assertThat(form.getInputsByName("_.connectionTimeout").get(0).getValueAttribute(), is(
                connectionTimeout.toString()));
        assertThat(form.getInputsByName("_.socketTimeout").size(), is(1));
        assertThat(form.getInputsByName("_.socketTimeout").get(0).getValueAttribute(), is(socketTimeout.toString()));
        assertThat(form.getSelectByName("_.credentialsId").getSelectedIndex(), is(0));
    }

    @Test
    public void testTestRedisConnection() throws IOException, ServletException {
        RedisFingerprintStorage.DescriptorImpl descriptor = new RedisFingerprintStorage.DescriptorImpl();
        FormValidation result = descriptor.doTestRedisConnection("", 0, 0, false, "", 2000, 2000);
        assertThat(result.kind, is(FormValidation.Kind.ERROR));

        result = descriptor.doTestRedisConnection(redis.getHost(), redis.getFirstMappedPort(), 0, false,
                "", 2000, 2000);
        assertThat(result.kind, is(FormValidation.Kind.OK));

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy());

        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName("dev"))) {
            assertThat(j.jenkins.hasPermission(Jenkins.ADMINISTER), is(false));
            result = descriptor.doTestRedisConnection(redis.getHost(), redis.getFirstMappedPort(), 0, false,
                    "", 2000, 2000);
            assertThat(result.kind, is(FormValidation.Kind.ERROR));
        }
    }

}
