package com.sfs.reconstruction.model;

import com.sfs.reconstruction.Decoder;
import com.sfs.reconstruction.ModelInput;
import com.sfs.reconstruction.ModelOutput;
import com.sfs.reconstruction.SFSReconstructionModel;
import com.sfs.reconstruction.decode.TemplatedDecoder;
import com.sfs.reconstruction.encode.SemanticDnaEncoder;
import com.sfs.reconstruction.encode.UnifiedRepresentation;

import java.util.List;
import java.util.Objects;

public final class DeterministicBaselineRenderer implements SFSReconstructionModel {

    public static final String MODEL_ID =
            "sfs-reconstruction/deterministic-baseline/0.1";

    private final SemanticDnaEncoder encoder = new SemanticDnaEncoder();
    private final Decoder decoder;

    public DeterministicBaselineRenderer(Decoder decoder) {
        this.decoder = Objects.requireNonNull(decoder, "decoder must not be null");
    }

    public DeterministicBaselineRenderer() {
        this(new TemplatedDecoder());
    }

    @Override
    public String modelId() {
        return MODEL_ID;
    }

    @Override
    public ModelOutput reconstruct(ModelInput input) {
        Objects.requireNonNull(input, "input must not be null");
        UnifiedRepresentation representation = encoder.encode(input);
        String draft = decoder.decode(representation);
        return new ModelOutput(MODEL_ID, draft, List.of(decoder.decoderId()));
    }
}
