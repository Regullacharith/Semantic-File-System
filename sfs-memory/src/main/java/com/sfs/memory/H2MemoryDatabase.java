package com.sfs.memory;

import com.sfs.core.dna.DnaCanonical;
import com.sfs.core.dna.EmbeddingRef;
import com.sfs.core.dna.StoredDna;
import com.sfs.core.dna.SemanticDna;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class H2MemoryDatabase implements AutoCloseable {

    private static final String[] SCHEMA = {
            "CREATE TABLE IF NOT EXISTS schema_version (version INT PRIMARY KEY, applied_at TIMESTAMP NOT NULL)",
            "CREATE TABLE IF NOT EXISTS semantic_object (object_id VARCHAR(64) PRIMARY KEY, state VARCHAR(32) NOT NULL, deleted_from VARCHAR(32), certified_dna_version VARCHAR(64), state_changed_at TIMESTAMP NOT NULL)",
            "CREATE TABLE IF NOT EXISTS metadata (object_id VARCHAR(64) PRIMARY KEY, file_name VARCHAR(255) NOT NULL, content_type VARCHAR(128) NOT NULL, size_bytes BIGINT NOT NULL, sha256 VARCHAR(64) NOT NULL, storage_address VARCHAR(512), registered_at TIMESTAMP NOT NULL, last_modified_at TIMESTAMP NOT NULL)",
            "CREATE TABLE IF NOT EXISTS file_version (object_id VARCHAR(64) NOT NULL, number INT NOT NULL, content_sha256 VARCHAR(64) NOT NULL, size_bytes BIGINT NOT NULL, captured_at TIMESTAMP NOT NULL, PRIMARY KEY (object_id, number))",
            "CREATE TABLE IF NOT EXISTS raw_content (object_id VARCHAR(64) PRIMARY KEY, content BLOB NOT NULL)",
            "CREATE TABLE IF NOT EXISTS semantic_dna (object_id VARCHAR(64) NOT NULL, dna_version INT NOT NULL, schema_version VARCHAR(32) NOT NULL, engine_version VARCHAR(64) NOT NULL, generated_at TIMESTAMP NOT NULL, canonical_json CLOB NOT NULL, canonical_sha256 VARCHAR(64) NOT NULL, previous_sha256 VARCHAR(64), PRIMARY KEY (object_id, dna_version))",
            "CREATE TABLE IF NOT EXISTS concept (object_id VARCHAR(64) NOT NULL, dna_version INT NOT NULL, idx INT NOT NULL, name VARCHAR(255) NOT NULL, PRIMARY KEY (object_id, dna_version, idx))",
            "CREATE TABLE IF NOT EXISTS topic (object_id VARCHAR(64) NOT NULL, dna_version INT NOT NULL, idx INT NOT NULL, name VARCHAR(255) NOT NULL, PRIMARY KEY (object_id, dna_version, idx))",
            "CREATE TABLE IF NOT EXISTS entity (object_id VARCHAR(64) NOT NULL, dna_version INT NOT NULL, idx INT NOT NULL, name VARCHAR(255) NOT NULL, type VARCHAR(64) NOT NULL, mentions INT NOT NULL, PRIMARY KEY (object_id, dna_version, idx))",
            "CREATE TABLE IF NOT EXISTS fact (object_id VARCHAR(64) NOT NULL, dna_version INT NOT NULL, idx INT NOT NULL, statement CLOB NOT NULL, critical BOOLEAN NOT NULL, confidence DOUBLE NOT NULL, PRIMARY KEY (object_id, dna_version, idx))",
            "CREATE TABLE IF NOT EXISTS relationship (object_id VARCHAR(64) NOT NULL, dna_version INT NOT NULL, idx INT NOT NULL, subject VARCHAR(255) NOT NULL, type VARCHAR(64) NOT NULL, object VARCHAR(512) NOT NULL, PRIMARY KEY (object_id, dna_version, idx))",
            "CREATE TABLE IF NOT EXISTS structure_node (object_id VARCHAR(64) NOT NULL, dna_version INT NOT NULL, idx INT NOT NULL, heading VARCHAR(255) NOT NULL, level INT NOT NULL, order_idx INT NOT NULL, PRIMARY KEY (object_id, dna_version, idx))",
            "CREATE TABLE IF NOT EXISTS reconstruction_rule (object_id VARCHAR(64) NOT NULL, dna_version INT NOT NULL, idx INT NOT NULL, rule_id VARCHAR(128) NOT NULL, rule_type VARCHAR(32) NOT NULL, version VARCHAR(64) NOT NULL, PRIMARY KEY (object_id, dna_version, idx))",
            "CREATE TABLE IF NOT EXISTS security_reference (object_id VARCHAR(64) NOT NULL, dna_version INT NOT NULL, idx INT NOT NULL, reference_id VARCHAR(128) NOT NULL, sensitive_type VARCHAR(64) NOT NULL, semantic_role VARCHAR(128) NOT NULL, location VARCHAR(128) NOT NULL, PRIMARY KEY (object_id, dna_version, idx))",
            "CREATE TABLE IF NOT EXISTS embedding (object_id VARCHAR(64) PRIMARY KEY, dna_version INT NOT NULL, algorithm VARCHAR(64) NOT NULL, dimensions INT NOT NULL, vector_json CLOB NOT NULL)",
            "CREATE TABLE IF NOT EXISTS ruleset (object_id VARCHAR(64) PRIMARY KEY, dna_version INT NOT NULL, rules_json CLOB NOT NULL, rules_sha256 VARCHAR(64) NOT NULL, bound_at TIMESTAMP NOT NULL)",
            "CREATE TABLE IF NOT EXISTS lifecycle_event (event_id VARCHAR(64) PRIMARY KEY, object_id VARCHAR(64) NOT NULL, type VARCHAR(64) NOT NULL, from_state VARCHAR(32), to_state VARCHAR(32) NOT NULL, principal_id VARCHAR(64), refused BOOLEAN NOT NULL, reason CLOB, at TIMESTAMP NOT NULL, duration_ms BIGINT)"
    };

    private final Connection connection;

    public H2MemoryDatabase(String jdbcUrl) {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl must not be null");
        try {
            this.connection = DriverManager.getConnection(jdbcUrl, "sfs", "");
            this.connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new IllegalStateException("could not open the Memory DB: " + e.getMessage(), e);
        }
    }

    public void initialize() {
        try (Statement statement = connection.createStatement()) {
            for (String ddl : SCHEMA) {
                statement.execute(ddl);
            }
            ResultSet existing = statement.executeQuery("SELECT COUNT(*) FROM schema_version");
            existing.next();
            if (existing.getInt(1) == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO schema_version (version, applied_at) VALUES (1, CURRENT_TIMESTAMP)")) {
                    insert.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("could not initialize the Memory DB schema", e);
        }
    }

    public StoredDna saveSemanticDna(SemanticDna dna, Instant at) {
        Objects.requireNonNull(dna, "dna must not be null");
        Objects.requireNonNull(at, "at must not be null");
        try {
            int current = maxDnaVersion(dna.objectId());
            if (current >= dna.dnaVersion()) {
                throw new IllegalArgumentException(
                        "DNA version must increase; the stored version is " + current);
            }
            String canonical = DnaCanonical.serialize(dna);
            String hash = com.sfs.core.identity.Digests.sha256Hex(canonical);
            String previous = current == 0 ? null : canonicalShaOf(dna.objectId(), current);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO semantic_dna (object_id, dna_version, schema_version, engine_version, generated_at, canonical_json, canonical_sha256, previous_sha256) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, dna.objectId());
                insert.setInt(2, dna.dnaVersion());
                insert.setString(3, dna.schemaVersion());
                insert.setString(4, dna.identity().engineVersion());
                insert.setObject(5, java.sql.Timestamp.from(dna.identity().generatedAt()));
                insert.setString(6, canonical);
                insert.setString(7, hash);
                insert.setString(8, previous);
                insert.executeUpdate();
            }
            writeGraphRows(dna);
            upsertEmbedding(dna);
            connection.commit();
            return new StoredDna(dna, hash, previous, at);
        } catch (SQLException e) {
            rollbackQuietly();
            throw new IllegalStateException("saving the Semantic Record failed", e);
        } catch (RuntimeException e) {
            rollbackQuietly();
            throw e;
        }
    }

    private void writeGraphRows(SemanticDna dna) throws SQLException {
        int version = dna.dnaVersion();
        String objectId = dna.objectId();
        deleteGraphRows(objectId, version);
        executeBatch(objectId, version, "INSERT INTO concept (object_id, dna_version, idx, name) VALUES (?, ?, ?, ?)",
                dna.concepts().stream().map(concept -> new Object[]{concept.name()}).toList());
        executeBatch(objectId, version, "INSERT INTO topic (object_id, dna_version, idx, name) VALUES (?, ?, ?, ?)",
                dna.topics().stream().map(topic -> new Object[]{topic.name()}).toList());
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO entity (object_id, dna_version, idx, name, type, mentions) VALUES (?, ?, ?, ?, ?, ?)")) {
            List<com.sfs.core.dna.Entity> entities = dna.entities();
            for (int i = 0; i < entities.size(); i++) {
                statement.setString(1, objectId);
                statement.setInt(2, version);
                statement.setInt(3, i);
                statement.setString(4, entities.get(i).name());
                statement.setString(5, entities.get(i).type());
                statement.setInt(6, entities.get(i).mentions());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO fact (object_id, dna_version, idx, statement, critical, confidence) VALUES (?, ?, ?, ?, ?, ?)")) {
            List<com.sfs.core.dna.Fact> facts = dna.facts();
            for (int i = 0; i < facts.size(); i++) {
                statement.setString(1, objectId);
                statement.setInt(2, version);
                statement.setInt(3, i);
                statement.setString(4, facts.get(i).statement());
                statement.setBoolean(5, facts.get(i).critical());
                statement.setDouble(6, facts.get(i).confidence());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO relationship (object_id, dna_version, idx, subject, type, object) VALUES (?, ?, ?, ?, ?, ?)")) {
            List<com.sfs.core.dna.Relationship> relationships = dna.relationships();
            for (int i = 0; i < relationships.size(); i++) {
                statement.setString(1, objectId);
                statement.setInt(2, version);
                statement.setInt(3, i);
                statement.setString(4, relationships.get(i).subject());
                statement.setString(5, relationships.get(i).type());
                statement.setString(6, relationships.get(i).object());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO structure_node (object_id, dna_version, idx, heading, level, order_idx) VALUES (?, ?, ?, ?, ?, ?)")) {
            List<com.sfs.core.dna.StructureNode> nodes = dna.structure();
            for (int i = 0; i < nodes.size(); i++) {
                statement.setString(1, objectId);
                statement.setInt(2, version);
                statement.setInt(3, i);
                statement.setString(4, nodes.get(i).heading());
                statement.setInt(5, nodes.get(i).level());
                statement.setInt(6, nodes.get(i).order());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO reconstruction_rule (object_id, dna_version, idx, rule_id, rule_type, version) VALUES (?, ?, ?, ?, ?, ?)")) {
            List<com.sfs.core.dna.ReconstructionRuleRef> rules = dna.reconstructionRules();
            for (int i = 0; i < rules.size(); i++) {
                statement.setString(1, objectId);
                statement.setInt(2, version);
                statement.setInt(3, i);
                statement.setString(4, rules.get(i).ruleId());
                statement.setString(5, rules.get(i).ruleType());
                statement.setString(6, rules.get(i).version());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO security_reference (object_id, dna_version, idx, reference_id, sensitive_type, semantic_role, location) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            List<com.sfs.core.dna.ProtectedReference> references =
                    dna.security().protectedReferences();
            for (int i = 0; i < references.size(); i++) {
                statement.setString(1, objectId);
                statement.setInt(2, version);
                statement.setInt(3, i);
                statement.setString(4, references.get(i).referenceId());
                statement.setString(5, references.get(i).sensitiveType());
                statement.setString(6, references.get(i).semanticRole());
                statement.setString(7, references.get(i).location());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void deleteGraphRows(String objectId, int version) throws SQLException {
        String[] tables = {"concept", "topic", "entity", "fact", "relationship",
                "structure_node", "reconstruction_rule", "security_reference"};
        for (String table : tables) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM " + table + " WHERE object_id = ? AND dna_version = ?")) {
                statement.setString(1, objectId);
                statement.setInt(2, version);
                statement.executeUpdate();
            }
        }
    }

    private void executeBatch(String objectId, int version, String sql,
                              List<Object[]> rows) throws SQLException {
        if (rows.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < rows.size(); i++) {
                statement.setString(1, objectId);
                statement.setInt(2, version);
                statement.setInt(3, i);
                Object[] row = rows.get(i);
                for (int j = 0; j < row.length; j++) {
                    statement.setObject(j + 4, row[j]);
                }
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void upsertEmbedding(SemanticDna dna) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM embedding WHERE object_id = ?")) {
            delete.setString(1, dna.objectId());
            delete.executeUpdate();
        }
        if (dna.embedding().dimensions() == 0) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO embedding (object_id, dna_version, algorithm, dimensions, vector_json) VALUES (?, ?, ?, ?, ?)")) {
            insert.setString(1, dna.objectId());
            insert.setInt(2, dna.dnaVersion());
            insert.setString(3, dna.embedding().algorithm());
            insert.setInt(4, dna.embedding().dimensions());
            insert.setString(5, com.sfs.core.dna.Json.write(dna.embedding().vector()));
            insert.executeUpdate();
        }
    }

    public Optional<StoredDna> findStored(String objectId) {
        List<StoredDna> history = history(objectId);
        return history.isEmpty() ? Optional.empty() : Optional.of(history.getLast());
    }

    public List<StoredDna> history(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return List.of();
        }
        List<StoredDna> history = new ArrayList<>();
        String previous = null;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT dna_version, canonical_json, canonical_sha256, generated_at FROM semantic_dna WHERE object_id = ? ORDER BY dna_version")) {
            statement.setString(1, objectId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    SemanticDna dna = DnaCanonical.deserialize(rows.getString(2));
                    history.add(new StoredDna(dna, rows.getString(3), previous,
                            rows.getTimestamp(4).toInstant()));
                    previous = rows.getString(3);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("reading DNA history failed", e);
        }
        return List.copyOf(history);
    }

    public int nextDnaVersion(String objectId) {
        return maxDnaVersion(objectId) + 1;
    }

    private int maxDnaVersion(String objectId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(dna_version), 0) FROM semantic_dna WHERE object_id = ?")) {
            statement.setString(1, objectId == null ? "" : objectId);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("reading the DNA version failed", e);
        }
    }

    private String canonicalShaOf(String objectId, int version) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT canonical_sha256 FROM semantic_dna WHERE object_id = ? AND dna_version = ?")) {
            statement.setString(1, objectId);
            statement.setInt(2, version);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("reading the DNA hash failed", e);
        }
    }

    public boolean removeDna(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return false;
        }
        try {
            int deleted = 0;
            String[] tables = {"semantic_dna", "embedding", "concept", "topic", "entity",
                    "fact", "relationship", "structure_node", "reconstruction_rule",
                    "security_reference", "ruleset"};
            for (String table : tables) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM " + table + " WHERE object_id = ?")) {
                    statement.setString(1, objectId);
                    deleted += statement.executeUpdate();
                }
            }
            connection.commit();
            return deleted > 0;
        } catch (SQLException e) {
            rollbackQuietly();
            throw new IllegalStateException("removing the Semantic Record failed", e);
        }
    }

    public List<StoredDna> allCurrentDna() {
        List<StoredDna> all = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT object_id FROM semantic_dna GROUP BY object_id ORDER BY object_id")) {
            while (rows.next()) {
                objectIds.add(rows.getString(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("listing DNA objects failed", e);
        }
        for (String objectId : objectIds) {
            findStored(objectId).ifPresent(all::add);
        }
        return List.copyOf(all);
    }

    public int dnaObjectCount() {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT COUNT(DISTINCT object_id) FROM semantic_dna")) {
            rows.next();
            return rows.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("counting DNA objects failed", e);
        }
    }

    public void storeRaw(String objectId, byte[] content) {
        Objects.requireNonNull(content, "content must not be null");
        try {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM raw_content WHERE object_id = ?")) {
                delete.setString(1, objectId);
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO raw_content (object_id, content) VALUES (?, ?)")) {
                insert.setString(1, objectId);
                insert.setBytes(2, content);
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new IllegalStateException("storing raw content failed", e);
        }
    }

    public Optional<byte[]> retrieveRaw(String objectId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT content FROM raw_content WHERE object_id = ?")) {
            statement.setString(1, objectId == null ? "" : objectId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(rows.getBytes(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("reading raw content failed", e);
        }
    }

    public boolean releaseRaw(String objectId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM raw_content WHERE object_id = ?")) {
            statement.setString(1, objectId == null ? "" : objectId);
            boolean removed = statement.executeUpdate() > 0;
            connection.commit();
            return removed;
        } catch (SQLException e) {
            rollbackQuietly();
            throw new IllegalStateException("releasing raw content failed", e);
        }
    }

    public boolean containsRaw(String objectId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM raw_content WHERE object_id = ?")) {
            statement.setString(1, objectId == null ? "" : objectId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("checking raw content failed", e);
        }
    }

    public void saveRuleSet(String objectId, int dnaVersion, String rulesJson,
                            String rulesSha256, Instant boundAt) {
        try {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM ruleset WHERE object_id = ?")) {
                delete.setString(1, objectId);
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO ruleset (object_id, dna_version, rules_json, rules_sha256, bound_at) VALUES (?, ?, ?, ?, ?)")) {
                insert.setString(1, objectId);
                insert.setInt(2, dnaVersion);
                insert.setString(3, rulesJson);
                insert.setString(4, rulesSha256);
                insert.setObject(5, java.sql.Timestamp.from(boundAt));
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new IllegalStateException("saving the rule set failed", e);
        }
    }

    public List<com.sfs.core.rules.RuleSet> loadRuleSets() {
        List<com.sfs.core.rules.RuleSet> sets = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT rules_json FROM ruleset ORDER BY object_id")) {
            while (rows.next()) {
                sets.add(com.sfs.core.rules.RuleSetCanonical.parse(rows.getString(1)));
            }
            return List.copyOf(sets);
        } catch (SQLException e) {
            throw new IllegalStateException("loading rule sets failed", e);
        }
    }

    public void saveObjectState(com.sfs.lifecycle.model.SemanticFile file) {
        Objects.requireNonNull(file, "file must not be null");
        try {
            String objectId = file.objectId().value();
            try (PreparedStatement deleteVersion = connection.prepareStatement(
                    "DELETE FROM file_version WHERE object_id = ?")) {
                deleteVersion.setString(1, objectId);
                deleteVersion.executeUpdate();
            }
            try (PreparedStatement mergeObject = connection.prepareStatement(
                    "MERGE INTO semantic_object (object_id, state, deleted_from, certified_dna_version, state_changed_at) KEY (object_id) VALUES (?, ?, ?, ?, ?)")) {
                mergeObject.setString(1, objectId);
                mergeObject.setString(2, file.state().name());
                mergeObject.setString(3, file.deletedFrom() == null
                        ? null : file.deletedFrom().name());
                mergeObject.setString(4, file.certifiedDnaVersion());
                mergeObject.setObject(5, java.sql.Timestamp.from(file.stateChangedAt()));
                mergeObject.executeUpdate();
            }
            try (PreparedStatement mergeMeta = connection.prepareStatement(
                    "MERGE INTO metadata (object_id, file_name, content_type, size_bytes, sha256, storage_address, registered_at, last_modified_at) KEY (object_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                mergeMeta.setString(1, objectId);
                mergeMeta.setString(2, file.metadata().fileName());
                mergeMeta.setString(3, file.metadata().contentType());
                mergeMeta.setLong(4, file.metadata().sizeBytes());
                mergeMeta.setString(5, file.metadata().sha256());
                mergeMeta.setString(6, file.metadata().storageAddress());
                mergeMeta.setObject(7, java.sql.Timestamp.from(file.metadata().registeredAt()));
                mergeMeta.setObject(8, java.sql.Timestamp.from(file.metadata().lastModifiedAt()));
                mergeMeta.executeUpdate();
            }
            try (PreparedStatement insertVersion = connection.prepareStatement(
                    "INSERT INTO file_version (object_id, number, content_sha256, size_bytes, captured_at) VALUES (?, ?, ?, ?, ?)")) {
                for (com.sfs.lifecycle.model.FileVersion version : file.versions()) {
                    insertVersion.setString(1, objectId);
                    insertVersion.setInt(2, version.number());
                    insertVersion.setString(3, version.contentSha256());
                    insertVersion.setLong(4, version.sizeBytes());
                    insertVersion.setObject(5, java.sql.Timestamp.from(version.capturedAt()));
                    insertVersion.addBatch();
                }
                insertVersion.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new IllegalStateException("persisting the object state failed", e);
        }
    }

    public List<com.sfs.lifecycle.model.SemanticFile> loadObjectStates() {
        List<com.sfs.lifecycle.model.SemanticFile> files = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT o.object_id, o.state, o.deleted_from, o.certified_dna_version, o.state_changed_at, "
                             + "m.file_name, m.content_type, m.size_bytes, m.sha256, m.storage_address, "
                             + "m.registered_at, m.last_modified_at "
                             + "FROM semantic_object o JOIN metadata m ON m.object_id = o.object_id "
                             + "ORDER BY o.object_id")) {
            while (rows.next()) {
                String objectId = rows.getString(1);
                List<com.sfs.lifecycle.model.FileVersion> versions = new ArrayList<>();
                try (PreparedStatement versionStatement = connection.prepareStatement(
                        "SELECT number, content_sha256, size_bytes, captured_at FROM file_version WHERE object_id = ? ORDER BY number")) {
                    versionStatement.setString(1, objectId);
                    try (ResultSet versionRows = versionStatement.executeQuery()) {
                        while (versionRows.next()) {
                            versions.add(new com.sfs.lifecycle.model.FileVersion(
                                    versionRows.getInt(1), versionRows.getString(2),
                                    versionRows.getLong(3), versionRows.getTimestamp(4).toInstant()));
                        }
                    }
                }
                files.add(new com.sfs.lifecycle.model.SemanticFile(
                        com.sfs.core.identity.ObjectId.of(objectId),
                        new com.sfs.lifecycle.model.FileMetadata(
                                rows.getString(6), rows.getString(7), rows.getLong(8),
                                rows.getString(9), rows.getString(10),
                                rows.getTimestamp(11).toInstant(), rows.getTimestamp(12).toInstant()),
                        com.sfs.lifecycle.state.FileState.valueOf(rows.getString(2)),
                        rows.getString(3) == null
                                ? null : com.sfs.lifecycle.state.FileState.valueOf(rows.getString(3)),
                        rows.getString(4),
                        versions,
                        rows.getTimestamp(5).toInstant()));
            }
            return List.copyOf(files);
        } catch (SQLException e) {
            throw new IllegalStateException("loading object states failed", e);
        }
    }

    public void saveLifecycleEvent(com.sfs.lifecycle.model.LifecycleEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        try {
            try (PreparedStatement merge = connection.prepareStatement(
                    "MERGE INTO lifecycle_event (event_id, object_id, type, from_state, to_state, principal_id, refused, reason, at, duration_ms) KEY (event_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                merge.setString(1, event.eventId());
                merge.setString(2, event.objectId());
                merge.setString(3, event.type().name());
                merge.setString(4, event.from() == null ? null : event.from().name());
                merge.setString(5, event.to().name());
                merge.setString(6, event.principalId());
                merge.setBoolean(7, event.refused());
                merge.setString(8, event.reason());
                merge.setObject(9, java.sql.Timestamp.from(event.at()));
                if (event.durationMs() == null) {
                    merge.setNull(10, java.sql.Types.BIGINT);
                } else {
                    merge.setLong(10, event.durationMs());
                }
                merge.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new IllegalStateException("persisting the lifecycle event failed", e);
        }
    }

    public List<com.sfs.lifecycle.model.LifecycleEvent> loadLifecycleEvents() {
        List<com.sfs.lifecycle.model.LifecycleEvent> events = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT event_id, object_id, type, from_state, to_state, principal_id, refused, reason, at, duration_ms FROM lifecycle_event ORDER BY event_id")) {
            while (rows.next()) {
                events.add(new com.sfs.lifecycle.model.LifecycleEvent(
                        rows.getString(1), rows.getString(2),
                        com.sfs.lifecycle.model.LifecycleEventType.valueOf(rows.getString(3)),
                        rows.getString(4) == null
                                ? null : com.sfs.lifecycle.state.FileState.valueOf(rows.getString(4)),
                        com.sfs.lifecycle.state.FileState.valueOf(rows.getString(5)),
                        rows.getString(6), rows.getBoolean(7), rows.getString(8),
                        rows.getTimestamp(9).toInstant(),
                        rows.getObject(10) == null ? null : rows.getLong(10)));
            }
            return List.copyOf(events);
        } catch (SQLException e) {
            throw new IllegalStateException("loading lifecycle events failed", e);
        }
    }

    public List<ObjectSearchData> loadSearchData() {
        List<ObjectSearchData> data = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT o.object_id, m.file_name, m.content_type, o.state "
                             + "FROM semantic_object o JOIN metadata m ON m.object_id = o.object_id "
                             + "ORDER BY o.object_id")) {
            while (rows.next()) {
                objectIds.add(rows.getString(1));
                data.add(new ObjectSearchData(rows.getString(1), rows.getString(2),
                        rows.getString(3), rows.getString(4),
                        null, List.of(), List.of(), List.of(), List.of(),
                        List.of(), 0, null));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("loading search objects failed", e);
        }
        List<ObjectSearchData> complete = new ArrayList<>();
        for (int i = 0; i < objectIds.size(); i++) {
            complete.add(withGraph(data.get(i), objectIds.get(i)));
        }
        return List.copyOf(complete);
    }

    private ObjectSearchData withGraph(ObjectSearchData base, String objectId) {
        return findStored(objectId)
                .map(stored -> {
                    SemanticDna dna = stored.dna();
                    List<String> relationships = dna.relationships().stream()
                            .map(relationship -> relationship.subject() + " "
                                    + relationship.type() + " " + relationship.object())
                            .toList();
                    return new ObjectSearchData(
                            base.objectId(), base.fileName(), base.contentType(),
                            base.state(), dna.summary(),
                            dna.concepts().stream().map(concept -> concept.name()).toList(),
                            dna.topics().stream().map(topic -> topic.name()).toList(),
                            dna.entities().stream().map(entity -> entity.name()).toList(),
                            dna.facts().stream().map(fact -> fact.statement()).toList(),
                            relationships,
                            dna.security().protectedReferences().size(),
                            dna.embedding().dimensions() > 0 ? dna.embedding().vector() : null);
                })
                .orElse(base);
    }

    public Map<String, Object> storageStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("dnaObjects", dnaObjectCount());
        stats.put("dnaVersions", scalar("SELECT COUNT(*) FROM semantic_dna"));
        stats.put("canonicalBytes", scalar("SELECT COALESCE(SUM(LENGTH(canonical_json)), 0) FROM semantic_dna"));
        stats.put("facts", scalar("SELECT COUNT(*) FROM fact"));
        stats.put("entities", scalar("SELECT COUNT(*) FROM entity"));
        stats.put("relationships", scalar("SELECT COUNT(*) FROM relationship"));
        stats.put("structureNodes", scalar("SELECT COUNT(*) FROM structure_node"));
        stats.put("securityReferences", scalar("SELECT COUNT(*) FROM security_reference"));
        stats.put("embeddings", scalar("SELECT COUNT(*) FROM embedding"));
        stats.put("ruleSets", scalar("SELECT COUNT(*) FROM ruleset"));
        stats.put("lifecycleEvents", scalar("SELECT COUNT(*) FROM lifecycle_event"));
        stats.put("storedObjects", scalar("SELECT COUNT(*) FROM semantic_object"));
        stats.put("rawContentEntries", scalar("SELECT COUNT(*) FROM raw_content"));
        return stats;
    }

    private long scalar(String query) {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(query)) {
            rows.next();
            return rows.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException("storage accounting query failed", e);
        }
    }

    void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}
