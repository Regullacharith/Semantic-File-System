package com.sfs.reconstruction.encode;

import com.sfs.reconstruction.Encoder;
import com.sfs.reconstruction.ModelInput;

import java.util.List;
import java.util.Objects;

public final class SemanticDnaEncoder implements Encoder<ModelInput, UnifiedRepresentation> {

    private final StructureEncoder structureEncoder = new StructureEncoder();
    private final FactEntityEncoder factEntityEncoder = new FactEntityEncoder();
    private final RelationshipEncoder relationshipEncoder = new RelationshipEncoder();

    @Override
    public UnifiedRepresentation encode(ModelInput input) {
        Objects.requireNonNull(input, "input must not be null");
        List<EncodedSection> sections = structureEncoder.encode(input);
        EncodedContent content = factEntityEncoder.encode(input);
        List<EncodedRelationship> relationships = relationshipEncoder.encode(input);
        int protectedCount = input.dna().security().protectedReferences().size();
        return new UnifiedRepresentation(
                input.dna().summary(), sections, content, relationships, protectedCount);
    }
}
