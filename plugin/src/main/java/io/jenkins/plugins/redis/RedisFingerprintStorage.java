package io.jenkins.plugins.redis;

import hudson.model.Fingerprint;
import hudson.model.Job;

/**
 * Pluggable external fingerprint storage for fingerprints in Redis.
 */

public class RedisFingerprintStorage {

    /**
     * Saves the given fingerprint.
     */
    public boolean save(Fingerprint fp){
        return true;
    }

    /**
     * Returns the fingerprint associated with the given md5sum and the Jenkins instance ID, from the storage.
     */
    public Fingerprint load(byte[] md5sum){
        return null;
    }

    /**
     * Deletes the fingerprint fp with the associated md5 of the given fingerprint and jenkins instance ID.
     */
    public boolean delete(Fingerprint fp){
        return true;
    }

    /**
     * Deletes the fingerprint fp with the associated md5 of the given fingerprint and jenkins instance ID.
     */
    public boolean delete(byte[] md5sum){
        return true;
    }

    /**
     * Returns all the fingerprints associated with the given md5sum, across all Jenkins instances connected to the
     * external storage.
     */
    public Fingerprint[] trace(byte[] md5sum){
        return null;
    }

    /**
     * Returns all the fingerprints associated with the system’s Jenkins instance ID in the storage.
     */
    public Fingerprint[] getAllLocalFingerprints() {return null;}

    /**
     * Returns all the fingerprints stored in the storage.
     */
    public Fingerprint[] getAllGlobalFingerprints() {return null;}

    /**
     * Returns all the fingerprints associated with the Job j across all Jenkins instances.
     */
    public Fingerprint[] trace(Job j){
        return null;
    }

    /**
     * Returns all the fingerprints associated with given Job and build across all Jenkins instances.
     */
    public Fingerprint[] trace(Job j, int build){
        return null;
    }

}
