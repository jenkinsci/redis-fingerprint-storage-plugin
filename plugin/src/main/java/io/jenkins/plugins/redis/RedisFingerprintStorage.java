package io.jenkins.plugins.redis;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.BulkChange;
import hudson.Extension;
import hudson.Util;
import hudson.model.Job;
import hudson.model.Fingerprint;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Logger;
import java.util.logging.Level;

import hudson.util.PersistedList;
import jenkins.model.FingerprintFacet;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import redis.clients.jedis.Jedis;

/**
 * Pluggable external fingerprint storage for fingerprints in Redis.
 */

@Extension
public class RedisFingerprintStorage{

    private String instanceId;
    private static final Logger logger = Logger.getLogger(Fingerprint.class.getName());

    RedisFingerprintStorage() throws IOException{
        setInstanceId();
    }

    private void setInstanceId() throws IOException{
        try {
            instanceId = Util.getDigestOf(new ByteArrayInputStream(InstanceIdentity.get().getPublic().getEncoded()));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to obtain Instance ID. "+e);
            throw e;
        }
    }

    /**
     * Saves the given fingerprint.
     */
    public synchronized void save(Fingerprint fp) throws IOException {
        if(BulkChange.contains(fp))
            return;
        Jedis jedis = new Jedis("localhost");
        System.out.println("Connection to server successfully");
        StringWriter writer = new StringWriter();
        fp.getXStream().toXML(fp, writer);
        jedis.set(instanceId+fp.getHashString(), writer.toString());
    }

    /**
     * Returns the fingerprint associated with the given md5sum and the Jenkins instance ID, from the storage.
     */
    public @CheckForNull Fingerprint load(@NonNull String md5sum) throws IOException {
        Jedis jedis = new Jedis("localhost");
        Object loaded = jedis.get(instanceId+md5sum);
        if (!(loaded instanceof Fingerprint)) {
            throw new IOException("Unexpected Fingerprint type. Expected " + Fingerprint.class + " or subclass but got "
                    + (loaded != null ? loaded.getClass() : "null"));
        }
        Fingerprint f = (Fingerprint) loaded;
//        if (f.facets==null)
//            f.facets = new PersistedList<>(f);
//        for (FingerprintFacet facet : f.facets)
//            facet._setOwner(f);
        return f;
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
