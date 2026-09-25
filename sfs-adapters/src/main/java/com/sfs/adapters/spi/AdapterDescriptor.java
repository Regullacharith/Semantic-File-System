package com.sfs.adapters.spi;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record AdapterDescriptor(
        String id,
        String displayName,
        String version,
        Set<String> supportedExtensions,
        Set<String> supportedContentTypes,
        Set<String> capabilities) {

    public AdapterDescriptor {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(version, "version must not be null");
        if (id.isBlank() || !id.matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException(
                    "adapter id must be lowercase letters, digits and dashes");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }
        Objects.requireNonNull(supportedExtensions, "supportedExtensions must not be null");
        Objects.requireNonNull(supportedContentTypes, "supportedContentTypes must not be null");
        Objects.requireNonNull(capabilities, "capabilities must not be null");
        supportedExtensions = supportedExtensions.stream()
                .map(extension -> extension.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        supportedContentTypes = supportedContentTypes.stream()
                .map(type -> type.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        capabilities = Set.copyOf(capabilities);
        if (supportedExtensions.isEmpty() && supportedContentTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "an adapter must claim at least one extension or content type");
        }
    }

    public boolean supportsExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return supportedExtensions.contains(extension.toLowerCase(Locale.ROOT));
    }

    public boolean supportsContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        return supportedContentTypes.contains(contentType.toLowerCase(Locale.ROOT));
    }
}
