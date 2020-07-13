/*
 * The MIT License
 *
 * Copyright (c) 2020, Jenkins project contributors
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

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import hudson.Util;
import hudson.model.Fingerprint;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.GenericContainer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

public class RedisAuthenticationTest {

    @Rule
    public GenericContainer redis = new GenericContainer<>("redis:6.0.4-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server --requirepass redis_password");

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testWithoutPasswordWhenPasswordIsConfigured() throws Exception {
        String id = Util.getDigestOf("testWithIncorrectPasswordWhenPasswordIsConfigured");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        assertThat(Fingerprint.load(id), is(not(nullValue())));
    }

    @Test
    public void testFingerprintOperationsWithDefaultUsernameAndStandardPassword() throws Exception {
        StandardUsernamePasswordCredentials credential = new UsernamePasswordCredentialsImpl(
                CredentialsScope.SYSTEM,
                "credentialId",
                null,
                "default",
                "redis_password"
        );

        SystemCredentialsProvider.getInstance().getCredentials().add(credential);
        SystemCredentialsProvider.getInstance().save();

        JenkinsRule.WebClient web = j.createWebClient();
        HtmlForm form = web.goTo("configure").getFormByName("config");
        form.getSelectByName("_.credentialsId").setSelectedIndex(1);

        j.submit(form);

        String id = Util.getDigestOf("testFingerprintOperationsWithAuthentication");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        assertThat(Fingerprint.load(id), is(not(nullValue())));
        Fingerprint.delete(id);
        assertThat(Fingerprint.load(id), is(nullValue()));
    }

    @Test
    public void testFingerprintOperationsWithEmptyUsernameAndStandardPassword() throws Exception {
        StandardUsernamePasswordCredentials credential = new UsernamePasswordCredentialsImpl(
                CredentialsScope.SYSTEM,
                "credentialId",
                null,
                "",
                "redis_password"
        );

        SystemCredentialsProvider.getInstance().getCredentials().add(credential);
        SystemCredentialsProvider.getInstance().save();

        JenkinsRule.WebClient web = j.createWebClient();
        HtmlForm form = web.goTo("configure").getFormByName("config");
        form.getSelectByName("_.credentialsId").setSelectedIndex(1);

        j.submit(form);

        String id = Util.getDigestOf("testFingerprintOperationsWithAuthentication");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        assertThat(Fingerprint.load(id), is(not(nullValue())));
        Fingerprint.delete(id);
        assertThat(Fingerprint.load(id), is(nullValue()));
    }

}
