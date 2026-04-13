package com.example.service;

import com.example.model.LoginResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private static final String ENCRYPTION_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final Map<String, UserAccount> USERS = Map.of(
        "a", new UserAccount("a", "Team A", "regular"),
        "b", new UserAccount("b", "Team B", "regular"),
        "yarden", new UserAccount("yarden", "team yarden", "admin")
    );

    private final SecretKeySpec secretKey;

    public AuthService(@Value("${app.auth.encryption-key:MDEyMzQ1Njc4OWFiY2RlZg==}") String encryptionKey) {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public LoginResult login(String encryptedUsername, String encryptedPassword) {
        String username = decrypt(encryptedUsername);
        String password = decrypt(encryptedPassword);

        validateRequired("username", username);
        validateRequired("password", password);

        UserAccount account = USERS.get(username.trim().toLowerCase(Locale.ROOT));
        if (account == null || !account.password().equals(password)) {
            throw new SecurityException("Invalid credentials");
        }

        return new LoginResult(account.teamName(), account.userRole());
    }

    public boolean isAuthenticatedUser(String userId) {
        return USERS.containsKey(normalizeUserId(userId));
    }

    public boolean isAdminUser(String userId) {
        UserAccount account = USERS.get(normalizeUserId(userId));
        return account != null && "admin".equalsIgnoreCase(account.userRole());
    }

    private String decrypt(String encryptedValue) {
        validateRequired("encryptedValue", encryptedValue);

        String[] parts = encryptedValue.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Encrypted value must be in '<iv>:<ciphertext>' format");
        }

        try {
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Encrypted value is not valid Base64", ex);
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("Failed to decrypt credentials", ex);
        }
    }

    private void validateRequired(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
    }

    private String normalizeUserId(String userId) {
        return userId == null ? null : userId.trim().toLowerCase(Locale.ROOT);
    }

    private record UserAccount(String password, String teamName, String userRole) {
    }
}
