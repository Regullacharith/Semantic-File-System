package com.sfs.ui.config;

import java.util.LinkedHashMap;
import java.util.Map;

public record EffectiveConfiguration(String schema, Map<String, Object> values) {

    public static final String SCHEMA = "sfs-config/0.1";

    public EffectiveConfiguration {
        if (!SCHEMA.equals(schema)) {
            throw new IllegalArgumentException(
                    "the configuration schema id is fixed to " + SCHEMA);
        }
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public static EffectiveConfiguration of(String serverAddress, String serverPort,
                                            String memoryPath, String keysDir,
                                            String secureDir, String profile,
                                            String allowNonLoopback) {
        if (!isLoopback(serverAddress) && !"true".equalsIgnoreCase(allowNonLoopback)) {
            throw new IllegalStateException(
                    "SFS binds to " + serverAddress + " which is not a loopback "
                            + "address; loopback binding is a V1 deployment "
                            + "restriction. Set SFS_ALLOW_NON_LOOPBACK=true only when "
                            + "an external transport-security layer exists.");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("schema", SCHEMA);
        values.put("server.address", serverAddress);
        values.put("server.port", serverPort);
        values.put("sfs.memory.path", memoryPath);
        values.put("sfs.security.keys-dir", keysDir);
        values.put("sfs.security.secure-dir", secureDir);
        values.put("sfs.profile", profile);
        values.put("loopbackEnforced", !isLoopback(serverAddress)
                ? "false (explicitly overridden)" : "true");
        return new EffectiveConfiguration(SCHEMA, values);
    }

    private static boolean isLoopback(String address) {
        return "127.0.0.1".equals(address) || "localhost".equals(address)
                || "::1".equals(address);
    }
}
