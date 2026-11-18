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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link WebPushManager}.
 *
 * <p>Created: 2026-11-17</p>
 */
class WebPushManagerTest {

    private WebPushManager webPushManager;

    @BeforeEach
    void setUp() {
        webPushManager = new WebPushManager();
        webPushManager.setNotificationEnabled(true);
        webPushManager.setWebPushEnabled(true);
        webPushManager.initialize();
    }

    @Test
    void testDisabledByDefault() {
        WebPushManager defaultManager = new WebPushManager();
        assertFalse(defaultManager.isNotificationEnabled(), "Notification must be disabled by default (opt-in)");
        assertFalse(defaultManager.isWebPushEnabled(), "Web Push must be disabled by default (opt-in)");
    }

    @Test
    void testVapidKeyInitialization() {
        String publicKey = webPushManager.getVapidPublicKey();
        assertNotNull(publicKey);
        byte[] decoded = Base64.getUrlDecoder().decode(publicKey);
        assertEquals(65, decoded.length, "Uncompressed EC P-256 public key must be exactly 65 bytes");
        assertEquals(0x04, decoded[0], "Uncompressed EC public key must start with 0x04");
    }

    @Test
    void testVapidJwtCreation() throws Exception {
        String audience = "https://fcm.googleapis.com";
        String subject = "mailto:test@aspectran.com";

        // Access internal createVapidJwt
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = kpg.generateKeyPair();

        String jwt = webPushManager.createVapidJwt(audience, subject, kp.getPrivate());
        assertNotNull(jwt);

        String[] parts = jwt.split("\\.");
        assertEquals(3, parts.length, "JWT must consist of 3 parts (header.claims.signature)");

        String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        assertTrue(header.contains("\"alg\":\"ES256\""));

        String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        assertTrue(claims.contains("\"aud\":\"https://fcm.googleapis.com\""));
        assertTrue(claims.contains("\"sub\":\"mailto:test@aspectran.com\""));

        byte[] sig = Base64.getUrlDecoder().decode(parts[2]);
        assertEquals(64, sig.length, "IEEE P1363 ES256 signature must be exactly 64 bytes");
    }

    @Test
    void testPayloadEncryption() throws Exception {
        // Mock client EC P-256 key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair clientKeyPair = kpg.generateKeyPair();

        // Convert client public key to uncompressed 65 bytes
        ECPublicKey pub = (ECPublicKey) clientKeyPair.getPublic();
        byte[] clientPubBytes = new byte[65];
        clientPubBytes[0] = 0x04;
        byte[] x = pub.getW().getAffineX().toByteArray();
        byte[] y = pub.getW().getAffineY().toByteArray();
        copyPadded(x, clientPubBytes, 1, 32);
        copyPadded(y, clientPubBytes, 33, 32);

        byte[] authSecret = new byte[16];
        new java.security.SecureRandom().nextBytes(authSecret);

        byte[] payload = "{\"title\":\"Hello Aspectow!\"}".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = webPushManager.encryptPayload(payload, clientPubBytes, authSecret);

        assertNotNull(encrypted);
        // Header structure: salt (16) + record_size (4) + idlen (1) + server_key (65) = 86 bytes
        assertTrue(encrypted.length > 86 + payload.length);

        ByteBuffer buf = ByteBuffer.wrap(encrypted);
        byte[] salt = new byte[16];
        buf.get(salt);
        int recordSize = buf.getInt();
        assertEquals(4096, recordSize);

        int idLen = buf.get() & 0xFF;
        assertEquals(65, idLen);

        byte[] serverKey = new byte[65];
        buf.get(serverKey);
        assertEquals(0x04, serverKey[0]);
    }

    @Test
    void testRfc8291ComplianceVector() throws Exception {
        // RFC 8291 Appendix A Official Test Vector
        String uaPubB64 = "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
        String authSecretB64 = "BTBZMqHH6r4Tts7J_aSIgg";
        String asPubB64 = "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8";
        String asPrivB64 = "yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw";
        String saltB64 = "DGv6ra1nlYgDCS1FRnbzlw";
        String expectedCiphertextB64 = "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN";

        byte[] uaPubBytes = Base64.getUrlDecoder().decode(uaPubB64);
        byte[] authSecretBytes = Base64.getUrlDecoder().decode(authSecretB64);
        byte[] asPubBytes = Base64.getUrlDecoder().decode(asPubB64);
        byte[] asPrivBytes = Base64.getUrlDecoder().decode(asPrivB64);
        byte[] saltBytes = Base64.getUrlDecoder().decode(saltB64);

        // Reconstruct KeyPair for Application Server
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        ECPoint asPoint = new ECPoint(
                new BigInteger(1, Arrays.copyOfRange(asPubBytes, 1, 33)),
                new BigInteger(1, Arrays.copyOfRange(asPubBytes, 33, 65))
        );
        PublicKey asPublicKey = keyFactory.generatePublic(new ECPublicKeySpec(asPoint, ecParameterSpec));
        PrivateKey asPrivateKey = keyFactory.generatePrivate(new ECPrivateKeySpec(new BigInteger(1, asPrivBytes), ecParameterSpec));
        KeyPair asKeyPair = new KeyPair(asPublicKey, asPrivateKey);

        byte[] plaintext = "When I grow up, I want to be a watermelon".getBytes(StandardCharsets.US_ASCII);

        byte[] encrypted = webPushManager.encryptPayload(plaintext, uaPubBytes, authSecretBytes, saltBytes, asKeyPair);
        String actualB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);

        assertEquals(expectedCiphertextB64, actualB64, "RFC 8291 official test vector ciphertext must match byte-for-byte");
    }

    private void copyPadded(byte[] src, byte[] dest, int destOffset, int length) {
        int srcOffset = 0;
        int srcLength = src.length;
        if (srcLength > length && src[0] == 0) {
            srcOffset = 1;
            srcLength--;
        }
        int padding = length - srcLength;
        System.arraycopy(src, srcOffset, dest, destOffset + padding, srcLength);
    }

}
