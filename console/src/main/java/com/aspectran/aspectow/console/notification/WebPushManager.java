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
package com.aspectran.aspectow.console.notification;

import com.aspectran.aspectow.console.common.db.mapper.NotificationMapper;
import com.aspectran.aspectow.console.common.db.model.PushSubscription;
import com.aspectran.aspectow.console.common.db.model.VapidKey;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Initialize;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

/**
 * Manages VAPID keys and performs Web Push encryption (RFC 8291 / aes128gcm)
 * and dispatching via standard Java HTTP Client without third-party dependencies.
 *
 * <p>Created: 2026-11-17</p>
 */
public class WebPushManager extends NotificationSupport {

    private static final Logger logger = LoggerFactory.getLogger(WebPushManager.class);

    private static final String EC_CURVE = "secp256r1";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String ECDSA_SHA256 = "SHA256withECDSA";
    private static final int RECORD_SIZE = 4096;

    private final SecureRandom secureRandom = new SecureRandom();
    private final HttpClient httpClient;
    private final NotificationMapper notificationMapper;

    private KeyPair vapidKeyPair;
    private String vapidPublicKeyBase64;

    @Autowired(required = false)
    public WebPushManager(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public WebPushManager() {
        this(null);
    }

    public String getVapidSubject() {
        return getWebPushSubject();
    }

    @Initialize
    public void initialize() {
        try {
            if (!isWebPushEnabled()) {
                logger.info("Web Push functionality is disabled by configuration (aspectow.console.notification.webPush.enabled=false)");
                return;
            }

            if (this.vapidKeyPair != null) {
                return;
            }

            // 1. Try to load cluster-wide VAPID key from Database first
            if (notificationMapper != null) {
                try {
                    VapidKey dbKey = instantActivity(() -> notificationMapper.getVapidKey());
                    if (dbKey != null && dbKey.getPublicKey() != null && dbKey.getPrivateKey() != null) {
                        setVapidKeys(dbKey.getPublicKey(), dbKey.getPrivateKey());
                        logger.info("Loaded cluster-wide VAPID key pair from database. Public key: {}", vapidPublicKeyBase64);
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("Could not query VAPID key from database, will check file fallback: {}", e.getMessage());
                }
            }

            // 2. Check local persistent file fallback
            File keyFile = resolveKeyFile();
            if (keyFile != null && keyFile.isFile()) {
                try (InputStream in = new FileInputStream(keyFile)) {
                    Properties props = new Properties();
                    props.load(in);
                    String pub = props.getProperty("publicKey");
                    String priv = props.getProperty("privateKey");
                    if (pub != null && priv != null) {
                        setVapidKeys(pub, priv);
                        logger.info("Loaded persistent VAPID key pair from file: {}", keyFile.getAbsolutePath());
                        // Sync to DB if mapper is available but DB was empty
                        syncKeyToDatabase(pub, priv);
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to read persistent VAPID key file: {}", keyFile.getAbsolutePath(), e);
                }
            }

            // 3. Generate new EC P-256 KeyPair
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec(EC_CURVE), secureRandom);
            this.vapidKeyPair = kpg.generateKeyPair();
            this.vapidPublicKeyBase64 = encodeBase64Url(toUncompressedPoint((ECPublicKey) vapidKeyPair.getPublic()));

            ECPrivateKey priv = (ECPrivateKey) vapidKeyPair.getPrivate();
            String privateKeyBase64 = encodeBase64Url(to32Bytes(priv.getS()));

            logger.info("Generated new cluster-wide VAPID EC P-256 key pair. Public key: {}", vapidPublicKeyBase64);

            // 4. Save to Database so all other cluster nodes can share it
            syncKeyToDatabase(vapidPublicKeyBase64, privateKeyBase64);

            // 5. Also persist to local file as backup
            if (keyFile != null) {
                try {
                    File parent = keyFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    Properties props = new Properties();
                    props.setProperty("publicKey", vapidPublicKeyBase64);
                    props.setProperty("privateKey", privateKeyBase64);
                    try (OutputStream out = new FileOutputStream(keyFile)) {
                        props.store(out, "Aspectow Console VAPID Key Pair");
                    }
                    logger.info("Saved persistent VAPID key pair to: {}", keyFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.warn("Failed to persist VAPID key pair to: {}", keyFile.getAbsolutePath(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to initialize VAPID key pair for WebPushManager", e);
        }
    }

    private void syncKeyToDatabase(String pub, String priv) {
        if (notificationMapper == null) {
            return;
        }
        try {
            instantActivity(() -> {
                VapidKey existing = notificationMapper.getVapidKey();
                if (existing == null) {
                    VapidKey vk = new VapidKey();
                    vk.setKeyId(1);
                    vk.setPublicKey(pub);
                    vk.setPrivateKey(priv);
                    try {
                        notificationMapper.insertVapidKey(vk);
                        logger.info("Stored cluster-wide VAPID key pair in database successfully.");
                    } catch (Exception ex) {
                        // In case another node inserted concurrently, load it
                        VapidKey concurrentKey = notificationMapper.getVapidKey();
                        if (concurrentKey != null) {
                            setVapidKeys(concurrentKey.getPublicKey(), concurrentKey.getPrivateKey());
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            logger.warn("Failed to sync VAPID key to database: {}", e.getMessage());
        }
    }

    @Nullable
    private File resolveKeyFile() {
        String workPath = System.getProperty("aspectran.workPath");
        if (workPath != null && !workPath.isBlank()) {
            return new File(workPath, "vapid-key.properties");
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            return new File(userHome, ".aspectow/vapid-key.properties");
        }
        return null;
    }

    private byte @NonNull [] to32Bytes(@NonNull BigInteger s) {
        byte[] raw = s.toByteArray();
        byte[] result = new byte[32];
        copyPadded(raw, result, 0, 32);
        return result;
    }

    /**
     * Sets the VAPID keys from Base64URL-encoded strings.
     * @param publicKeyBase64 the Base64URL-encoded public key (65 bytes uncompressed EC point)
     * @param privateKeyBase64 the Base64URL-encoded private key (32 bytes scalar)
     */
    public void setVapidKeys(String publicKeyBase64, String privateKeyBase64) {
        try {
            ECPublicKey pubKey = decodePublicKey(publicKeyBase64);
            ECPrivateKey privKey = decodePrivateKey(privateKeyBase64);
            this.vapidKeyPair = new KeyPair(pubKey, privKey);
            this.vapidPublicKeyBase64 = publicKeyBase64;
            logger.info("Configured VAPID keys. Public key: {}, Subject: {}", this.vapidPublicKeyBase64, getVapidSubject());
        } catch (Exception e) {
            logger.error("Failed to set VAPID keys", e);
        }
    }

    /**
     * Returns the Base64URL-encoded VAPID public key to be sent to browsers.
     * @return the VAPID public key, or null if Web Push is disabled
     */
    public String getVapidPublicKey() {
        if (!isWebPushEnabled()) {
            return null;
        }
        return vapidPublicKeyBase64;
    }

    /**
     * Encrypts and sends a Web Push notification to the given subscription.
     * @param subscription the target push subscription
     * @param payloadJson the payload string (typically JSON)
     * @return HTTP status code returned by the push service (e.g. 200, 201, 404, 410)
     * @throws Exception if an error occurs during encryption or HTTP transmission
     */
    public int sendPushNotification(@NonNull PushSubscription subscription, @NonNull String payloadJson) throws Exception {
        if (!isWebPushEnabled()) {
            logger.debug("Web Push is disabled; skipping notification to endpoint: {}", subscription.getEndpoint());
            return 200;
        }
        byte[] clientPublicKey = decodeBase64Url(subscription.getP256dhKey());
        byte[] clientAuthSecret = decodeBase64Url(subscription.getAuthKey());

        // 1. Encrypt payload using RFC 8291 (aes128gcm)
        byte[] encryptedBody = encryptPayload(payloadJson.getBytes(StandardCharsets.UTF_8), clientPublicKey, clientAuthSecret);

        // 2. Build VAPID JWT token
        URI endpointUri = URI.create(subscription.getEndpoint());
        String origin = endpointUri.getScheme() + "://" + endpointUri.getHost() +
                (endpointUri.getPort() != -1 ? ":" + endpointUri.getPort() : "");
        String vapidJwt = createVapidJwt(origin, getVapidSubject(), vapidKeyPair.getPrivate());

        // 3. Dispatch HTTP/2 POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpointUri)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/octet-stream")
                .header("Content-Encoding", "aes128gcm")
                .header("TTL", "86400")
                .header("Authorization", "vapid t=" + vapidJwt + ", k=" + vapidPublicKeyBase64)
                .POST(HttpRequest.BodyPublishers.ofByteArray(encryptedBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();

        if (statusCode >= 200 && statusCode < 300) {
            logger.debug("Web Push sent successfully to endpoint: {}, status: {}", subscription.getEndpoint(), statusCode);
        } else {
            logger.warn("Web Push server responded with status: {}, body: {}, endpoint: {}",
                    statusCode, response.body(), subscription.getEndpoint());
        }

        return statusCode;
    }

    /**
     * Encrypts payload bytes according to RFC 8291 (Message Encryption for Web Push).
     */
    byte[] encryptPayload(byte[] payload, byte[] clientPublicKeyBytes, byte[] clientAuthSecret) throws Exception {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return encryptPayload(payload, clientPublicKeyBytes, clientAuthSecret, salt, null);
    }

    /**
     * Encrypts payload bytes with specific salt and ephemeral key pair (for testing and RFC 8291 compliance).
     */
    byte[] encryptPayload(byte[] payload, byte[] clientPublicKeyBytes, byte[] clientAuthSecret,
                          byte[] salt, KeyPair ephemeralKeyPair) throws Exception {
        if (ephemeralKeyPair == null) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec(EC_CURVE), secureRandom);
            ephemeralKeyPair = kpg.generateKeyPair();
        }
        byte[] serverPublicKeyBytes = toUncompressedPoint((ECPublicKey) ephemeralKeyPair.getPublic());

        // Decode client public key
        ECPublicKey clientPublicKey = decodePublicKeyBytes(clientPublicKeyBytes);

        // Compute ECDH Shared Secret
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ephemeralKeyPair.getPrivate());
        ka.doPhase(clientPublicKey, true);
        byte[] sharedSecret = ka.generateSecret();

        // 1. IKM = HKDF-Extract(salt=clientAuthSecret, ikm=sharedSecret)
        byte[] ikm = hkdfExtract(clientAuthSecret, sharedSecret);

        // 2. PRK = HKDF-Expand(ikm, info="WebPush: info\0" || clientPublicKey || serverPublicKey, length=32)
        ByteArrayOutputStream infoStream = new ByteArrayOutputStream();
        infoStream.write("WebPush: info\0".getBytes(StandardCharsets.US_ASCII));
        infoStream.write(clientPublicKeyBytes);
        infoStream.write(serverPublicKeyBytes);
        byte[] prk = hkdfExpand(ikm, infoStream.toByteArray(), 32);

        // 3. Content Encryption Key (CEK) = HKDF-Expand(HKDF-Extract(salt, prk), "Content-Encoding: aes128gcm\0", 16)
        byte[] cekInfo = "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII);
        byte[] cek = hkdfExpand(hkdfExtract(salt, prk), cekInfo, 16);

        // 4. Nonce = HKDF-Expand(HKDF-Extract(salt, prk), "Content-Encoding: nonce\0", 12)
        byte[] nonceInfo = "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII);
        byte[] nonce = hkdfExpand(hkdfExtract(salt, prk), nonceInfo, 12);

        // 5. Add record padding delimiter (0x02) according to RFC 8291
        byte[] paddedPayload = new byte[payload.length + 1];
        System.arraycopy(payload, 0, paddedPayload, 0, payload.length);
        paddedPayload[payload.length] = 0x02;

        // 6. AES-GCM Encrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] ciphertext = cipher.doFinal(paddedPayload);

        // 7. Assemble RFC 8291 binary header + ciphertext
        // Format: salt (16 bytes) || record_size (4 bytes) || idlen (1 byte = 65) || server_public_key (65 bytes) || ciphertext
        ByteBuffer buffer = ByteBuffer.allocate(16 + 4 + 1 + serverPublicKeyBytes.length + ciphertext.length);
        buffer.put(salt);
        buffer.putInt(RECORD_SIZE);
        buffer.put((byte) serverPublicKeyBytes.length);
        buffer.put(serverPublicKeyBytes);
        buffer.put(ciphertext);
        return buffer.array();
    }

    /**
     * Creates a VAPID JWT token signed with ECDSA P-256 (ES256).
     */
    String createVapidJwt(String audience, String subject, PrivateKey privateKey) throws Exception {
        long now = Instant.now().getEpochSecond();
        long exp = now + 12 * 3600; // 12 hours

        String headerJson = "{\"alg\":\"ES256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"aud\":\"" + audience + "\",\"exp\":" + exp + ",\"sub\":\"" + subject + "\"}";

        String encodedHeader = encodeBase64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = encodeBase64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String unsignedToken = encodedHeader + "." + encodedPayload;

        Signature signature = Signature.getInstance(ECDSA_SHA256);
        signature.initSign(privateKey);
        signature.update(unsignedToken.getBytes(StandardCharsets.US_ASCII));
        byte[] derSignature = signature.sign();

        byte[] p1363Signature = derToP1363(derSignature);
        return unsignedToken + "." + encodeBase64Url(p1363Signature);
    }

    // --- Cryptographic & HKDF Utilities ---

    private byte[] hkdfExtract(byte[] salt, byte[] ikm) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        if (salt == null || salt.length == 0) {
            salt = new byte[32]; // 32 zeros
        }
        mac.init(new SecretKeySpec(salt, HMAC_SHA256));
        return mac.doFinal(ikm);
    }

    private byte[] hkdfExpand(byte[] prk, byte[] info, int length) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(prk, HMAC_SHA256));
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] t = new byte[0];
        int iterations = (int) Math.ceil((double) length / 32);

        for (int i = 1; i <= iterations; i++) {
            mac.reset();
            mac.update(t);
            if (info != null) {
                mac.update(info);
            }
            mac.update((byte) i);
            t = mac.doFinal();
            result.write(t);
        }

        byte[] all = result.toByteArray();
        return Arrays.copyOfRange(all, 0, length);
    }

    private byte[] toUncompressedPoint(ECPublicKey publicKey) {
        ECPoint point = publicKey.getW();
        byte[] x = point.getAffineX().toByteArray();
        byte[] y = point.getAffineY().toByteArray();

        byte[] result = new byte[65];
        result[0] = 0x04; // uncompressed format indicator
        copyPadded(x, result, 1, 32);
        copyPadded(y, result, 33, 32);
        return result;
    }

    private ECPublicKey decodePublicKey(String base64Url) throws Exception {
        return decodePublicKeyBytes(decodeBase64Url(base64Url));
    }

    private ECPublicKey decodePublicKeyBytes(byte @NonNull [] keyBytes) throws Exception {
        if (keyBytes.length != 65 || keyBytes[0] != 0x04) {
            throw new IllegalArgumentException("Invalid uncompressed EC public key format");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(keyBytes, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(keyBytes, 33, 65));
        ECPoint point = new ECPoint(x, y);

        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec(EC_CURVE));
        ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);

        KeyFactory kf = KeyFactory.getInstance("EC");
        return (ECPublicKey) kf.generatePublic(new ECPublicKeySpec(point, ecSpec));
    }

    private ECPrivateKey decodePrivateKey(String base64Url) throws Exception {
        byte[] privBytes = decodeBase64Url(base64Url);
        BigInteger s = new BigInteger(1, privBytes);

        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec(EC_CURVE));
        ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);

        KeyFactory kf = KeyFactory.getInstance("EC");
        return (ECPrivateKey) kf.generatePrivate(new ECPrivateKeySpec(s, ecSpec));
    }

    private void copyPadded(byte @NonNull [] src, byte[] dest, int destOffset, int length) {
        int srcOffset = 0;
        int srcLength = src.length;
        if (srcLength > length && src[0] == 0) {
            srcOffset = 1;
            srcLength--;
        }
        int padding = length - srcLength;
        Arrays.fill(dest, destOffset, destOffset + padding, (byte) 0);
        System.arraycopy(src, srcOffset, dest, destOffset + padding, srcLength);
    }

    /**
     * Converts ASN.1 DER signature to IEEE P1363 (r || s, 64 bytes).
     */
    private byte @NonNull [] derToP1363(byte @NonNull [] der) throws IOException {
        if (der.length < 8 || der[0] != 0x30) {
            throw new IOException("Invalid DER signature format");
        }
        int offset = 2;
        if ((der[1] & 0x80) != 0) {
            offset += (der[1] & 0x7F);
        }

        if (der[offset++] != 0x02) throw new IOException("Expected INTEGER for r");
        int rLen = der[offset++];
        int rOffset = offset;
        offset += rLen;

        if (der[offset++] != 0x02) throw new IOException("Expected INTEGER for s");
        int sLen = der[offset++];
        int sOffset = offset;

        byte[] p1363 = new byte[64];
        copyPadded(Arrays.copyOfRange(der, rOffset, rOffset + rLen), p1363, 0, 32);
        copyPadded(Arrays.copyOfRange(der, sOffset, sOffset + sLen), p1363, 32, 32);
        return p1363;
    }

    private String encodeBase64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] decodeBase64Url(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

}
