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

import eu.rekawek.toxiproxy.model.ToxicDirection;
import hudson.Util;
import hudson.model.Fingerprint;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.Network;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class RedisConnectionTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule
    public Network network = Network.newNetwork();

    @Rule
    public GenericContainer redis = new GenericContainer("redis:6.0.4-alpine")
            .withExposedPorts(6379)
            .withNetwork(network);

    @Rule
    public ToxiproxyContainer toxiproxy = new ToxiproxyContainer()
            .withNetwork(network)
            .withNetworkAliases("toxiproxy");

    private void setRedisConfigurationViaProxy(ToxiproxyContainer.ContainerProxy proxy) {
        final String host = proxy.getContainerIpAddress();
        final int port = proxy.getProxyPort();
        Utils.setRedisConfiguration(host, port);
    }

    @Test
    public void testRedisConnectionFailureForSave() throws IOException {
        String id = Util.getDigestOf("testRedisConnectionFailureForSave");

        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(redis, 6379);
        setRedisConfigurationViaProxy(proxy);
        proxy.setConnectionCut(true);

        try {
            new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        } catch (JedisException e) {
            proxy.setConnectionCut(false);
            Fingerprint fingerprintLoaded = Fingerprint.load(id);
            assertThat(fingerprintLoaded, is(nullValue()));
            return;
        }
        fail("Expected JedisException");
    }

    @Test(expected=JedisException.class)
    public void testRedisConnectionFailureForLoad() throws IOException {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(redis, 6379);
        setRedisConfigurationViaProxy(proxy);

        String id = Util.getDigestOf("testRedisConnectionFailureForLoad");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));

        proxy.setConnectionCut(true);
        Fingerprint.load(id);
    }

    @Test
    public void testRedisConnectionFailureForDelete() throws IOException {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(redis, 6379);
        setRedisConfigurationViaProxy(proxy);

        String id = Util.getDigestOf("testRedisConnectionFailureForDelete");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));

        proxy.setConnectionCut(true);

        try {
            Fingerprint.delete(id);
        } catch (JedisException e) {
            proxy.setConnectionCut(false);
            Fingerprint fingerprintLoaded = Fingerprint.load(id);
            assertThat(fingerprintLoaded, is(not(nullValue())));
            return;
        }
        fail("Expected JedisException");
    }

    @Test
    public void testRedisConnectionFailureForIsReady() {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(redis, 6379);
        setRedisConfigurationViaProxy(proxy);

        proxy.setConnectionCut(true);

        try {
            RedisFingerprintStorage.get().isReady();
        } catch (JedisException e) {
            proxy.setConnectionCut(false);
            assertThat(RedisFingerprintStorage.get().isReady(), is(false));
            return;
        }
        fail("Expected JedisException");
    }

    @Test(expected=JedisException.class)
    public void testSlowRedisConnectionForSave() throws IOException {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(redis, 6379);
        proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 2010);
        setRedisConfigurationViaProxy(proxy);

        String id = Util.getDigestOf("testSlowRedisConnectionForSave");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));
    }

    @Test(expected=JedisException.class)
    public void testSlowRedisConnectionForLoad() throws IOException {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(redis, 6379);
        proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 2010);
        setRedisConfigurationViaProxy(proxy);

        String id = Util.getDigestOf("testSlowRedisConnectionForLoad");
        Fingerprint.load(id);
    }

    @Test(expected=JedisException.class)
    public void testSlowRedisConnectionForDelete() throws IOException {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(redis, 6379);
        proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 2010);
        setRedisConfigurationViaProxy(proxy);

        String id = Util.getDigestOf("testSlowRedisConnectionForDelete");
        Fingerprint.delete(id);
    }

    @Test(expected=JedisException.class)
    public void testSlowRedisConnectionForIsReady() throws IOException {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(redis, 6379);
        proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 2010);
        setRedisConfigurationViaProxy(proxy);

        RedisFingerprintStorage.get().isReady();
    }

}
