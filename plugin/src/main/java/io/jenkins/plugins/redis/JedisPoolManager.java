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

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.fingerprints.GlobalFingerprintConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

public enum JedisPoolManager {

    INSTANCE;

    JedisPoolManager() {
        createJedisPoolFromConfig();
    }

    private volatile JedisPool jedisPool;

    void createJedisPoolFromConfig() {
        RedisFingerprintStorage redisFingerprintStorage = (RedisFingerprintStorage) GlobalFingerprintConfiguration.get()
                .getFingerprintStorage();

        createJedisPool(redisFingerprintStorage.getHost(), redisFingerprintStorage.getPort(),
                redisFingerprintStorage.getConnectionTimeout(), redisFingerprintStorage.getSocketTimeout(),
                redisFingerprintStorage.getUsername(), redisFingerprintStorage.getPassword(),
                redisFingerprintStorage.getDatabase(), redisFingerprintStorage.getSsl());
    }

    synchronized void createJedisPool(
            String host, int port, int connectionTimeout, int socketTimeout, String username, String password,
            int database, boolean ssl) {

        jedisPool = new JedisPool(new JedisPoolConfig(), host, port, connectionTimeout, socketTimeout, username,
                password, database, "Jenkins", ssl);
    }

    @NonNull Jedis getJedis() throws JedisException {
        return jedisPool.getResource();
    }

}
