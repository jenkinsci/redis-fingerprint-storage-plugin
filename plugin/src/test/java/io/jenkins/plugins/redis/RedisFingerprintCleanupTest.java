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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Util;
import hudson.model.Fingerprint;
import hudson.model.TaskListener;
import jenkins.fingerprints.FingerprintCleanupThread;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.Jedis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class RedisFingerprintCleanupTest {

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

    @After
    public void teardown() {
        if (jedis!=null) jedis.close();
    }

    private void setConfiguration() {
        GlobalRedisConfiguration redisConfiguration = GlobalRedisConfiguration.get();
        String host = redis.getHost();
        Integer port = redis.getFirstMappedPort();
        RedisFingerprintStorage redisFingerprintStorage = RedisFingerprintStorage.get();
        redisFingerprintStorage.createJedisPool(host, port, 2000, 2000,
                "default", "", 0, false);
    }

    @Test
    public void checkRedisFingerprintCleanupIsDefaultCleanup() throws IOException {
        Object fingerprintCleanupThread = FingerprintCleanupThread.get();
        assertThat(fingerprintCleanupThread, instanceOf(RedisFingerprintCleanup.class));
    }

    @Test
    public void shouldDeleteFingerprintAfterCleanup() throws IOException {
        TestTaskListener testTaskListener = new TestTaskListener();
        setConfiguration();
        String instanceId = Util.getDigestOf(new ByteArrayInputStream(InstanceIdentity.get().getPublic().getEncoded()));
        String id = Util.getDigestOf("shouldDeleteFingerprintAfterCleanup");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));

        FingerprintCleanupThread.get().execute(testTaskListener);

        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(nullValue()));
        assertThat(jedis.smembers(instanceId), not(hasItem(id)));
    }

    private static class TestTaskListener implements TaskListener {

        private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private PrintStream logStream = new PrintStream(outputStream);

        @NonNull
        @Override
        public PrintStream getLogger() {
            return logStream;
        }

    }

}
