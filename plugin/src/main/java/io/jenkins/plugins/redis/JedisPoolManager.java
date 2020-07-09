package io.jenkins.plugins.redis;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionList;
import jenkins.fingerprints.GlobalFingerprintConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

public class JedisPoolManager {

    private volatile static JedisPool jedisPool;

    static void createJedisPoolFromConfig() {
        RedisFingerprintStorage redisFingerprintStorage = (RedisFingerprintStorage) GlobalFingerprintConfiguration.get().getFingerprintStorage();
        createJedisPool(redisFingerprintStorage.getHost(), redisFingerprintStorage.getPort(),
                redisFingerprintStorage.getConnectionTimeout(), redisFingerprintStorage.getSocketTimeout(),
                redisFingerprintStorage.getUsername(), redisFingerprintStorage.getPassword(),
                redisFingerprintStorage.getDatabase(), redisFingerprintStorage.getSsl());
    }

    static void createJedisPool(
            String host, int port, int connectionTimeout, int socketTimeout, String username, String password,
            int database, boolean ssl) {
        jedisPool = new JedisPool(new JedisPoolConfig(), host, port, connectionTimeout, socketTimeout, username,
                password, database, "Jenkins", ssl);
    }

    static @NonNull Jedis getJedis() throws JedisException {
        return jedisPool.getResource();
    }
}
