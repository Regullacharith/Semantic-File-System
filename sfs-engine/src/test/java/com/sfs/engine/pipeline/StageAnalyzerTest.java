package com.sfs.engine.pipeline;

import com.sfs.engine.core.SemanticContext;
import com.sfs.engine.level.AnalysisLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pipeline stages")
class StageAnalyzerTest {

    private static final String DOCUMENT = """
            # Summary

            This report reviews the database platform for Q3 2026. Query latency
            decreased by 40 percent after indexing changes were deployed in August.

            # Measurements

            PostgreSQL hosts the production workload for the analytics platform.
            The reporting period covers Q3 2026.
            """ + "\n";

    private SemanticContext context() {
        return new SemanticContext("sfs-obj-0001-a1b2c3d4", DOCUMENT,
                com.sfs.engine.core.Digests.sha256Hex(DOCUMENT),
                AnalysisLevel.STANDARD, "sfs-engine/0.1", 1,
                Instant.parse("2026-03-15T10:00:00Z"));
    }

    private SemanticIntermediateRepresentation parsed() {
        SemanticIntermediateRepresentation ir = new SemanticIntermediateRepresentation(DOCUMENT);
        new TextParsingAnalyzer().perform(context(), ir);
        return ir;
    }

    @Test
    @DisplayName("text parsing extracts paragraphs, sentences, tokens and frequencies")
    void textParsingExtractsStructure() {
        SemanticIntermediateRepresentation ir = parsed();

        assertThat(ir.paragraphs()).hasSize(4);
        assertThat(ir.sentences()).isNotEmpty();
        assertThat(ir.sentences()).noneMatch(sentence -> sentence.startsWith("#"));
        assertThat(ir.tokens()).contains("postgresql", "latency");
        assertThat(ir.wordFrequency()).containsEntry("postgresql", 1L);
        assertThat(ir.bigramFrequency()).containsEntry("q3 2026", 2L);
    }

    @Test
    @DisplayName("headings are not treated as sentences or summary text")
    void headingsStayOutOfSentences() {
        SemanticIntermediateRepresentation ir = parsed();
        new SummaryAnalyzer().perform(context(), ir);

        assertThat(ir.summary()).startsWith("This report reviews");
        assertThat(ir.summary()).doesNotContain("#");
    }

    @Test
    @DisplayName("structure extraction finds markdown headings and falls back to Body")
    void structureExtraction() {
        SemanticContext ctx = context();
        SemanticIntermediateRepresentation ir = new SemanticIntermediateRepresentation(DOCUMENT);
        new TextParsingAnalyzer().perform(ctx, ir);
        new StructureAnalyzer().perform(ctx, ir);
        assertThat(ir.structure()).hasSize(2);
        assertThat(ir.structure().get(0).heading()).isEqualTo("Summary");
        assertThat(ir.structure().get(0).level()).isEqualTo(1);

        SemanticIntermediateRepresentation plain = new SemanticIntermediateRepresentation(
                "Just a plain line of text.\n");
        new StructureAnalyzer().perform(ctx, plain);
        assertThat(plain.structure()).hasSize(1);
        assertThat(plain.structure().getFirst().heading()).isEqualTo("Body");
    }

    @Test
    @DisplayName("concepts prefer repeated bigrams; topics cover distinct unigrams")
    void conceptsAndTopics() {
        SemanticIntermediateRepresentation ir = parsed();
        new ConceptAnalyzer().perform(context(), ir);
        new TopicAnalyzer().perform(context(), ir);

        assertThat(ir.concepts()).isNotEmpty();
        assertThat(ir.concepts()).contains("q3 2026");
        assertThat(ir.topics()).isNotEmpty();
        assertThat(ir.topics()).doesNotContain("the", "and");
    }

    @Test
    @DisplayName("entities require mid-sentence evidence, digits or multiple words")
    void entities() {
        SemanticIntermediateRepresentation ir = parsed();
        new EntityAnalyzer().perform(context(), ir);

        assertThat(ir.entities()).isNotEmpty();
        assertThat(ir.entities()).anySatisfy(entity -> {
            assertThat(entity.name()).isEqualTo("PostgreSQL");
            assertThat(entity.mentions()).isEqualTo(1);
        });
        assertThat(ir.entities()).noneMatch(entity -> entity.name().equals("The"));
        assertThat(ir.entities()).noneMatch(entity -> entity.name().matches("\\d+"));
    }

