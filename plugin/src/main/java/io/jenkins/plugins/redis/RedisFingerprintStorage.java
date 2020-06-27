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
import hudson.ExtensionList;
import hudson.model.TaskListener;
import jenkins.fingerprints.FingerprintStorage;
import hudson.model.Fingerprint;
import hudson.Util;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Pluggable external fingerprint storage for fingerprints in Redis.
 *
 * @author Sumit Sarin
 */

@Extension
public class RedisFingerprintStorage extends FingerprintStorage {

    private final String instanceId;
    private static final Logger LOGGER = Logger.getLogger(Fingerprint.class.getName());
    private volatile JedisPool jedisPool;
    private static final int MAX_FINGERPRINT_DELETES = 100;

    public static RedisFingerprintStorage get() {
        return ExtensionList.lookup(RedisFingerprintStorage.class).get(0);
    }

    public RedisFingerprintStorage() throws IOException {
        instanceId = Util.getDigestOf(new ByteArrayInputStream(InstanceIdentity.get().getPublic().getEncoded()));
        createJedisPoolFromConfig();
    }

    void createJedisPoolFromConfig() {
        GlobalRedisConfiguration config = GlobalRedisConfiguration.get();
        createJedisPool(config.getHost(), config.getPort(), config.getConnectionTimeout(), config.getSocketTimeout(),
                config.getUsername(), config.getPassword(), config.getDatabase(), config.getSsl());
    }

    void createJedisPool(
            String host, int port, int connectionTimeout, int socketTimeout, String username, String password,
            int database, boolean ssl) {
        jedisPool = new JedisPool(new JedisPoolConfig(), host, port, connectionTimeout, socketTimeout, username,
                password, database, "Jenkins", ssl);
    }

    private @NonNull Jedis getJedis() throws JedisException{
        return jedisPool.getResource();
    }

    /**
     * Saves the given fingerprint.
     */
    public synchronized void save(Fingerprint fp) throws JedisException {
        StringWriter writer = new StringWriter();
        Fingerprint.getXStream().toXML(fp, writer);
        try (Jedis jedis = getJedis()) {
            Transaction transaction = jedis.multi();
            transaction.set(instanceId + fp.getHashString(), writer.toString());
            transaction.sadd(instanceId, fp.getHashString());
            transaction.exec();
        } catch (JedisException e) {
            LOGGER.log(Level.WARNING, "Jedis failed in saving fingerprint: " + fp.toString(), e);
            throw e;
        }
    }

    /**
     * Returns the fingerprint associated with the given unique id and the Jenkins instance ID, from the storage.
     */
    public @CheckForNull Fingerprint load(@NonNull String id) throws IOException, JedisException {
        String loadedData;

        try (Jedis jedis = getJedis()) {
            loadedData = jedis.get(instanceId + id);
        } catch (JedisException e) {
            LOGGER.log(Level.WARNING, "Jedis failed in loading fingerprint: " + id, e);
            throw e;
        }

        if (loadedData == null) return null;

        return blobToFingerprint(loadedData);
    }

    private Fingerprint blobToFingerprint(String blob) throws IOException {
        Object loadedObject = null;
        Fingerprint loadedFingerprint;

        try (InputStream in = new ByteArrayInputStream(blob.getBytes(StandardCharsets.UTF_8))) {
            loadedObject = Fingerprint.getXStream().fromXML(in);
            loadedFingerprint = (Fingerprint) loadedObject;
        } catch (RuntimeException e) {
            throw new IOException("Unexpected Fingerprint type. Expected " + Fingerprint.class + " or subclass but got "
                    + (loadedObject != null ? loadedObject.getClass() : "null"));
        }

        return loadedFingerprint;

    }

    /**
     * Deletes the fingerprint with the given id.
     */
    public void delete(@NonNull String id) throws JedisException {
        try (Jedis jedis = getJedis()) {
            Transaction transaction = jedis.multi();
            transaction.del(instanceId + id);
            transaction.srem(instanceId, id);
            transaction.exec();
        } catch (JedisException e) {
            LOGGER.log(Level.WARNING, "Jedis failed in deleting fingerprint: " + id, e);
            throw e;
        }
    }

    /**
     * Returns true if there's some data in the fingerprint database.
     */
    public boolean isReady() {
        try (Jedis jedis = getJedis()) {
            return jedis.smembers(instanceId).size() != 0;
        } catch (JedisException e) {
            LOGGER.log(Level.WARNING, "Failed to connect to Jedis", e);
            throw e;
        }
    }

    public void iterateAndCleanupFingerprints(TaskListener listener) {
        String currentPointer = ScanParams.SCAN_POINTER_START;

        try {
            do {
                ScanResult<String> scanResult = RedisFingerprintStorage.get().getFingerprintIdsForCleanup(currentPointer);
                List<String> fingerprintIds = scanResult.getResult();

                try {
                    List<Fingerprint> fingerprints = bulkLoad(fingerprintIds);
                    for (Fingerprint fingerprint : fingerprints) {
                        if (fingerprint != null) {
                            cleanFingerprint(fingerprint, listener);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Fingerprints found were malformed.", e);
                }

                currentPointer = scanResult.getCursor();
            } while (!currentPointer.equals(ScanParams.SCAN_POINTER_START));
        } catch (JedisException e) {
            LOGGER.log(Level.WARNING, "Jedis failed to clean fingerprints. ", e);
        }

    }

    ScanResult<String> getFingerprintIdsForCleanup(String cur) throws JedisException {
        ScanParams scanParams = new ScanParams().count(MAX_FINGERPRINT_DELETES);
        try (Jedis jedis = getJedis()) {
            return jedis.sscan(instanceId, cur, scanParams);
        } catch (JedisException e) {
            LOGGER.log(Level.WARNING, "Failed to connect to Jedis", e);
            throw e;
        }
    }

    List<Fingerprint> bulkLoad(List<String> ids) throws IOException {
        List<String> instanceConcatenatedIds = new ArrayList<>();

        for (String id : ids) {
            instanceConcatenatedIds.add(instanceId + id);
        }

        String[] fingerprintIds = instanceConcatenatedIds.toArray(new String[instanceConcatenatedIds.size()]);
        try (Jedis jedis = getJedis()) {
            List<String> fingerprintBlobs = jedis.mget(fingerprintIds);
            List<Fingerprint> fingerprints = new ArrayList<>();
            for (String fingerprintBlob : fingerprintBlobs) {
                fingerprints.add(blobToFingerprint(fingerprintBlob));
            }
            return Collections.unmodifiableList(fingerprints);
        } catch (JedisException e) {
            LOGGER.log(Level.WARNING, "Failed to connect to Jedis", e);
            throw e;
        }
    }

}
