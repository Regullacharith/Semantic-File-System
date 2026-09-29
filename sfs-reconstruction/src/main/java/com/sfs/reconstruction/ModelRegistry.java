package com.sfs.reconstruction;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ModelRegistry {

    private final Map<String, SFSReconstructionModel> modelsById = new ConcurrentHashMap<>();
    private volatile String defaultModelId;

    public synchronized ModelRegistry register(SFSReconstructionModel model) {
        modelsById.put(model.modelId(), model);
        if (defaultModelId == null) {
            defaultModelId = model.modelId();
        }
        return this;
    }

    public Optional<SFSReconstructionModel> find(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(modelsById.get(modelId));
    }

    public SFSReconstructionModel defaultModel() {
        String id = defaultModelId;
        if (id == null) {
            throw new IllegalStateException("no reconstruction model is registered");
        }
        return modelsById.get(id);
    }

    public List<String> modelIds() {
        return modelsById.keySet().stream().sorted(Comparator.naturalOrder()).toList();
    }

    public int size() {
        return modelsById.size();
    }
}
