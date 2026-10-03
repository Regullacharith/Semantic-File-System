package com.sfs.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public final class FileKeyManager implements KeyManager {

    private final Path keyFile;
    private final byte[] key;
    private final String keyId;

    public FileKeyManager(Path keysDirectory) {
        Objects.requireNonNull(keysDirectory, "keysDirectory must not be null");
        this.keyFile = keysDirectory.resolve("master.key");
        this.key = loadOrCreate();
        this.keyId = keyIdOf(key);
    }

    @Override
    public String currentKeyId() {
        return keyId;
    }

    @Override
    public byte[] currentKey() {
        return key.clone();
    }

    public Path keyFile() {
        return keyFile;
    }

    private byte[] loadOrCreate() {
        try {
            if (Files.exists(keyFile)) {
                return Base64.getDecoder()
                        .decode(Files.readString(keyFile, StandardCharsets.UTF_8)
                                .strip());
            }
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            Files.createDirectories(keyFile.getParent());
            Files.writeString(keyFile,
                    Base64.getEncoder().encodeToString(generated),
                    StandardCharsets.UTF_8);
            try {
                Files.setPosixFilePermissions(keyFile,
                        PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
                java.util.logging.Logger.getLogger(getClass().getName())
                        .warning("POSIX permissions unavailable for the key file; "
                                + "restrict the directory manually");
            }
            return generated;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "the master key could not be loaded or created; refusing to "
                            + "run with degraded secret protection", e);
        }
    }

    private static String keyIdOf(byte[] key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key);
            return "key-" + HexFormatHolder.hex(digest).substring(0, 8);
        } catch (Exception e) {
            throw new IllegalStateException("key id derivation failed", e);
        }
    }

    private static final class HexFormatHolder {
        static String hex(byte[] bytes) {
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        }
    }
}
