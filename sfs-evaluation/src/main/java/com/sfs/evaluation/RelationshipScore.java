package com.sfs.evaluation;

import java.util.List;

public record RelationshipScore(int total, int preserved, int withDirection,
                                List<String> missing) {

    public RelationshipScore {
        if (total < 0 || preserved < 0 || withDirection < 0) {
            throw new IllegalArgumentException("counts must not be negative");
        }
        if (preserved > total || withDirection > preserved) {
            throw new IllegalArgumentException(
                    "preserved relationships cannot exceed the total and directed "
                            + "relationships cannot exceed the preserved ones");
        }
        missing = missing == null ? List.of() : List.copyOf(missing);
    }

    public double toDimensionScore() {
        return total == 0 ? 1.0 : (double) preserved / total;
    }
}
