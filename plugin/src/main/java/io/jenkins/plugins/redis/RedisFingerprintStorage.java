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

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.fingerprints.FingerprintStorage;
import hudson.model.Fingerprint;
import hudson.Util;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import jenkins.fingerprints.FingerprintStorageDescriptor;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

import javax.servlet.ServletException;

/**
 * Pluggable external fingerprint storage for fingerprints in Redis.
 *
 * @author Sumit Sarin
 */

@Extension
public class RedisFingerprintStorage extends FingerprintStorage {

    private final String instanceId;
    private static final Logger LOGGER = Logger.getLogger(Fingerprint.class.getName());

    public static RedisFingerprintStorage get() {
        return ExtensionList.lookup(RedisFingerprintStorage.class).get(0);
    }

    @DataBoundConstructor
    public RedisFingerprintStorage() throws IOException {
        instanceId = Util.getDigestOf(new ByteArrayInputStream(InstanceIdentity.get().getPublic().getEncoded()));
        JedisPoolManager.createJedisPoolFromConfig();
    }

    /**
     * Saves the given fingerprint.
     */
    public synchronized void save(Fingerprint fp) throws JedisException {
        StringWriter writer = new StringWriter();
        Fingerprint.getXStream().toXML(fp, writer);
        try (Jedis jedis = JedisPoolManager.getJedis()) {
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

        try (Jedis jedis = JedisPoolManager.getJedis()) {
            loadedData = jedis.get(instanceId + id);
        } catch (JedisException e) {
            LOGGER.log(Level.WARNING, "Jedis failed in loading fingerprint: " + id, e);
            throw e;
        }

        if (loadedData == null) return null;

        Object loadedObject = null;
        Fingerprint loadedFingerprint;

        try (InputStream in = new ByteArrayInputStream(loadedData.getBytes(StandardCharsets.UTF_8))) {
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
        try (Jedis jedis = JedisPoolManager.getJedis()) {
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
        try (Jedis jedis = JedisPoolManager.getJedis()) {
            return jedis.smembers(instanceId).size() != 0;
        } catch (JedisException e) {
            LOGGER.log(Level.WARNING, "Failed to connect to Jedis", e);
            throw e;
        }
    }

    private String host = "localhost";
    private int port = 6379;
    private int database = 0;
    private boolean ssl;
    private int connectionTimeout = 2000;
    private int socketTimeout = 2000;
    private String credentialsId = "";

    public String getHost() {
        return host;
    }

    @DataBoundSetter
    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    @DataBoundSetter
    public void setPort(int port) {
        this.port = port;
    }

    public int getDatabase() {
        return database;
    }

    @DataBoundSetter
    public void setDatabase(int database) {
        this.database = database;
    }

    public boolean getSsl() {
        return this.ssl;
    }

    @DataBoundSetter
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    @DataBoundSetter
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    @DataBoundSetter
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public @NonNull String getUsername() {
        StandardUsernamePasswordCredentials credential = getCredential(credentialsId);
        return getUsernameFromCredential(credential);
    }

    public @NonNull String getPassword() {
        StandardUsernamePasswordCredentials credential = getCredential(credentialsId);
        return getPasswordFromCredential(credential);
    }

    private static @NonNull String getUsernameFromCredential(@CheckForNull StandardUsernamePasswordCredentials credential) {
        if (credential == null) {
            return "default";
        }
        String username = credential.getUsername();
        if (username.equals("")) {
            return "default";
        }
        return username;
    }

    private static @NonNull String getPasswordFromCredential(@CheckForNull StandardUsernamePasswordCredentials credential) {
        if (credential == null) {
            return "";
        }
        return credential.getPassword().getPlainText();
    }

    private static StandardUsernamePasswordCredentials getCredential(String id) {
        StandardUsernamePasswordCredentials credential = null;
        List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList());
        IdMatcher matcher = new IdMatcher(id);
        for (StandardUsernamePasswordCredentials c : credentials) {
            if (matcher.matches(c)) {
                credential = c;
            }
        }
        return credential;
    }

    @Extension
    public static class DescriptorImpl extends FingerprintStorageDescriptor {

        @Override
        public String getDisplayName() {
            return "Redis Fingerprint Storage";
        }

        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }
            return result
                    .includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM,
                            Jenkins.get(),
                            StandardUsernamePasswordCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.always()
                    )
                    .includeCurrentValue(credentialsId);
        }

        public FormValidation doCheckCredentialsId(@AncestorInPath Item item, @QueryParameter String value) {
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok();
                }
            }
            if (StringUtils.isBlank(value)) {
                return FormValidation.ok();
            }
            if (CredentialsProvider.listCredentials(
                    StandardUsernamePasswordCredentials.class,
                    Jenkins.get(),
                    ACL.SYSTEM,
                    Collections.emptyList(),
                    CredentialsMatchers.withId(value)
            ).isEmpty()) {
                return FormValidation.error("Cannot find currently selected credentials");
            }
            return FormValidation.ok();
        }

        @RequirePOST
        public FormValidation doTestRedisConnection(
                @QueryParameter("host") final String host,
                @QueryParameter("port") final int port,
                @QueryParameter("database") final int database,
                @QueryParameter("ssl") final boolean ssl,
                @QueryParameter("credentialsId") final String credentialsId,
                @QueryParameter("connectionTimeout") final int connectionTimeout,
                @QueryParameter("socketTimeout") final int socketTimeout
        ) throws IOException, ServletException {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.error("Need admin permission to perform this action");
            }
            try {
                testConnection(host, port, database, credentialsId, ssl, connectionTimeout, socketTimeout);
                return FormValidation.ok("Success");
            } catch (Exception e) {
                return FormValidation.error("Connection error : " + e.getMessage());
            }
        }

        private void testConnection (String host, int port, int database, String credentialsId, boolean ssl,
                                     int connectionTimeout, int socketTimeout) throws JedisException {
            Jedis jedis = new Jedis(host, port, connectionTimeout, socketTimeout, ssl);
            StandardUsernamePasswordCredentials credential = getCredential(credentialsId);
            String username = getUsernameFromCredential(credential);
            String password = getPasswordFromCredential(credential);
            jedis.auth(username, password);
            jedis.select(database);
            jedis.close();
        }

    }

}
