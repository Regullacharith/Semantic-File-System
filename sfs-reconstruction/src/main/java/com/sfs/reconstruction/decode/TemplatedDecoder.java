package com.sfs.reconstruction.decode;

import com.sfs.reconstruction.Decoder;
import com.sfs.reconstruction.encode.EncodedEntity;
import com.sfs.reconstruction.encode.EncodedFact;
import com.sfs.reconstruction.encode.EncodedSection;
import com.sfs.reconstruction.encode.UnifiedRepresentation;

public final class TemplatedDecoder implements Decoder {

    public static final String DECODER_ID = "sfs-reconstruction/templated-decoder/0.1";

    @Override
    public String decoderId() {
        return DECODER_ID;
    }

    @Override
    public String decode(UnifiedRepresentation representation) {
        StringBuilder text = new StringBuilder();
        text.append(representation.summary()).append("\n\n");

        for (EncodedSection section : representation.sections()) {
            text.append("#".repeat(section.level()))
                    .append(' ')
                    .append(section.heading())
                    .append("\n\n");
        }

        if (!representation.content().facts().isEmpty()) {
            for (EncodedFact fact : representation.content().facts()) {
                text.append(fact.critical()
                        ? "[" + EncodedFact.CRITICAL_MARKER + "] " + fact.statement()
                        : fact.statement())
                        .append('\n');
            }
            text.append('\n');
        }

        if (!representation.content().entities().isEmpty()) {
            for (EncodedEntity entity : representation.content().entities()) {
                for (int i = 0; i < entity.requiredEmissions(); i++) {
                    text.append(entity.name())
                            .append(" (")
                            .append(entity.type())
                            .append(")\n");
                }
            }
            text.append('\n');
        }

        if (!representation.relationships().isEmpty()) {
            for (var relationship : representation.relationships()) {
                text.append(relationship.subject())
                        .append(' ')
                        .append(relationship.type())
                        .append(' ')
                        .append(relationship.object())
                        .append(".\n");
            }
            text.append('\n');
        }

        if (!representation.content().concepts().isEmpty()) {
            text.append(String.join(", ", representation.content().concepts()))
                    .append("\n\n");
        }

        if (!representation.content().topics().isEmpty()) {
            text.append(String.join(", ", representation.content().topics()))
                    .append("\n");
        }

        return text.toString();
    }
}
