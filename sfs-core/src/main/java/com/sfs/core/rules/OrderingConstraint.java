package com.sfs.core.rules;

import java.util.List;
import java.util.Objects;

public record OrderingConstraint(boolean preserveSectionOrder,
                                 List<String> sectionOrder) implements Constraint {

    public OrderingConstraint {
        Objects.requireNonNull(sectionOrder, "sectionOrder must not be null");
        if (preserveSectionOrder && sectionOrder.isEmpty()) {
            throw new IllegalArgumentException(
                    "an ordering rule that preserves section order needs the order");
        }
        sectionOrder = List.copyOf(sectionOrder);
    }
}
