package com.sfs.core.rules;

public sealed interface Constraint
        permits RequiredFactConstraint, RequiredEntityConstraint, RelationshipConstraint,
        StructureConstraint, ContentConstraint, OrderingConstraint, ValidationConstraint {
}
