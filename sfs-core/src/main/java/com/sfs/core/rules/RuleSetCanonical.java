package com.sfs.core.rules;

import com.sfs.core.dna.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RuleSetCanonical {

    public static final String RULES_SCHEMA_VERSION = "sfs-rules/0.2";

    private RuleSetCanonical() {
    }

    public static String serialize(RuleSet set) {
        List<Object> rules = new ArrayList<>();
        for (Rule rule : set.rules()) {
            Map<String, Object> ruleJson = new LinkedHashMap<>();
            ruleJson.put("ruleId", rule.ruleId());
            ruleJson.put("type", rule.type().name());
            ruleJson.put("priority", rule.priority().name());
            ruleJson.put("description", rule.description());
            List<Object> constraints = new ArrayList<>();
            for (Constraint constraint : rule.constraints()) {
                constraints.add(constraintTree(constraint));
            }
            ruleJson.put("constraints", constraints);
            rules.add(ruleJson);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("rulesSchemaVersion", RULES_SCHEMA_VERSION);
        root.put("objectId", set.objectId());
        root.put("dnaVersion", (double) set.dnaVersion());
        root.put("dnaSha256", set.dnaSha256());
        root.put("rulesVersion", set.rulesVersion());
        root.put("rules", rules);
        return Json.write(root);
    }

    public static RuleSet parse(String canonicalJson) {
        Object tree = Json.parse(canonicalJson);
        Map<String, Object> root = Json.object(tree);
        String schemaVersion = Json.string(field(root, "rulesSchemaVersion"));
        if (!RULES_SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException(
                    "unsupported rules schema version " + schemaVersion);
        }
        List<Rule> rules = new ArrayList<>();
        for (Object item : Json.array(field(root, "rules"))) {
            Map<String, Object> ruleJson = Json.object(item);
            List<Constraint> constraints = new ArrayList<>();
            for (Object constraintItem : Json.array(field(ruleJson, "constraints"))) {
                constraints.add(constraintOf(Json.object(constraintItem)));
            }
            rules.add(new Rule(
                    Json.string(field(ruleJson, "ruleId")),
                    RuleType.valueOf(Json.string(field(ruleJson, "type"))),
                    RulePriority.valueOf(Json.string(field(ruleJson, "priority"))),
                    Json.string(field(ruleJson, "description")),
                    constraints));
        }
        return new RuleSet(
                Json.string(field(root, "objectId")),
                (int) Json.number(field(root, "dnaVersion")),
                Json.string(field(root, "dnaSha256")),
                Json.string(field(root, "rulesVersion")),
                rules);
    }

    public static String integrityHash(RuleSet set) {
        return com.sfs.core.identity.Digests.sha256Hex(serialize(set));
    }

    private static Map<String, Object> constraintTree(Constraint constraint) {
        Map<String, Object> json = new LinkedHashMap<>();
        if (constraint instanceof RequiredFactConstraint requiredFact) {
            json.put("kind", "required-fact");
            json.put("statement", requiredFact.statement());
            json.put("failIfMissing", requiredFact.failIfMissing());
        } else if (constraint instanceof RequiredEntityConstraint requiredEntity) {
            json.put("kind", "required-entity");
            json.put("name", requiredEntity.name());
            json.put("minMentions", (double) requiredEntity.minMentions());
        } else if (constraint instanceof RelationshipConstraint relationship) {
            json.put("kind", "relationship");
            json.put("subject", relationship.subject());
            json.put("type", relationship.type());
            json.put("object", relationship.object());
            json.put("failIfMissing", relationship.failIfMissing());
        } else if (constraint instanceof StructureConstraint structure) {
            json.put("kind", "structure");
            json.put("minHeadings", (double) structure.minHeadings());
            json.put("maxHeadings", (double) structure.maxHeadings());
            json.put("requiredHeadings", structure.requiredHeadings());
        } else if (constraint instanceof ContentConstraint content) {
            json.put("kind", "content");
            json.put("preserveSummaryVerbatim", content.preserveSummaryVerbatim());
            json.put("minConcepts", (double) content.minConcepts());
            json.put("minTopics", (double) content.minTopics());
        } else if (constraint instanceof OrderingConstraint ordering) {
            json.put("kind", "ordering");
            json.put("preserveSectionOrder", ordering.preserveSectionOrder());
            json.put("sectionOrder", ordering.sectionOrder());
        } else if (constraint instanceof ValidationConstraint validation) {
            json.put("kind", "validation");
            json.put("forbidInventedFacts", validation.forbidInventedFacts());
            json.put("requireAllCriticalFacts", validation.requireAllCriticalFacts());
            json.put("minFactConfidence", validation.minFactConfidence());
        } else {
            throw new IllegalArgumentException(
                    "unknown constraint type " + constraint.getClass().getName());
        }
        return json;
    }

    private static Constraint constraintOf(Map<String, Object> json) {
        String kind = Json.string(field(json, "kind"));
        return switch (kind) {
            case "required-fact" -> new RequiredFactConstraint(
                    Json.string(field(json, "statement")),
                    Json.bool(field(json, "failIfMissing")));
            case "required-entity" -> new RequiredEntityConstraint(
                    Json.string(field(json, "name")),
                    (int) Json.number(field(json, "minMentions")));
            case "relationship" -> new RelationshipConstraint(
                    Json.string(field(json, "subject")),
                    Json.string(field(json, "type")),
                    Json.string(field(json, "object")),
                    Json.bool(field(json, "failIfMissing")));
            case "structure" -> new StructureConstraint(
                    (int) Json.number(field(json, "minHeadings")),
                    (int) Json.number(field(json, "maxHeadings")),
                    Json.array(field(json, "requiredHeadings")).stream()
                            .map(Json::string).toList());
            case "content" -> new ContentConstraint(
                    Json.bool(field(json, "preserveSummaryVerbatim")),
                    (int) Json.number(field(json, "minConcepts")),
                    (int) Json.number(field(json, "minTopics")));
            case "ordering" -> new OrderingConstraint(
                    Json.bool(field(json, "preserveSectionOrder")),
                    Json.array(field(json, "sectionOrder")).stream()
                            .map(Json::string).toList());
            case "validation" -> new ValidationConstraint(
                    Json.bool(field(json, "forbidInventedFacts")),
                    Json.bool(field(json, "requireAllCriticalFacts")),
                    Json.number(field(json, "minFactConfidence")));
            default -> throw new IllegalArgumentException(
                    "unknown constraint kind '" + kind + "'");
        };
    }

    private static Object field(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException(
                    "missing required rules field '" + key + "'");
        }
        return value;
    }
}
