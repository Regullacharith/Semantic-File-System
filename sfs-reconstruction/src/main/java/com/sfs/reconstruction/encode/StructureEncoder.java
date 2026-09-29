package com.sfs.reconstruction.encode;

import com.sfs.reconstruction.Encoder;
import com.sfs.reconstruction.ModelInput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StructureEncoder implements Encoder<ModelInput, List<EncodedSection>> {

    @Override
    public List<EncodedSection> encode(ModelInput input) {
        List<EncodedSection> sections = new ArrayList<>();
        List<String> requiredOrder = input.plan().sectionOrder();
        List<com.sfs.core.dna.StructureNode> nodes =
                new ArrayList<>(input.dna().structure());
        nodes.sort(Comparator.comparingInt(com.sfs.core.dna.StructureNode::order));
        for (com.sfs.core.dna.StructureNode node : nodes) {
            sections.add(new EncodedSection(
                    node.heading(), node.level(), node.order(),
                    requiredOrder.contains(node.heading())));
        }
        return List.copyOf(sections);
    }
}
