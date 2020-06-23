package io.jenkins.plugins.redis;

import hudson.Extension;
import hudson.model.Fingerprint;
import hudson.model.TaskListener;
import jenkins.fingerprints.FingerprintCleanupThread;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.util.List;

@Extension
public class RedisFingerprintCleanup extends FingerprintCleanupThread {

    public void execute(TaskListener listener) {
        String currentPointer = redis.clients.jedis.ScanParams.SCAN_POINTER_START;
        do {
            ScanResult<String> scanResult = RedisFingerprintStorage.get().getFingerprintIdsForCleanup(currentPointer);
            List<String> fingerprintIds = scanResult.getResult();

            for (String fingerprintId : fingerprintIds) {
                Fingerprint fingerprint = Fingerprint.load(fingerprintId);
                if (fingerprint != null) {
                    cleanFingerprint(fingerprint, listener);
                }
            }
            currentPointer = scanResult.getCursor();
        } while (currentPointer != redis.clients.jedis.ScanParams.SCAN_POINTER_START);

    }

}
