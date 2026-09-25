package com.sfs.adapters.resolve;

import com.sfs.adapters.registry.AdapterRegistry;
import com.sfs.adapters.spi.FileTypeAdapter;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class AdapterResolver {

    private final AdapterRegistry registry;
    private final AtomicLong resolutions = new AtomicLong();
    private final AtomicLong refusals = new AtomicLong();

    public AdapterResolver(AdapterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    public Optional<FileTypeAdapter> find(String fileName, String contentType) {
        if (fileName == null || fileName.isBlank()) {
            refusals.incrementAndGet();
            return Optional.empty();
        }
        String extension = extensionOf(fileName);
        String normalizedType = contentType == null || contentType.isBlank()
                ? null
                : contentType.toLowerCase(Locale.ROOT);
        for (FileTypeAdapter adapter : registry.all()) {
            boolean extensionMatch = adapter.descriptor().supportsExtension(extension);
            boolean typeMatch = normalizedType != null
                    && adapter.descriptor().supportsContentType(normalizedType);
            if (extensionMatch || typeMatch) {
                resolutions.incrementAndGet();
                return Optional.of(adapter);
            }
        }
        refusals.incrementAndGet();
        return Optional.empty();
    }

    public FileTypeAdapter resolve(String fileName, String contentType) {
        return find(fileName, contentType)
                .orElseThrow(() -> new UnsupportedFileTypeException(fileName, contentType));
    }

    public long resolutions() {
        return resolutions.get();
    }

    public long refusals() {
        return refusals.get();
    }

    private static String extensionOf(String fileName) {
        String name = fileName.strip();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
