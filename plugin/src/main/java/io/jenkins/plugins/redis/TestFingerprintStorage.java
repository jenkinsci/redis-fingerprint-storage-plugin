package io.jenkins.plugins.redis;

import hudson.Extension;
import hudson.model.Fingerprint;
import jenkins.fingerprints.FingerprintStorage;
import jenkins.fingerprints.FingerprintStorageDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.HashMap;
import java.util.Map;

@Extension
public class TestFingerprintStorage extends FingerprintStorage {

    private String host = DescriptorImpl.defaultHost;;

    public String getHost() {
        return host;
    }

    @DataBoundSetter
    public void setHost(String host) {
        this.host = host;
    }

    @DataBoundConstructor
    public TestFingerprintStorage() {

    }

    Map<String, Fingerprint> storage = new HashMap<>();

    @Override
    public void save(Fingerprint fp) {
        storage.put(fp.getHashString(), fp);
    }


    @Override
    public Fingerprint load(String id) {
        return storage.get(id);
    }

    @Override
    public void delete(String id) {
        storage.remove(id);
    }

    @Override
    public boolean isReady() {
        return storage.size() != 0;
    }

    @Extension
    public static class DescriptorImpl extends FingerprintStorageDescriptor {

        public final static String defaultHost = "localhost";

        @Override
        public String getDisplayName() {
            return "Test Fingerprint Storage";
        }

    }

}
