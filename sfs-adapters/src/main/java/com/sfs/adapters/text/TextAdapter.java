package com.sfs.adapters.text;

import com.sfs.adapters.spi.AdapterDescriptor;
import com.sfs.adapters.spi.AdapterRefusedException;
import com.sfs.adapters.spi.AdapterRequest;
import com.sfs.adapters.spi.AdapterResult;
import com.sfs.adapters.spi.FileTypeAdapter;
import com.sfs.adapters.spi.TextMetrics;

import java.util.Objects;
import java.util.Set;

public final class TextAdapter implements FileTypeAdapter {

    public static final String ADAPTER_ID = "sfs-adapter-text";
    public static final String ADAPTER_VERSION = "sfs-text-adapter/0.1";

    private final TextLoader loader = new TextLoader();
    private final TextNormalizer normalizer = new TextNormalizer();
    private final StructuralParser structuralParser = new StructuralParser();

    @Override
    public AdapterDescriptor descriptor() {
        return new AdapterDescriptor(
                ADAPTER_ID,
                "Text Adapter",
                ADAPTER_VERSION,
                Set.of("txt", "text", "md", "markdown", "log"),
                Set.of("text/plain", "text/markdown"),
                Set.of("text-extraction", "normalization", "structure-outline"));
    }

    @Override
    public AdapterResult adapt(AdapterRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String decoded = loader.load(request.content());
        String normalized = normalizer.normalize(decoded);
        TextMetrics metrics = TextMetricsCalculator.of(normalized);
        if (normalized.isBlank() || metrics.wordCount() == 0) {
            throw new AdapterRefusedException("The content carries no words to analyze.");
        }
        return new AdapterResult(
                ADAPTER_ID,
                ADAPTER_VERSION,
                normalized,
                metrics,
                structuralParser.parse(normalized));
    }
}
