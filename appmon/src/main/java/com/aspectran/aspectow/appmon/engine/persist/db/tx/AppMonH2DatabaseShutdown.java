/*
 * Copyright (c) 2020-present The Aspectran Project
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
package com.aspectran.aspectow.appmon.engine.persist.db.tx;

import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Profile;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A component to shut down the H2 database gracefully upon application context destruction.
 * This is typically used for embedded H2 databases.
 *
 * <p>Created: 2025. 2. 15.</p>
 */
@Component
@Bean(lazyDestroy = true)
@Profile("(appmon.standalone, !appmon.ext-persistence)")
public final class AppMonH2DatabaseShutdown {

    private static final Logger logger = LoggerFactory.getLogger(AppMonH2DatabaseShutdown.class);

    private final SqlSessionFactory sqlSessionFactory;

    /**
     * Instantiates a new AppMonH2DatabaseShutdown.
     * @param sqlSessionFactory the MyBatis SqlSessionFactory
     */
    @Autowired(required = false)
    public AppMonH2DatabaseShutdown(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * Executes the H2 database shutdown command.
     * This method is invoked when the bean is destroyed.
     */
    @Destroy(profile = "h2")
    public void shutdown() {
        if (sqlSessionFactory != null) {
            try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
                executeShutdown(sqlSession.getConnection());
            } catch (SQLException e) {
                if (isIgnorableException(e)) {
                    logger.debug("H2 database closed: {}", e.getMessage());
                } else {
                    logger.error("Failed to shutdown H2 database", e);
                }
            }
        }
    }

    /**
     * Executes the 'SHUTDOWN' command on the given database connection if it is an H2 database.
     * @param connection the database connection
     * @throws SQLException if a database access error occurs
     */
    public void executeShutdown(@NonNull Connection connection) throws SQLException {
        if ("H2".equals(connection.getMetaData().getDatabaseProductName())) {
            String url = connection.getMetaData().getURL();
            if (url != null) {
                String upperUrl = url.toUpperCase();
                if (upperUrl.contains("AUTO_SERVER=TRUE") || upperUrl.contains(":H2:TCP:") || upperUrl.contains(":H2:SSL:")) {
                    logger.debug("Skipping explicit H2 SHUTDOWN command in shared server mode: {}", url);
                    return;
                }
            }
            logger.info("Shutting down H2 database");
            connection.createStatement().execute("SHUTDOWN");
        } else {
            logger.info("Not shutting down non-H2 database");
        }
    }

    private boolean isIgnorableException(@NonNull SQLException e) {
        int errorCode = e.getErrorCode();
        // 90067: CONNECTION_BROKEN_1 ("Connection is broken: session closed")
        // 90098: DATABASE_CALLED_AT_SHUTDOWN ("The database has been closed")
        // 90007: DATABASE_ALREADY_CLOSED ("The database is already closed")
        if (errorCode == 90067 || errorCode == 90098 || errorCode == 90007) {
            return true;
        }
        String message = e.getMessage();
        return (message != null && (message.contains("Database is already closed") ||
                message.contains("session closed") ||
                message.contains("Connection is broken")));
    }

}
