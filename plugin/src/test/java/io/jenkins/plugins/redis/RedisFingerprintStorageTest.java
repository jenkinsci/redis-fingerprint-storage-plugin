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

import hudson.Util;
import hudson.model.Fingerprint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import jenkins.fingerprints.FingerprintStorage;
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
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;

public class RedisFingerprintStorageTest {

    private Jedis jedis;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public GenericContainer redis = new GenericContainer<>("redis:6.0.4-alpine").withExposedPorts(6379);

    @Before
    public void setup() {
        String host = redis.getHost();
        Integer port = redis.getFirstMappedPort();
        jedis = new Jedis(host, port);
    }

    private void setConfiguration() {
        GlobalRedisConfiguration redisConfiguration = GlobalRedisConfiguration.get();
        String host = redis.getHost();
        Integer port = redis.getFirstMappedPort();
        redisConfiguration.setHost(host);
        redisConfiguration.setPort(port);
        redisConfiguration.setEnabled(true);
    }

    @After
    public void teardown() {
        if (jedis!=null) jedis.close();
    }

    @Test
    public void checkFingerprintStorageIsRedis() throws IOException {
        setConfiguration();
        Object fingerprintStorage = FingerprintStorage.get();
        assertThat(fingerprintStorage, instanceOf(RedisFingerprintStorage.class));
    }

    @Test
    public void roundTrip() throws IOException {
        setConfiguration();
        String id = Util.getDigestOf("roundTrip");
        Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(not(nullValue())));
        assertThat(fingerprintSaved.toString(), is(equalTo(fingerprintLoaded.toString())));
    }

    @Test
    public void loadingNonExistentFingerprintShouldReturnNull() throws IOException{
        setConfiguration();
        String id = Util.getDigestOf("loadingNonExistentFingerprintShouldReturnNull");
        Fingerprint fingerprint = Fingerprint.load(id);
        assertThat(fingerprint, is(nullValue()));
    }

    @Test(expected=IOException.class)
    public void shouldFailWhenStoredObjectIsInvalidFingerprint() throws IOException {
        setConfiguration();
        String id = Util.getDigestOf("shouldFailWhenStoredObjectIsInvalidFingerprint");
        String instanceId = Util.getDigestOf(new ByteArrayInputStream(InstanceIdentity.get().getPublic().getEncoded()));
        jedis.set(instanceId+id, "Invalid Data");
        Fingerprint.load(id);
    }

    @Test
    public void shouldDeleteFingerprint() throws IOException {
        setConfiguration();
        String id = Util.getDigestOf("shouldDeleteFingerprint");
        Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        Fingerprint.delete(id);
        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(nullValue()));
        Fingerprint.delete(id);
        fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(nullValue()));
    }

}
