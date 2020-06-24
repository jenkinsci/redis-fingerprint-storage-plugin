package io.jenkins.plugins.redis;

import hudson.Extension;
import hudson.Functions;
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
