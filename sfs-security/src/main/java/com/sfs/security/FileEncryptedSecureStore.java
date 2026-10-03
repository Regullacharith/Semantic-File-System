package com.sfs.security;

import com.sfs.contracts.security.EncryptionMetadata;
import com.sfs.contracts.security.SecretRecord;
import com.sfs.contracts.security.SecretSubmission;
import com.sfs.contracts.security.SecretVault;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

public final class FileEncryptedSecureStore implements SecretVault {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String FIELD_SEPARATOR = "\\|";

    private final Path storeFile;
    private final KeyManager keyManager;
    private final ReentrantLock lock = new ReentrantLock();
    private final Properties records = new Properties();

    public FileEncryptedSecureStore(Path secureDirectory, KeyManager keyManager) {
        Objects.requireNonNull(secureDirectory, "secureDirectory must not be null");
        this.keyManager = Objects.requireNonNull(keyManager, "keyManager must not be null");
        this.storeFile = secureDirectory.resolve("secrets.properties");
        load();
    }

    @Override
    public java.util.Optional<com.sfs.contracts.security.SecretRecord> store(
            com.sfs.contracts.security.SecretSubmission submission) {
        return java.util.Optional.of(encryptAndStore(submission));
    }

    private void persistRecord(SecretRecord record) {
        lock.lock();
        try {
            records.setProperty(record.referenceId(), encode(record));
            persist();
        } finally {
            lock.unlock();
        }
    }

    public SecretRecord encryptAndStore(SecretSubmission submission) {
        Objects.requireNonNull(submission, "submission must not be null");
        if (!submission.sensitiveType().isReversibleByDefault()) {
            throw new IllegalArgumentException(
                    submission.sensitiveType()
                            + " is never reversibly stored; refusing to encrypt it");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyManager.currentKey(), "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(submission.plaintextValue()
                    .getBytes(StandardCharsets.UTF_8));
            SecretRecord record = new SecretRecord(
                    submission.referenceId(), submission.objectId(),
                    submission.sensitiveType(), "authorized-resolution-only",
                    Base64.getEncoder().encodeToString(ciphertext),
                    new EncryptionMetadata(EncryptionMetadata.AES_256_GCM,
                            keyManager.currentKeyId(),
                            Base64.getEncoder().encodeToString(iv),
                            ciphertext.length, Instant.now()));
            persistRecord(record);
            return record;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "the secret could not be encrypted; it was not stored and the "
                            + "reference must be treated as non-resolvable", e);
        }
    }

    @Override
    public Optional<SecretRecord> find(String referenceId) {
        if (referenceId == null || referenceId.isBlank()) {
            return Optional.empty();
        }
        lock.lock();
        try {
            String encoded = records.getProperty(referenceId);
            return Optional.ofNullable(encoded)
                    .map(value -> decode(referenceId, value));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(String referenceId) {
        return find(referenceId).isPresent();
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return records.size();
        } finally {
            lock.unlock();
        }
    }

    public String decrypt(SecretRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        if (!record.metadata().keyId().equals(keyManager.currentKeyId())) {
            throw new IllegalStateException(
                    "the secret was encrypted under key " + record.metadata().keyId()
                            + " which is not the current key " + keyManager.currentKeyId()
                            + "; resolution refused");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyManager.currentKey(), "AES"),
                    new GCMParameterSpec(TAG_BITS, Base64.getDecoder().decode(
                            record.metadata().initializationVectorBase64())));
            return new String(cipher.doFinal(Base64.getDecoder()
                    .decode(record.ciphertextBase64())), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "the secret could not be decrypted; resolution refused", e);
        }
    }

    public Path storeFile() {
        return storeFile;
    }

    private String encode(SecretRecord record) {
        return String.join("|",
                record.objectId(),
                record.sensitiveType().name(),
                record.reversible(),
                record.ciphertextBase64(),
                record.metadata().algorithm(),
                record.metadata().keyId(),
                record.metadata().initializationVectorBase64(),
                String.valueOf(record.metadata().ciphertextBytes()),
                record.metadata().encryptedAt().toString());
    }

    private SecretRecord decode(String referenceId, String encoded) {
        String[] parts = encoded.split(FIELD_SEPARATOR, -1);
        return new SecretRecord(referenceId, parts[0], sensitiveTypeOf(parts[1]),
                parts[2], parts[3], new EncryptionMetadata(parts[4], parts[5],
                parts[6], Integer.parseInt(parts[7]), Instant.parse(parts[8])));
    }

    private static com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType
            sensitiveTypeOf(String name) {
        return com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType
                .valueOf(name);
    }

    private void load() {
        if (Files.exists(storeFile)) {
            try (InputStream in = Files.newInputStream(storeFile)) {
                records.load(in);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "the secure store could not be read; refusing to run with "
                                + "possibly incomplete secret state", e);
            }
        }
    }

    private void persist() {
        try {
            Files.createDirectories(storeFile.getParent());
            try (OutputStream out = Files.newOutputStream(storeFile)) {
                records.store(out, null);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "the secure store could not be persisted; the secret state may "
                            + "be incomplete and must not be trusted silently", e);
        }
    }
}
