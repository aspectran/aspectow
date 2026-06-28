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
package com.aspectran.aspectow.console.common.service;

import com.aspectran.aspectow.console.common.db.model.Vault;

import java.util.List;

/**
 * Service interface for vault management.
 */
public interface VaultService {

    /**
     * Retrieves a vault by its unique identifier.
     * @param vaultId the unique identifier of the desired vault
     * @return the vault associated with the specified ID, or null if no such vault exists
     */
    Vault getVaultById(Long vaultId);

    /**
     * Retrieves a list of all vault entities.
     * @return a list of {@code Vault} objects representing the stored vault entries
     */
    List<Vault> getVaultList();

    /**
     * Creates a new vault entry and encrypts the specified plain text.
     * @param vault the Vault object containing metadata for the new entry
     * @param plainText the plain text to be encrypted and stored in the vault
     */
    void createVault(Vault vault, String plainText);

    /**
     * Updates an existing vault entry with a new value and ensures proper encryption handling.
     * @param vault the vault entity to update
     * @param plainText the new plain text value to be encrypted and stored
     * @param existingEncryptedValue the existing encrypted value in the vault for verification
     */
    void updateVault(Vault vault, String plainText, String existingEncryptedValue);

    /**
     * Deletes a vault by its ID.
     * @param vaultId the unique identifier of the vault to be deleted
     */
    void deleteVault(Long vaultId);

    /**
     * Decrypts an encrypted value using the provided token type.
     * @param encryptedValue the encrypted value to be decrypted
     * @param tokenType the type of token used for decryption
     * @return the decrypted value as a string
     */
    String decrypt(String encryptedValue, String tokenType);

}
