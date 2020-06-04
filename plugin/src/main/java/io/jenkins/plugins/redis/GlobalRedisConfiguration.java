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

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.StaplerRequest;

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

    public GlobalRedisConfiguration() {
        load();
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

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        json = json.getJSONObject("redis");
        setEnabled(json.getBoolean("enabled"));
        setHost(json.getString("host"));
        setPort(json.getInt("port"));
        setDatabase(json.getInt("database"));
        save();
        return true;
    }

}