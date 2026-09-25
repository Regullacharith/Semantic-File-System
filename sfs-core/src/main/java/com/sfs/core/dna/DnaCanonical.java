package com.sfs.core.dna;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DnaCanonical {

    private DnaCanonical() {
    }

    public static String serialize(SemanticDna dna) {
        return Json.write(toTree(dna));
    }

    public static SemanticDna deserialize(String canonicalJson) {
        Object tree = Json.parse(canonicalJson);
        String schemaVersion = Json.string(field(tree, "schemaVersion"));
        if (DnaSchemaValidator.CURRENT_SCHEMA_VERSION.equals(schemaVersion)) {
            return fromTree(tree);
        }
        throw new IllegalArgumentException(
                "unsupported DNA schema version " + schemaVersion
                        + "; use the migrator for older versions");
    }

    public static String integrityHash(SemanticDna dna) {
        return com.sfs.core.identity.Digests.sha256Hex(serialize(dna));
    }

    static Map<String, Object> toTree(SemanticDna dna) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", dna.schemaVersion());
        root.put("objectId", dna.objectId());
        root.put("dnaVersion", (double) dna.dnaVersion());
        root.put("engineVersion", dna.identity().engineVersion());
        root.put("generatedAt", dna.identity().generatedAt().toString());
        root.put("summary", dna.summary());
        root.put("concepts", dna.concepts().stream().map(Concept::name).toList());
        root.put("topics", dna.topics().stream().map(Topic::name).toList());
        root.put("entities", dna.entities().stream()
                .map(entity -> orderedMap(
                        "name", entity.name(),
                        "type", entity.type(),
                        "mentions", (double) entity.mentions()))
                .toList());
        root.put("facts", dna.facts().stream()
                .map(fact -> orderedMap(
                        "statement", fact.statement(),
                        "critical", fact.critical(),
                        "confidence", fact.confidence()))
                .toList());
        root.put("relationships", dna.relationships().stream()
                .map(relationship -> orderedMap(
                        "subject", relationship.subject(),
                        "type", relationship.type(),
                        "object", relationship.object()))
                .toList());
        root.put("structure", dna.structure().stream()
                .map(node -> orderedMap(
                        "heading", node.heading(),
                        "level", (double) node.level(),
                        "order", (double) node.order()))
                .toList());
        Map<String, Object> embedding = new LinkedHashMap<>();
        embedding.put("algorithm", dna.embedding().algorithm());
        embedding.put("dimensions", (double) dna.embedding().dimensions());
        embedding.put("vector", dna.embedding().vector());
        root.put("embedding", embedding);
        Map<String, Object> behaviour = new LinkedHashMap<>();
        behaviour.put("documentType", dna.behaviour().documentType());
        behaviour.put("structured", dna.behaviour().structured());
        behaviour.put("averageSentenceWords", dna.behaviour().averageSentenceWords());
        behaviour.put("guidance", dna.behaviour().guidance());
        root.put("behaviour", behaviour);
        root.put("reconstructionRules", dna.reconstructionRules().stream()
                .map(rule -> orderedMap(
                        "ruleId", rule.ruleId(),
                        "ruleType", rule.ruleType(),
                        "version", rule.version()))
                .toList());
        Map<String, Object> fidelity = new LinkedHashMap<>();
        fidelity.put("extractionConfidence", dna.fidelity().extractionConfidence());
        fidelity.put("structuralCompleteness", dna.fidelity().structuralCompleteness());
        fidelity.put("analyzerVersion", dna.fidelity().analyzerVersion());
        root.put("fidelity", fidelity);
        Map<String, Object> security = new LinkedHashMap<>();
        security.put("handlingPolicy", dna.security().handlingPolicy());
        security.put("protectedReferences", dna.security().protectedReferences().stream()
                .map(reference -> orderedMap(
                        "referenceId", reference.referenceId(),
                        "sensitiveType", reference.sensitiveType(),
                        "semanticRole", reference.semanticRole(),
                        "location", reference.location()))
                .toList());
        root.put("security", security);
        return root;
    }

    static SemanticDna fromTree(Object tree) {
        Map<String, Object> root = Json.object(tree);
        DnaIdentity identity = new DnaIdentity(
                Json.string(field(root, "objectId")),
                Json.string(field(root, "schemaVersion")),
                (int) Json.number(field(root, "dnaVersion")),
                Json.string(field(root, "engineVersion")),
                java.time.Instant.parse(Json.string(field(root, "generatedAt"))));
        Map<String, Object> embedding = Json.object(field(root, "embedding"));
        EmbeddingRef embeddingRef = new EmbeddingRef(
                Json.string(field(embedding, "algorithm")),
                (int) Json.number(field(embedding, "dimensions")),
                Json.array(field(embedding, "vector")).stream()
                        .map(Json::number)
                        .toList());
        Map<String, Object> behaviour = Json.object(field(root, "behaviour"));
        BehaviourProfile behaviourProfile = new BehaviourProfile(
                Json.string(field(behaviour, "documentType")),
                Json.bool(field(behaviour, "structured")),
                Json.number(field(behaviour, "averageSentenceWords")),
                Json.array(field(behaviour, "guidance")).stream()
                        .map(Json::string)
                        .toList());
        Map<String, Object> fidelity = Json.object(field(root, "fidelity"));
        FidelityProfile fidelityProfile = new FidelityProfile(
                Json.number(field(fidelity, "extractionConfidence")),
                Json.number(field(fidelity, "structuralCompleteness")),
                Json.string(field(fidelity, "analyzerVersion")));
        Map<String, Object> security = Json.object(field(root, "security"));
        SecurityProfile securityProfile = new SecurityProfile(
                Json.string(field(security, "handlingPolicy")),
                Json.array(field(security, "protectedReferences")).stream()
                        .map(reference -> {
                            Map<String, Object> ref = Json.object(reference);
                            return new ProtectedReference(
                                    Json.string(field(ref, "referenceId")),
                                    Json.string(field(ref, "sensitiveType")),
                                    Json.string(field(ref, "semanticRole")),
                                    Json.string(field(ref, "location")));
                        })
                        .toList());
        return new SemanticDna(
                identity,
                Json.string(field(root, "summary")),
                Json.array(field(root, "concepts")).stream()
                        .map(Json::string).map(Concept::new).toList(),
                Json.array(field(root, "topics")).stream()
                        .map(Json::string).map(Topic::new).toList(),
                Json.array(field(root, "entities")).stream()
                        .map(item -> {
                            Map<String, Object> entity = Json.object(item);
                            return new Entity(
                                    Json.string(field(entity, "name")),
                                    Json.string(field(entity, "type")),
                                    (int) Json.number(field(entity, "mentions")));
                        })
                        .toList(),
                Json.array(field(root, "facts")).stream()
                        .map(item -> {
                            Map<String, Object> fact = Json.object(item);
                            return new Fact(
                                    Json.string(field(fact, "statement")),
                                    Json.bool(field(fact, "critical")),
                                    Json.number(field(fact, "confidence")));
                        })
                        .toList(),
                Json.array(field(root, "relationships")).stream()
                        .map(item -> {
                            Map<String, Object> rel = Json.object(item);
                            return new Relationship(
                                    Json.string(field(rel, "subject")),
                                    Json.string(field(rel, "type")),
                                    Json.string(field(rel, "object")));
                        })
                        .toList(),
                Json.array(field(root, "structure")).stream()
                        .map(item -> {
                            Map<String, Object> node = Json.object(item);
                            return new StructureNode(
                                    Json.string(field(node, "heading")),
                                    (int) Json.number(field(node, "level")),
                                    (int) Json.number(field(node, "order")));
                        })
                        .toList(),
                embeddingRef,
                behaviourProfile,
                Json.array(field(root, "reconstructionRules")).stream()
                        .map(item -> {
                            Map<String, Object> rule = Json.object(item);
                            return new ReconstructionRuleRef(
                                    Json.string(field(rule, "ruleId")),
                                    Json.string(field(rule, "ruleType")),
                                    Json.string(field(rule, "version")));
                        })
                        .toList(),
                fidelityProfile,
                securityProfile);
    }

    private static Object field(Object tree, String key) {
        Map<String, Object> map = Json.object(tree);
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("missing required DNA field '" + key + "'");
        }
        return value;
    }

    private static Map<String, Object> orderedMap(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
