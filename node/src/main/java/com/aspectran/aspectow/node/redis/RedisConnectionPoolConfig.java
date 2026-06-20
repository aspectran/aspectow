/*
 * Copyright (c) 2019-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aspectran.aspectow.node.redis;

import com.aspectran.utils.StringUtils;
import com.aspectran.utils.ToStringBuilder;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jspecify.annotations.NonNull;

import java.util.Properties;

/**
 * Redis connection pool configuration based on Lettuce.
 *
 * <p>Created: 2019/12/07</p>
 */
public class RedisConnectionPoolConfig extends GenericObjectPoolConfig<StatefulRedisConnection<String, String>> {

    private RedisURI redisURI;

    private ClientOptions clientOptions;

    /**
     * Instantiates a new RedisConnectionPoolConfig with default settings.
     */
    public RedisConnectionPoolConfig() {
        super();
    }

    /**
     * Instantiates a new RedisConnectionPoolConfig using the specified properties.
     * @param properties the properties containing configuration values
     */
    public RedisConnectionPoolConfig(@NonNull Properties properties) {
        super();
        setUri(properties.getProperty("aspectow.redis.uri"));
    }

    /**
     * Returns the Redis URI for connection.
     * @return the Redis URI
     */
    public RedisURI getRedisURI() {
        return redisURI;
    }

    /**
     * Sets the Redis URI for connection.
     * @param redisURI the Redis URI
     * @throws IllegalArgumentException if the {@code redisURI} is null
     */
    public void setRedisURI(RedisURI redisURI) {
        if (redisURI == null) {
            throw new IllegalArgumentException("redisURI must not be null");
        }
        this.redisURI = redisURI;
    }

    /**
     * Sets the connection URI for Redis.
     * @param uri the URI string
     * @throws IllegalArgumentException if the {@code uri} is null or empty
     */
    public void setUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            throw new IllegalArgumentException("uri must not be null or empty");
        }
        this.redisURI = RedisURI.create(uri);
    }

    /**
     * Returns the Lettuce client options.
     * @return the client options, or {@code null} if not configured
     */
    public ClientOptions getClientOptions() {
        return clientOptions;
    }

    /**
     * Sets the Lettuce client options.
     * @param clientOptions the client options to set
     */
    public void setClientOptions(ClientOptions clientOptions) {
        this.clientOptions = clientOptions;
    }

    /**
     * Returns a string representation of the Redis connection pool configuration.
     * @return a string representation of the configuration
     */
    @Override
    public String toString() {
        ToStringBuilder tsb = new ToStringBuilder();
        tsb.append("redisURI", redisURI);
        tsb.append("clientOptions", clientOptions);
        return tsb.toString();
    }

}
