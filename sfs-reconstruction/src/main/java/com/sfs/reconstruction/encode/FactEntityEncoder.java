package com.sfs.reconstruction.encode;

import com.sfs.reconstruction.Encoder;
import com.sfs.reconstruction.ModelInput;

import java.util.List;

public final class FactEntityEncoder implements Encoder<ModelInput, EncodedContent> {

    @Override
    public EncodedContent encode(ModelInput input) {
        var dna = input.dna();
        var plan = input.plan();
        List<EncodedFact> facts = dna.facts().stream()
                .map(fact -> new EncodedFact(
                        fact.statement(), fact.critical(), fact.confidence(),
                        plan.requiredFacts().stream()
                                .anyMatch(required -> required.statement()
                                        .equals(fact.statement()))))
                .toList();
        List<EncodedEntity> entities = dna.entities().stream()
                .map(entity -> new EncodedEntity(
                        entity.name(), entity.type(), entity.mentions(),
                        plan.requiredEntities().stream()
                                .filter(required -> required.name().equals(entity.name()))
                                .mapToInt(required -> required.minMentions())
                                .max()
                                .orElse(0)))
                .toList();
        List<String> concepts = dna.concepts().stream()
                .map(concept -> concept.name())
                .toList();
        List<String> topics = dna.topics().stream()
                .map(topic -> topic.name())
                .toList();
        return new EncodedContent(facts, entities, concepts, topics);
    }
}
