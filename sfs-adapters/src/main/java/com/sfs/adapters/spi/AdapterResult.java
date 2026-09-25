package com.sfs.adapters.spi;

import com.sfs.contracts.semantic.SemanticDnaView;

import java.util.List;
import java.util.Objects;

public record AdapterResult(
        String adapterId,
        String adapterVersion,
        String normalizedText,
        TextMetrics metrics,
        List<SemanticDnaView.StructureNodeView> structure) {

    public AdapterResult {
        Objects.requireNonNull(adapterId, "adapterId must not be null");
        Objects.requireNonNull(adapterVersion, "adapterVersion must not be null");
        Objects.requireNonNull(normalizedText, "normalizedText must not be null");
        Objects.requireNonNull(metrics, "metrics must not be null");
        structure = structure == null ? List.of() : List.copyOf(structure);
    }
}
