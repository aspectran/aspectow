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
package com.aspectran.aspectow.console.common.db.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing an encrypted token in the vault.
 */
public class Vault implements Serializable {

    @Serial
    private static final long serialVersionUID = -1234567890123456789L;

    private Long vaultId;
    private String label;
    private String tokenType;
    private String encryptedValue;
    private String description;
    private LocalDateTime validUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Gets the vault ID.
     * @return the vault ID
     */
    public Long getVaultId() {
        return vaultId;
    }

    /**
     * Sets the vault ID.
     * @param vaultId the vault ID
     */
    public void setVaultId(Long vaultId) {
        this.vaultId = vaultId;
    }

    /**
     * Gets the label of the vault entry.
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label of the vault entry.
     * @param label the label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the token type of the vault entry.
     * @return the token type
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Sets the token type of the vault entry.
     * @param tokenType the token type
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Gets the encrypted value.
     * @return the encrypted value
     */
    public String getEncryptedValue() {
        return encryptedValue;
    }

    /**
     * Sets the encrypted value.
     * @param encryptedValue the encrypted value
     */
    public void setEncryptedValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }

    /**
     * Gets the description.
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the expiration time of the vault entry.
     * @return the expiration time, or null if it does not expire
     */
    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    /**
     * Sets the expiration time of the vault entry.
     * @param validUntil the expiration time
     */
    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    /**
     * Gets the creation time.
     * @return the creation time
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation time.
     * @param createdAt the creation time
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the modification time.
     * @return the modification time
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the modification time.
     * @param updatedAt the modification time
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
