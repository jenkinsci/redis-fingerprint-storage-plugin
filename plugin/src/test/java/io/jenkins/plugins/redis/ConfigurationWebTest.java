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

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import jenkins.fingerprints.GlobalFingerprintConfiguration;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.testcontainers.containers.GenericContainer;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ConfigurationWebTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public GenericContainer redis = new GenericContainer<>("redis:6.0.4-alpine").withExposedPorts(6379);

    @Test
    public void submitAndReloadConfigurationPageTest() throws Exception {
        JenkinsRule.WebClient web = j.createWebClient();
        HtmlForm form = web.goTo("configure").getFormByName("config");

        assertThat(form.getInputsByName("_.host").size(), is(1));
        form.getInputsByName("_.host").get(0).setValueAttribute("local");
        assertThat(form.getInputsByName("_.port").size(), is(1));
        form.getInputsByName("_.port").get(0).setValueAttribute("3333");
        assertThat(form.getInputsByName("_.ssl").size(), is(1));
        form.getInputsByName("_.ssl").get(0).setChecked(false);
        assertThat(form.getInputsByName("_.database").size(), is(1));
        form.getInputsByName("_.database").get(0).setValueAttribute("3");
        assertThat(form.getInputsByName("_.connectionTimeout").size(), is(1));
        form.getInputsByName("_.connectionTimeout").get(0).setValueAttribute("3");
        assertThat(form.getInputsByName("_.socketTimeout").size(), is(1));
        form.getInputsByName("_.socketTimeout").get(0).setValueAttribute("3");

        j.submit(form);

        form = web.goTo("configure").getFormByName("config");

        assertThat(form.getInputsByName("_.host").size(), is(1));
        assertThat(form.getInputsByName("_.host").get(0).getValueAttribute(), is("local"));
        assertThat(form.getInputsByName("_.port").size(), is(1));
        assertThat(form.getInputsByName("_.port").get(0).getValueAttribute(), is("3333"));
        assertThat(form.getInputsByName("_.ssl").size(), is(1));
        assertThat(form.getInputsByName("_.ssl").get(0).getCheckedAttribute(), is(""));
        assertThat(form.getInputsByName("_.database").size(), is(1));
        assertThat(form.getInputsByName("_.database").get(0).getValueAttribute(), is("3"));
        assertThat(form.getInputsByName("_.connectionTimeout").size(), is(1));
        assertThat(form.getInputsByName("_.connectionTimeout").get(0).getValueAttribute(), is("3"));
        assertThat(form.getInputsByName("_.socketTimeout").size(), is(1));
        assertThat(form.getInputsByName("_.socketTimeout").get(0).getValueAttribute(), is("3"));

        web.goTo("configure").refresh();

        form = web.goTo("configure").getFormByName("config");

        assertThat(form.getInputsByName("_.host").size(), is(1));
        assertThat(form.getInputsByName("_.host").get(0).getValueAttribute(), is("local"));
        assertThat(form.getInputsByName("_.port").size(), is(1));
        assertThat(form.getInputsByName("_.port").get(0).getValueAttribute(), is("3333"));
        assertThat(form.getInputsByName("_.ssl").size(), is(1));
        assertThat(form.getInputsByName("_.ssl").get(0).getCheckedAttribute(), is(""));
        assertThat(form.getInputsByName("_.database").size(), is(1));
        assertThat(form.getInputsByName("_.database").get(0).getValueAttribute(), is("3"));
        assertThat(form.getInputsByName("_.connectionTimeout").size(), is(1));
        assertThat(form.getInputsByName("_.connectionTimeout").get(0).getValueAttribute(), is("3"));
        assertThat(form.getInputsByName("_.socketTimeout").size(), is(1));
        assertThat(form.getInputsByName("_.socketTimeout").get(0).getValueAttribute(), is("3"));
    }


    @Test
    public void changesToRedisConfigCauseChangesOnWebUI() throws Exception {
        RedisConfiguration.setConfiguration("host", 3333, 3, 3, "default", "", 3, false);

        JenkinsRule.WebClient web = j.createWebClient();
        HtmlForm form = web.goTo("configure").getFormByName("config");
        j.submit(form);

        RedisFingerprintStorage redisFingerprintStorage = (RedisFingerprintStorage) GlobalFingerprintConfiguration.get()
                .getFingerprintStorage();

        assertThat(form.getInputsByName("_.host").size(), is(1));
        assertThat(form.getInputsByName("_.host").get(0).getValueAttribute(), is("host"));
        assertThat(form.getInputsByName("_.port").size(), is(1));
        assertThat(form.getInputsByName("_.port").get(0).getValueAttribute(), is("3333"));
        assertThat(form.getInputsByName("_.ssl").size(), is(1));
        assertThat(form.getInputsByName("_.ssl").get(0).getCheckedAttribute(), is(""));
        assertThat(form.getInputsByName("_.database").size(), is(1));
        assertThat(form.getInputsByName("_.database").get(0).getValueAttribute(), is("3"));
        assertThat(form.getInputsByName("_.connectionTimeout").size(), is(1));
        assertThat(form.getInputsByName("_.connectionTimeout").get(0).getValueAttribute(), is("3"));
        assertThat(form.getInputsByName("_.socketTimeout").size(), is(1));
        assertThat(form.getInputsByName("_.socketTimeout").get(0).getValueAttribute(), is("3"));

//        redisFingerprintStorage.setSocketTimeout(3000);
//        redisFingerprintStorage.setConnectionTimeout(3000);
//        redisFingerprintStorage.setCredentialsId("dummy");
//        redisFingerprintStorage.setDatabase(0);
//        redisFingerprintStorage.setHost("dummy");
//        redisFingerprintStorage.setPort(3333);
//        redisFingerprintStorage.setSsl(true);
//
//        GlobalFingerprintConfiguration.get().save();
//        GlobalFingerprintConfiguration.get().load();
//
//        form = web.goTo("configure").getFormByName("config");
//        j.submit(form);
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
