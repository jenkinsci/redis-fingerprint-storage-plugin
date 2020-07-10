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

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.fingerprints.FingerprintStorageDescriptor;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;

public class RedisFingerprintStorageDescriptor extends FingerprintStorageDescriptor {

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

    protected void testConnection (String host, int port, int database, String credentialsId, boolean ssl,
                                 int connectionTimeout, int socketTimeout) throws JedisException {
        Jedis jedis = new Jedis(host, port, connectionTimeout, socketTimeout, ssl);
        StandardUsernamePasswordCredentials credential = CredentialHelper.getCredential(credentialsId);
        String username = CredentialHelper.getUsernameFromCredential(credential);
        String password = CredentialHelper.getPasswordFromCredential(credential);
        jedis.auth(username, password);
        jedis.select(database);
        jedis.close();
    }

}
