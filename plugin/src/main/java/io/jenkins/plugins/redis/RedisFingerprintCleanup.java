package io.jenkins.plugins.redis;

import hudson.Extension;
import hudson.model.TaskListener;
import jenkins.fingerprints.FingerprintCleanupThread;

@Extension
public class RedisFingerprintCleanup extends FingerprintCleanupThread {

    @Override
    public void execute(TaskListener listener) {

    }

}
