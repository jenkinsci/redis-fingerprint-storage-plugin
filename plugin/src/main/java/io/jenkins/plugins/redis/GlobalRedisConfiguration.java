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
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Global configuration for Redis Fingerprint Storage.
 *
 * @author Sumit Sarin
 */
@Extension
@Symbol("redis")
public class GlobalRedisConfiguration extends GlobalConfiguration {

    private boolean enabled;
    private String host = "localhost";
    private int port = 6379;
    private int database = 0;
    private boolean ssl;
    private int connectionTimeout = 2000;
    private int socketTimeout = 2000;
    private String credentialsId = "";

    public GlobalRedisConfiguration() {
        load();
        setEnabled(this.enabled);
    }

    public static GlobalRedisConfiguration get() {
        return GlobalConfiguration.all().getInstance(GlobalRedisConfiguration.class);
    }

    public boolean getEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled){
            System.setProperty("FingerprintStorageEngine", "io.jenkins.plugins.redis.RedisFingerprintStorage");
        } else {
            System.setProperty("FingerprintStorageEngine", "jenkins.fingerprints.FileFingerprintStorage");
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public boolean getSsl() {
        return this.ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public @NonNull String getUsername() {
        StandardUsernamePasswordCredentials credential = getCredential(credentialsId);
        return getUsernameFromCredential(credential);
    }

    private @NonNull String getUsernameFromCredential(@CheckForNull StandardUsernamePasswordCredentials credential) {
        if (credential == null) {
            return "default";
        }
        String username = credential.getUsername();
        if (username.equals("")) {
            return "default";
        }
        return username;
    }

    public @NonNull String getPassword() {
        StandardUsernamePasswordCredentials credential = getCredential(credentialsId);
        return getPasswordFromCredential(credential);
    }

    private @NonNull String getPasswordFromCredential(@CheckForNull StandardUsernamePasswordCredentials credential) {
        if (credential == null) {
            return "";
        }
        return credential.getPassword().getPlainText();
    }

    private StandardUsernamePasswordCredentials getCredential(String id) {
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

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        json = json.getJSONObject("redis");
        setEnabled(json.getBoolean("enabled"));
        setHost(json.getString("host"));
        setPort(json.getInt("port"));
        setDatabase(json.getInt("database"));
        setSsl(json.getBoolean("ssl"));
        setCredentialsId(json.getString("credentialsId"));
        setConnectionTimeout(json.getInt(("connectionTimeout")));
        setConnectionTimeout(json.getInt(("socketTimeout")));
        save();
        return true;
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
    }

}
