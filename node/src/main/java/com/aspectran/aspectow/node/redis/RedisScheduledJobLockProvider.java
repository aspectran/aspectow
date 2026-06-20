/*
 * Copyright (c) 2026-present The Aspectran Project
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

import com.aspectran.core.component.schedule.ScheduledJobLockProvider;
import com.aspectran.utils.Assert;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis-based implementation of {@link ScheduledJobLockProvider} using Lettuce.
 * It uses the 'SET NX' pattern to ensure only one node in the cluster executes
 * a specific job at a time.
 */
public class RedisScheduledJobLockProvider implements ScheduledJobLockProvider {

    private static final Logger logger = LoggerFactory.getLogger(RedisScheduledJobLockProvider.class);

    private final RedisConnectionPool connectionPool;

    private final String clusterId;

    private long lockTimeoutSeconds = 60; // Default 1 minute

    private boolean releasedOnUnlock = false;

    /**
     * Instantiates a new RedisScheduledJobLockProvider with the connection pool and cluster ID.
     * @param connectionPool the Redis connection pool
     * @param clusterId the unique cluster identifier, can be {@code null}
     */
    public RedisScheduledJobLockProvider(RedisConnectionPool connectionPool, String clusterId) {
        Assert.notNull(connectionPool, "connectionPool must not be null");
        this.connectionPool = connectionPool;
        this.clusterId = clusterId;
    }

    /**
     * Sets the lock timeout in seconds.
     * @param lockTimeoutSeconds the lock timeout in seconds
     */
    public void setLockTimeoutSeconds(long lockTimeoutSeconds) {
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }

    /**
     * Sets whether to immediately release the lock upon calling unlock.
     * <p>If set to {@code false} (default), the lock key is preserved in Redis
     * until it expires naturally via TTL. This is highly recommended when
     * using timestamp-based lock keys to prevent nodes with clock drifts
     * from re-executing the same time slot.</p>
     * @param releasedOnUnlock true to delete the key immediately, false otherwise
     */
    public void setReleasedOnUnlock(boolean releasedOnUnlock) {
        this.releasedOnUnlock = releasedOnUnlock;
    }

    /**
     * Acquires a distributed lock for the specified job key.
     * @param lockKey the unique key representing the job to lock
     * @return {@code true} if the lock was successfully acquired, otherwise {@code false}
     */
    @Override
    public boolean lock(String lockKey) {
        String fullKey = getFullKey(lockKey);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            RedisCommands<String, String> sync = connection.sync();
            String result = sync.set(fullKey, "locked", SetArgs.Builder.nx().ex(lockTimeoutSeconds));
            boolean acquired = "OK".equals(result);
            if (acquired && logger.isTraceEnabled()) {
                logger.trace("Acquired distributed lock for job: {}", fullKey);
            }
            return acquired;
        } catch (Exception e) {
            logger.error("Failed to acquire distributed lock for job: {}", fullKey, e);
            return false;
        }
    }

    /**
     * Releases the distributed lock for the specified job key.
     * @param lockKey the unique key representing the job to unlock
     */
    @Override
    public void unlock(String lockKey) {
        if (!releasedOnUnlock) {
            return;
        }
        String fullKey = getFullKey(lockKey);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            connection.sync().del(fullKey);
            if (logger.isTraceEnabled()) {
                logger.trace("Released distributed lock for job: {}", fullKey);
            }
        } catch (Exception e) {
            logger.warn("Failed to release distributed lock for job: {}", fullKey, e);
        }
    }

    @NonNull
    private String getFullKey(String lockKey) {
        return "job-lock:" + (clusterId != null ? clusterId + ":" : "") + lockKey;
    }

}
