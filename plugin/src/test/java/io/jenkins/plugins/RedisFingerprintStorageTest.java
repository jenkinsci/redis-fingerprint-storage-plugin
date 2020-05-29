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
package io.jenkins.plugins;

import hudson.Util;
import hudson.model.Fingerprint;
import io.jenkins.plugins.redis.RedisFingerprintStorage;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class RedisFingerprintStorageTest {

    private RedisFingerprintStorage redisFingerprintStorage;
    private Map<String, String> savedSystemProperties = new HashMap<>();
    private static final byte[] SOME_MD5 = Fingerprint.toByteArray(Util.getDigestOf("whatever"));

    @Rule
    public GenericContainer redis = new GenericContainer<>("redis:5.0.3-alpine").withExposedPorts(6379);

    @Before
    public void setUp() throws IOException {
        preserveSystemProperty("redis.host");
        preserveSystemProperty("redis.port");
        preserveSystemProperty("FingerprintStorageEngine");

        String address = redis.getHost();
        Integer port = redis.getFirstMappedPort();

        System.setProperty("redis.host", address);
        System.setProperty("redis.port", String.valueOf(port));
        System.setProperty("FingerprintStorageEngine", "io.jenkins.plugins.redis.RedisFingerprintStorage");

        redisFingerprintStorage = new RedisFingerprintStorage();
    }

    @After
    public void restoreSystemProperties() {
        for (Map.Entry<String, String> entry : savedSystemProperties.entrySet()) {
            if (entry.getValue() != null) {
                System.setProperty(entry.getKey(), entry.getValue());
            } else {
                System.clearProperty(entry.getKey());
            }
        }
    }

    private void preserveSystemProperty(String propertyName) {
        savedSystemProperties.put(propertyName, System.getProperty(propertyName));
        System.clearProperty(propertyName);
    }

    @Test
    public void roundTrip() throws IOException {
        Fingerprint fingerprintSaved = new Fingerprint(null, "stuff&more.jar", SOME_MD5);
        Fingerprint fingerprintLoaded = Fingerprint.load(fingerprintSaved.getHashString());

        assertThat(fingerprintLoaded, is(not(nullValue())));
        assertThat(fingerprintSaved.toString(), is(equalTo(fingerprintLoaded.toString())));
    }

}
