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
import jenkins.fingerprints.GlobalFingerprintConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;

import static org.hamcrest.Matchers.isA;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

public class RedisAuthenticationTest {

    private static final String NO_RESOURCE_FROM_POOL = "Could not get a resource from the pool";

    private static final StandardUsernamePasswordCredentials INCORRECT_CREDENTIAL = new UsernamePasswordCredentialsImpl(
            CredentialsScope.SYSTEM,
            "credentialId",
            null,
            "default",
            "redis_incorrect_password"
    );

    @Rule
    public GenericContainer redis = new GenericContainer<>("redis:6.0.4-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server --requirepass redis_password");

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testSaveWithoutPasswordWhenPasswordIsConfigured() throws Exception {
        exceptionRule.expect(JedisConnectionException.class);
        exceptionRule.expectMessage(NO_RESOURCE_FROM_POOL);
        exceptionRule.expectCause(isA(JedisAccessControlException.class));

        RedisConfiguration.setConfiguration(redis.getHost(), redis.getFirstMappedPort());
        String id = Util.getDigestOf("testSaveWithoutPasswordWhenPasswordIsConfigured");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));
    }

    @Test
    public void testLoadWithoutPasswordWhenPasswordIsConfigured() throws Exception {
        exceptionRule.expect(JedisConnectionException.class);
        exceptionRule.expectMessage(NO_RESOURCE_FROM_POOL);
        exceptionRule.expectCause(isA(JedisAccessControlException.class));

        RedisConfiguration.setConfiguration(redis.getHost(), redis.getFirstMappedPort());
        String id = Util.getDigestOf("testLoadWithoutPasswordWhenPasswordIsConfigured");
        Fingerprint.load(id);
    }

    @Test
    public void testDeleteWithoutPasswordWhenPasswordIsConfigured() throws Exception {
        exceptionRule.expect(JedisConnectionException.class);
        exceptionRule.expectMessage(NO_RESOURCE_FROM_POOL);
        exceptionRule.expectCause(isA(JedisAccessControlException.class));

        RedisConfiguration.setConfiguration(redis.getHost(), redis.getFirstMappedPort());
        String id = Util.getDigestOf("testDeleteWithoutPasswordWhenPasswordIsConfigured");
        Fingerprint.delete(id);
    }

    @Test
    public void testIsReadyWithoutPasswordWhenPasswordIsConfigured() throws IOException {
        exceptionRule.expect(JedisConnectionException.class);
        exceptionRule.expectMessage(NO_RESOURCE_FROM_POOL);
        exceptionRule.expectCause(isA(JedisAccessControlException.class));

        RedisConfiguration.setConfiguration(redis.getHost(), redis.getFirstMappedPort());
        GlobalFingerprintConfiguration.get().getStorage().isReady();
    }

    @Test
    public void testSaveWithIncorrectPassword() throws Exception {
        exceptionRule.expect(JedisConnectionException.class);
        exceptionRule.expectMessage(NO_RESOURCE_FROM_POOL);
        exceptionRule.expectCause(isA(JedisAccessControlException.class));

        SystemCredentialsProvider.getInstance().getCredentials().add(INCORRECT_CREDENTIAL);
        SystemCredentialsProvider.getInstance().save();

        RedisConfiguration.setConfiguration(redis.getHost(), redis.getFirstMappedPort(), INCORRECT_CREDENTIAL.getId());
        String id = Util.getDigestOf("testSaveWithIncorrectPassword");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));
    }

    @Test
    public void testLoadWithIncorrectPassword() throws Exception {
        exceptionRule.expect(JedisConnectionException.class);
        exceptionRule.expectMessage(NO_RESOURCE_FROM_POOL);
        exceptionRule.expectCause(isA(JedisAccessControlException.class));

        SystemCredentialsProvider.getInstance().getCredentials().add(INCORRECT_CREDENTIAL);
        SystemCredentialsProvider.getInstance().save();

        RedisConfiguration.setConfiguration(redis.getHost(), redis.getFirstMappedPort(), INCORRECT_CREDENTIAL.getId());
        String id = Util.getDigestOf("testLoadWithIncorrectPassword");
        Fingerprint.load(id);
    }

    @Test
    public void testDeleteWithIncorrectPassword() throws Exception {
        exceptionRule.expect(JedisConnectionException.class);
        exceptionRule.expectMessage(NO_RESOURCE_FROM_POOL);
        exceptionRule.expectCause(isA(JedisAccessControlException.class));

        SystemCredentialsProvider.getInstance().getCredentials().add(INCORRECT_CREDENTIAL);
        SystemCredentialsProvider.getInstance().save();

        RedisConfiguration.setConfiguration(redis.getHost(), redis.getFirstMappedPort(), INCORRECT_CREDENTIAL.getId());
        String id = Util.getDigestOf("testDeleteWithIncorrectPassword");
        Fingerprint.delete(id);
    }

    @Test
    public void testIsReadyWithIncorrectPassword() throws Exception {
        exceptionRule.expect(JedisConnectionException.class);
        exceptionRule.expectMessage(NO_RESOURCE_FROM_POOL);
        exceptionRule.expectCause(isA(JedisAccessControlException.class));

        SystemCredentialsProvider.getInstance().getCredentials().add(INCORRECT_CREDENTIAL);
        SystemCredentialsProvider.getInstance().save();

        RedisConfiguration.setConfiguration(redis.getHost(), redis.getFirstMappedPort(), INCORRECT_CREDENTIAL.getId());
        GlobalFingerprintConfiguration.get().getStorage().isReady();
    }

    @Test
    public void testFingerprintOperationsWithDefaultUsernameAndCorrectPassword() throws Exception {
        StandardUsernamePasswordCredentials credential = new UsernamePasswordCredentialsImpl(
                CredentialsScope.SYSTEM,
                "credentialId",
                null,
                "default",
                "redis_password"
        );

        SystemCredentialsProvider.getInstance().getCredentials().add(credential);
        SystemCredentialsProvider.getInstance().save();

        RedisConfiguration.setConfiguration(redis.getHost(), redis.getFirstMappedPort(), credential.getId());

        String id = Util.getDigestOf("testFingerprintOperationsWithDefaultUsernameAndCorrectPassword");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        assertThat(Fingerprint.load(id), is(not(nullValue())));
        assertThat(GlobalFingerprintConfiguration.get().getStorage().isReady(), is(true));
        Fingerprint.delete(id);
        assertThat(Fingerprint.load(id), is(nullValue()));
        assertThat(GlobalFingerprintConfiguration.get().getStorage().isReady(), is(false));
    }

    @Test
    public void testAuthenticationViaWebUI() throws Exception {
        RedisConfiguration.setConfiguration(redis.getHost(), redis.getFirstMappedPort());

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

        String id = Util.getDigestOf("testAuthenticationViaWebUI");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        assertThat(Fingerprint.load(id), is(not(nullValue())));
        assertThat(GlobalFingerprintConfiguration.get().getStorage().isReady(), is(true));
        Fingerprint.delete(id);
        assertThat(Fingerprint.load(id), is(nullValue()));
        assertThat(GlobalFingerprintConfiguration.get().getStorage().isReady(), is(false));
    }

}
