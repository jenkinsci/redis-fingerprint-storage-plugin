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

import jenkins.fingerprints.GlobalFingerprintConfiguration;

public class RedisConfiguration {

    public static void setConfiguration(String host, int port, int connectionTimeout, int socketTimeout,
                                        String credentialsId, int database, boolean ssl) {
        RedisFingerprintStorage redisFingerprintStorage = RedisFingerprintStorage.get();
        redisFingerprintStorage.setHost(host);
        redisFingerprintStorage.setPort(port);
        redisFingerprintStorage.setConnectionTimeout(connectionTimeout);
        redisFingerprintStorage.setSocketTimeout(socketTimeout);
        redisFingerprintStorage.setCredentialsId(credentialsId);
        redisFingerprintStorage.setDatabase(database);
        redisFingerprintStorage.setSsl(ssl);
        GlobalFingerprintConfiguration.get().setFingerprintStorage(redisFingerprintStorage);
    }

    public static void setConfiguration(String host, int port) {
        setConfiguration(host, port, 3000, 3000, "", 0, false);
    }

}
