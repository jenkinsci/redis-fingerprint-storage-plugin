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

import hudson.Extension;
import hudson.model.Fingerprint;
import hudson.model.TaskListener;
import jenkins.fingerprints.FingerprintCleanupThread;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class RedisFingerprintCleanup extends FingerprintCleanupThread {

    private static final Logger LOGGER = Logger.getLogger(RedisFingerprintCleanup.class.getName());

    public void execute(TaskListener listener) {
        String currentPointer = redis.clients.jedis.ScanParams.SCAN_POINTER_START;

        try {
            do {
                ScanResult<String> scanResult = RedisFingerprintStorage.get().getFingerprintIdsForCleanup(currentPointer);
                List<String> fingerprintIds = scanResult.getResult();

                for (String fingerprintId : fingerprintIds) {
                    try {
                        Fingerprint fingerprint = Fingerprint.load(fingerprintId);
                        if (fingerprint != null) {
                            cleanFingerprint(fingerprint, listener);
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Fingerprints with id " + fingerprintId + "is malformed.", e);
                    }
                }
                currentPointer = scanResult.getCursor();
            } while (!currentPointer.equals(redis.clients.jedis.ScanParams.SCAN_POINTER_START));
        } catch (JedisException e) {
            LOGGER.log(Level.WARNING, "Jedis failed to clean fingerprints. ", e);
        }

    }

}
