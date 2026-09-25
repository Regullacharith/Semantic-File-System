package com.sfs.adapters.registry;

import com.sfs.adapters.spi.AdapterDescriptor;
import com.sfs.adapters.spi.FileTypeAdapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AdapterRegistry {

    private final Map<String, FileTypeAdapter> adaptersById = new LinkedHashMap<>();

    public void register(FileTypeAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter must not be null");
        AdapterDescriptor descriptor = adapter.descriptor();
        FileTypeAdapter existing = adaptersById.get(descriptor.id());
        if (existing != null) {
            throw new IllegalArgumentException(
                    "an adapter with id " + descriptor.id() + " is already registered");
        }
        adaptersById.put(descriptor.id(), adapter);
    }

    public Optional<FileTypeAdapter> find(String adapterId) {
        if (adapterId == null || adapterId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(adaptersById.get(adapterId));
    }

    public List<FileTypeAdapter> all() {
        return List.copyOf(adaptersById.values());
    }

    public List<AdapterDescriptor> descriptors() {
        return adaptersById.values().stream()
                .map(FileTypeAdapter::descriptor)
                .toList();
    }

    public int count() {
        return adaptersById.size();
    }
}
