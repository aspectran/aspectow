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
package com.aspectran.aspectow.console.common.db.mapper;

import com.aspectran.aspectow.console.common.db.model.Vault;
import com.aspectran.aspectow.console.common.db.tx.ConsoleSqlMapperProvider;
import com.aspectran.aspectow.console.common.pagination.PageInfo;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlMapperAccess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * The MyBatis mapper interface for vault data.
 */
@Mapper
public interface VaultMapper {

    /**
     * Retrieves a vault by its ID.
     * @param vaultId the vault ID
     * @return the vault, or null if not found
     */
    Vault getVaultById(Long vaultId);

    /**
     * Retrieves vaults matching the search keyword.
     * @param pageInfo the pagination info
     * @param searchKeyword the search keyword
     * @return the list of vaults
     */
    List<Vault> getVaultList(@Param("pageInfo") PageInfo pageInfo, @Param("searchKeyword") String searchKeyword);

    /**
     * Retrieves the total count of vaults matching the search keyword.
     * @param searchKeyword the search keyword
     * @return the total count of vaults
     */
    long getVaultCount(@Param("searchKeyword") String searchKeyword);

    /**
     * Inserts a new vault.
     * @param vault the vault to insert
     * @return the number of affected rows
     */
    int insertVault(Vault vault);

    /**
     * Updates an existing vault.
     * @param vault the vault to update
     * @return the number of affected rows
     */
    int updateVault(Vault vault);

    /**
     * Deletes a vault by its ID.
     * @param vaultId the vault ID to delete
     * @return the number of affected rows
     */
    int deleteVault(Long vaultId);

    /**
     * Data Access Object (DAO) for {@link VaultMapper}.
     */
    @Component
    @Bean("console.vaultDao")
    class Dao extends SqlMapperAccess<VaultMapper> implements VaultMapper {

        /**
         * Constructs a new Dao.
         * @param sqlMapperProvider the SQL mapper provider
         */
        @Autowired
        public Dao(ConsoleSqlMapperProvider sqlMapperProvider) {
            super(sqlMapperProvider);
        }

        @Override
        public Vault getVaultById(Long vaultId) {
            return mapper().getVaultById(vaultId);
        }

        @Override
        public List<Vault> getVaultList(PageInfo pageInfo, String searchKeyword) {
            return mapper().getVaultList(pageInfo, searchKeyword);
        }

        @Override
        public long getVaultCount(String searchKeyword) {
            return mapper().getVaultCount(searchKeyword);
        }

        @Override
        public int insertVault(Vault vault) {
            return mapper().insertVault(vault);
        }

        @Override
        public int updateVault(Vault vault) {
            return mapper().updateVault(vault);
        }

        @Override
        public int deleteVault(Long vaultId) {
            return mapper().deleteVault(vaultId);
        }
    }

}