    @Test
    @DisplayName("facts capture percent and date statements with bounded confidence")
    void facts() {
        SemanticIntermediateRepresentation ir = parsed();
        new FactAnalyzer().perform(context(), ir);

        assertThat(ir.facts()).isNotEmpty();
        assertThat(ir.facts()).anySatisfy(fact -> {
            assertThat(fact.statement()).contains("40 percent");
            assertThat(fact.critical()).isTrue();
        });
        assertThat(ir.facts()).allSatisfy(fact ->
                assertThat(fact.confidence()).isBetween(0.72, 0.95));
    }

    @Test
    @DisplayName("relationships extract subject, verb and object triples")
    void relationships() {
        SemanticIntermediateRepresentation ir = parsed();
        new RelationshipAnalyzer().perform(context(), ir);

        assertThat(ir.relationships()).isNotEmpty();
        assertThat(ir.relationships()).anySatisfy(relationship -> {
            assertThat(relationship.subject()).isEqualTo("PostgreSQL");
            assertThat(relationship.type()).isEqualTo("hosts");
            assertThat(relationship.object()).isNotBlank();
        });
    }

    @Test
    @DisplayName("embeddings are deterministic, normalized and of fixed dimension")
    void embeddings() {
        SemanticIntermediateRepresentation ir = parsed();
        new EmbeddingAnalyzer().perform(context(), ir);

        double[] first = ir.embedding();
        assertThat(first).hasSize(EmbeddingAnalyzer.DIMENSIONS);
        double norm = 0.0;
        for (double value : first) {
            norm += value * value;
        }
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));

        SemanticIntermediateRepresentation again = parsed();
        new EmbeddingAnalyzer().perform(context(), again);
        assertThat(again.embedding()).containsExactly(first);
    }

    @Test
    @DisplayName("protected values are referenced without ever copying the value")
    void protectedValues() {
        String secret = "password=hunter2\ncontact=ops@example.com\n";
        SemanticContext ctx = new SemanticContext("sfs-obj-0002-e5f6a7b8", secret,
                com.sfs.engine.core.Digests.sha256Hex(secret),
                AnalysisLevel.STANDARD, "sfs-engine/0.1", 1,
                Instant.parse("2026-03-15T10:00:00Z"));
        SemanticIntermediateRepresentation ir = new SemanticIntermediateRepresentation(secret);
        new ProtectedValueAnalyzer().perform(ctx, ir);

        List<com.sfs.contracts.semantic.ProtectedReferenceView> references =
                ir.protectedReferences();
        assertThat(references).hasSize(2);
        assertThat(references.get(0).sensitiveType().name()).isEqualTo("PASSWORD");
        assertThat(references.get(1).sensitiveType().name()).isEqualTo("EMAIL_ADDRESS");
        assertThat(references).allSatisfy(reference -> {
            assertThat(reference.location()).startsWith("line ");
            assertThat(reference.referenceId()).startsWith("sfs-ref-");
        });
        assertThat(references.toString())
                .doesNotContain("hunter2")
                .doesNotContain("ops@example.com");
    }

    @Test
    @DisplayName("the dna builder produces a complete view with versioned schema")
    void dnaBuilder() {
        SemanticIntermediateRepresentation ir = parsed();
        var ctx = context();
        new SummaryAnalyzer().perform(ctx, ir);
        new StructureAnalyzer().perform(ctx, ir);
        new ConceptAnalyzer().perform(ctx, ir);
        new TopicAnalyzer().perform(ctx, ir);
        new EntityAnalyzer().perform(ctx, ir);
        new FactAnalyzer().perform(ctx, ir);
        new RelationshipAnalyzer().perform(ctx, ir);
        new EmbeddingAnalyzer().perform(ctx, ir);
        new ProtectedValueAnalyzer().perform(ctx, ir);
        new DnaBuilderAnalyzer().perform(ctx, ir);

        var dna = ir.dnaDraft();
        assertThat(dna).isNotNull();
        assertThat(dna.objectId()).isEqualTo("sfs-obj-0001-a1b2c3d4");
        assertThat(dna.schemaVersion()).isEqualTo("sfs-dna/0.1");
        assertThat(dna.dnaVersion()).isEqualTo(1);
        assertThat(dna.summary()).isNotBlank();
        assertThat(dna.concepts()).isNotEmpty();
        assertThat(dna.topics()).isNotEmpty();
        assertThat(dna.entities()).isNotEmpty();
        assertThat(dna.facts()).isNotEmpty();
        assertThat(dna.relationships()).isNotEmpty();
        assertThat(dna.structure()).isNotEmpty();
        assertThat(dna.embeddingDimensions()).isEqualTo(EmbeddingAnalyzer.DIMENSIONS);
        assertThat(dna.fidelity().analyzerVersion()).isEqualTo("sfs-engine/0.1");
    }
}