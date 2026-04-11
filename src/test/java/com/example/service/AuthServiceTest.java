package com.example.service;

import com.example.model.LoginResult;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    private static final String ENCRYPTION_KEY = "MDEyMzQ1Njc4OWFiY2RlZg==";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final AuthService authService = new AuthService(ENCRYPTION_KEY);

    @Test
    void login_shouldReturnTeamAAndRegularRole_forUserA() {
        LoginResult result = authService.login(encrypt("a"), encrypt("a"));

        assertEquals("Team A", result.teamName());
        assertEquals("regular", result.userRole());
    }

    @Test
    void login_shouldReturnTeamBAndRegularRole_forUserB() {
        LoginResult result = authService.login(encrypt("b"), encrypt("b"));

        assertEquals("Team B", result.teamName());
        assertEquals("regular", result.userRole());
    }

    @Test
    void login_shouldReturnTeamYardenAndAdminRole_forYarden() {
        LoginResult result = authService.login(encrypt("yarden"), encrypt("yarden"));

        assertEquals("team yarden", result.teamName());
        assertEquals("admin", result.userRole());
    }

    @Test
    void login_shouldRejectInvalidPassword() {
        SecurityException exception = assertThrows(SecurityException.class,
            () -> authService.login(encrypt("a"), encrypt("wrong")));

        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void login_shouldRejectMalformedEncryptedValues() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> authService.login("not-encrypted", "still-not-encrypted"));

        assertEquals("Encrypted value must be in '<iv>:<ciphertext>' format", exception.getMessage());
    }

    private String encrypt(String plainText) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            byte[] key = Base64.getDecoder().decode(ENCRYPTION_KEY);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt test credential", ex);
        }
    }
}
