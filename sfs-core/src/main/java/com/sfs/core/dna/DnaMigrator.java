package com.sfs.core.dna;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class DnaMigrator {

    public static final String LEGACY_SCHEMA_VERSION = "sfs-dna/0.1";
    public static final String MIGRATION_ENGINE_VERSION = "sfs-dna-migrator/0.2";

    private final DnaSchemaValidator validator = new DnaSchemaValidator();

    public SemanticDna migrate(String legacyCanonicalJson) {
        Object tree = Json.parse(legacyCanonicalJson);
        String version = Json.string(field(tree, "schemaVersion"));
        if (!LEGACY_SCHEMA_VERSION.equals(version)) {
            throw new IllegalArgumentException(
                    "the migrator upgrades " + LEGACY_SCHEMA_VERSION
                            + " only; received " + version);
        }
        Map<String, Object> root = Json.object(tree);
        SemanticDna migrated = new SemanticDna(
                new DnaIdentity(
                        Json.string(field(root, "objectId")),
                        DnaSchemaValidator.CURRENT_SCHEMA_VERSION,
                        (int) Json.number(field(root, "dnaVersion")),
                        MIGRATION_ENGINE_VERSION,
                        Instant.EPOCH),
                Json.string(field(root, "summary")),
                strings(root, "concepts").stream().map(Concept::new).toList(),
                strings(root, "topics").stream().map(Topic::new).toList(),
                objects(root, "entities").stream()
                        .map(entity -> new Entity(
                                Json.string(field(entity, "name")),
                                Json.string(field(entity, "type")),
                                (int) Json.number(field(entity, "mentions"))))
                        .toList(),
                objects(root, "facts").stream()
                        .map(fact -> new Fact(
                                Json.string(field(fact, "statement")),
                                Json.bool(field(fact, "critical")),
                                Json.number(field(fact, "confidence"))))
                        .toList(),
                objects(root, "relationships").stream()
                        .map(relationship -> new Relationship(
                                Json.string(field(relationship, "subject")),
                                Json.string(field(relationship, "type")),
                                Json.string(field(relationship, "object"))))
                        .toList(),
                objects(root, "structure").stream()
                        .map(node -> new StructureNode(
                                Json.string(field(node, "heading")),
                                (int) Json.number(field(node, "level")),
                                (int) Json.number(field(node, "order"))))
                        .toList(),
                EmbeddingRef.absent(),
                new BehaviourProfile("migrated-document", false, 0,
                        List.of("review migrated representation")),
                SemanticDnaBuilder.SEED_RULES,
                new FidelityProfile(
                        Json.number(field(root, "extractionConfidence")),
                        Json.number(field(root, "structuralCompleteness")),
                        Json.string(field(root, "analyzerVersion"))),
                SecurityProfile.unrestricted());
        List<String> issues = validator.validate(migrated);
        if (!issues.isEmpty()) {
            throw new IllegalArgumentException(
                    "migrated DNA is not schema-valid: " + String.join("; ", issues));
        }
        return migrated;
    }

    private static Object field(Object tree, String key) {
        Map<String, Object> map = Json.object(tree);
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("missing required field '" + key + "'");
        }
        return value;
    }

    private static List<String> strings(Map<String, Object> root, String key) {
        return Json.array(field(root, key)).stream().map(Json::string).toList();
    }

    private static List<Map<String, Object>> objects(Map<String, Object> root, String key) {
        return Json.array(field(root, key)).stream()
                .map(Json::object)
                .toList();
    }
}
