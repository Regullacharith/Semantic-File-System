package com.sfs.reconstruction.encode;

import com.sfs.reconstruction.Encoder;
import com.sfs.reconstruction.ModelInput;

import java.util.List;

public final class RelationshipEncoder implements Encoder<ModelInput, List<EncodedRelationship>> {

    @Override
    public List<EncodedRelationship> encode(ModelInput input) {
        return input.dna().relationships().stream()
                .map(relationship -> new EncodedRelationship(
                        relationship.subject(), relationship.type(),
                        relationship.object(),
                        input.plan().requiredRelationships().contains(relationship)))
                .toList();
    }
}
