package com.sfs.reconstruction;

import com.sfs.reconstruction.encode.EncodedFact;
import com.sfs.reconstruction.encode.EncodedEntity;
import com.sfs.reconstruction.encode.EncodedRelationship;
import com.sfs.reconstruction.encode.SemanticDnaEncoder;
import com.sfs.reconstruction.encode.UnifiedRepresentation;

import java.util.List;
import java.util.Objects;

public final class NaiveEchoModel implements SFSReconstructionModel {

    public static final String MODEL_ID = "naive-echo/0.1";

    private final SemanticDnaEncoder encoder = new SemanticDnaEncoder();

    @Override
    public String modelId() {
        return MODEL_ID;
    }

    @Override
    public ModelOutput reconstruct(ModelInput input) {
        Objects.requireNonNull(input, "input must not be null");
        UnifiedRepresentation representation = encoder.encode(input);
        StringBuilder text = new StringBuilder();
        List<EncodedFact> facts = representation.content().facts();
        for (int i = facts.size() - 1; i >= 0; i--) {
            text.append(facts.get(i).statement()).append('\n');
        }
        for (EncodedEntity entity : representation.content().entities()) {
            text.append(entity.name()).append('\n');
        }
        for (EncodedRelationship relationship : representation.relationships()) {
            text.append(relationship.subject())
                    .append(' ')
                    .append(relationship.type())
                    .append(' ')
                    .append(relationship.object())
                    .append('\n');
        }
        for (String concept : representation.content().concepts()) {
            text.append(concept).append('\n');
        }
        return new ModelOutput(MODEL_ID, text.toString(),
                List.of("no summary, no sections, reversed facts"));
    }
}
