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

import hudson.EnvVars;
import hudson.Util;
import hudson.model.Fingerprint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import hudson.slaves.EnvironmentVariablesNodeProperty;
import jenkins.util.SystemProperties;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.Jedis;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class RedisFingerprintStorageTest {

    private Jedis jedis;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public GenericContainer redis = new GenericContainer<>("redis:6.0.4-alpine").withExposedPorts(6379);

    @Before
    public void setUp() throws IOException {
        setEnvironmentVariables();

        String address = redis.getHost();
        Integer port = redis.getFirstMappedPort();
        jedis = new Jedis(address, port);
    }

    public void setEnvironmentVariables() throws IOException {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars env = prop.getEnvVars();
        env.put("redis.host", redis.getHost());
        env.put("redis.port", String.valueOf(redis.getFirstMappedPort()));
        env.put("FingerprintStorageEngine", "io.jenkins.plugins.redis.RedisFingerprintStorage");
        j.jenkins.getGlobalNodeProperties().add(prop);
    }

    @After
    public void restoreSystemProperties() {
        jedis.close();
    }

    @Test
    public void roundTrip() throws IOException {
        byte[] md5 = Fingerprint.toByteArray(Util.getDigestOf("roundTrip"));
        Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", md5);
        Fingerprint fingerprintLoaded = Fingerprint.load(md5);
        assertThat(fingerprintLoaded, is(not(nullValue())));
        assertThat(fingerprintSaved.toString(), is(equalTo(fingerprintLoaded.toString())));
    }

    @Test
    public void loadingNonExistentFingerprintShouldReturnNull() throws IOException{
        byte[] md5 = Fingerprint.toByteArray(Util.getDigestOf("loadingNonExistentFingerprintShouldReturnNull"));
        Fingerprint fingerprint = Fingerprint.load(md5);
        assertThat(fingerprint, is(nullValue()));
    }

    @Test
    public void shouldThrowIOExceptionWhenFingerprintIsInvalid() throws IOException {
        byte[] md5 = Fingerprint.toByteArray(Util.getDigestOf("shouldThrowIOExceptionWhenFingerprintIsInvalid"));
        String instanceId = Util.getDigestOf(new ByteArrayInputStream(InstanceIdentity.get().getPublic().getEncoded()));
        jedis.set(instanceId+Util.toHexString(md5), "garbageData");
        try {
            Fingerprint.load(md5);
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("Unexpected Fingerprint type"));
            return;
        }
        fail("Expected IOException");
    }

}
