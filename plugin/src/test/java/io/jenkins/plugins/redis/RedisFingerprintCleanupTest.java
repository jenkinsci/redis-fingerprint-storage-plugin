package io.jenkins.plugins.redis;

import hudson.Util;
import hudson.model.Fingerprint;
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
import java.io.IOException;

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
        setConfiguration();
        String instanceId = Util.getDigestOf(new ByteArrayInputStream(InstanceIdentity.get().getPublic().getEncoded()));
        String id = Util.getDigestOf("shouldDeleteFingerprintAfterCleanup");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));

        // Check if fingerprint has been stored
        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(not(nullValue())));
        assertThat(jedis.smembers(instanceId), hasItem(id));

        // Check if fingerprint gets deleted after cleanup
        RedisFingerprintCleanup redisFingerprintCleanup = new RedisFingerprintCleanup();
        redisFingerprintCleanup.execute(null);
        fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(nullValue()));
        assertThat(jedis.smembers(instanceId), not(hasItem(id)));
    }

}
