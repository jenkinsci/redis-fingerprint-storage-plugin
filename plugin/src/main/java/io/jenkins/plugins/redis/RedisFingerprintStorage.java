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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.Extension;
import hudson.model.FingerprintStorage;
import hudson.model.Fingerprint;
import hudson.Util;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.logging.Level;

import jenkins.util.SystemProperties;

import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Pluggable external fingerprint storage for fingerprints in Redis.
 */

@Extension
public class RedisFingerprintStorage extends FingerprintStorage {

    private static String host;
    private static int port;
    private final String instanceId;
    private static volatile JedisPool jedisPool;
    private static final Logger logger = Logger.getLogger(Fingerprint.class.getName());

    /**
     * Saves the given fingerprint.
     */
    public RedisFingerprintStorage() throws IOException{
        try {
            instanceId = Util.getDigestOf(new ByteArrayInputStream(InstanceIdentity.get().getPublic().getEncoded()));
            initConnection();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to obtain Instance ID. "+e);
            throw e;
        }
    }

    /**
     * Initializes connection params and JedisPool.
     * It initializes a JedisPool only when either it is being created for the first time or connection parameters
     * are being changed.
     */
    private synchronized void initConnection(){
        String newHost = SystemProperties.getString("redis.host", "localhost");
        int newPort = SystemProperties.getInteger("redis.port", 6379);
        if ((!newHost.equals(host)) || (newPort != port)) {
            host = newHost;
            port = newPort;
            if (jedisPool != null) jedisPool.close();
            jedisPool = new JedisPool(host, port);
        }
    }

    /**
     * Saves the given fingerprint.
     */
    public synchronized void save(Fingerprint fp) throws JedisException {
        Jedis jedis = null;
        StringWriter writer = new StringWriter();
        Fingerprint.getXStream().toXML(fp, writer);
        try {
            jedis = jedisPool.getResource();
            jedis.set(instanceId + fp.getHashString(), writer.toString());
        } catch (JedisException e) {
            logger.log(Level.WARNING, "Jedis failed in saving fingerprint: "+fp.toString()+e);
            throw e;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * Returns the fingerprint associated with the given md5sum and the Jenkins instance ID, from the storage.
     */
    public @CheckForNull Fingerprint load(@NonNull byte[] md5sum) throws IOException {
        return load(Util.toHexString(md5sum));
    }

    /**
     * Returns the fingerprint associated with the given md5sum and the Jenkins instance ID, from the storage.
     */
    public @CheckForNull Fingerprint load(@NonNull String md5sum) throws IOException, JedisException{
        String loadedData;
        Jedis jedis = null;

        try {
            jedis = jedisPool.getResource();
            loadedData = jedis.get(instanceId + md5sum);
        } catch (JedisException e) {
            logger.log(Level.WARNING, "Jedis failed in loading fingerprint: "+md5sum+e);
            throw e;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        if (loadedData == null) return null;

        Object loadedObject = null;
        Fingerprint loadedFingerprint;

        try (InputStream in = new ByteArrayInputStream(loadedData.getBytes(StandardCharsets.UTF_8))) {
            loadedObject = Fingerprint.getXStream().fromXML(in);
            loadedFingerprint = (Fingerprint) loadedObject;
        } catch (RuntimeException | Error e) {
            throw new IOException("Unexpected Fingerprint type. Expected " + Fingerprint.class + " or subclass but got "
                    + (loadedObject != null ? loadedObject.getClass() : "null"));
        }

        return loadedFingerprint;

    }

}
