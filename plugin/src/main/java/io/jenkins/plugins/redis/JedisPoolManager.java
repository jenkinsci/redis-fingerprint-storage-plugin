package io.jenkins.plugins.redis;

import edu.umd.cs.findbugs.annotations.NonNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

public class JedisPoolManager {

    private volatile static JedisPool jedisPool;

    static void createJedisPoolFromConfig() {
        GlobalRedisConfiguration config = GlobalRedisConfiguration.get();
        createJedisPool(config.getHost(), config.getPort(), config.getConnectionTimeout(), config.getSocketTimeout(),
                config.getUsername(), config.getPassword(), config.getDatabase(), config.getSsl());
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
