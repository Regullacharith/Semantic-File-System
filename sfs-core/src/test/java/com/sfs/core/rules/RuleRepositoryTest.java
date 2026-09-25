package com.sfs.core.rules;

import com.sfs.core.dna.DnaCanonical;
import com.sfs.core.dna.SemanticDna;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RuleRepository")
class RuleRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");

    private final RuleRepository repository = new RuleRepository();
    private final RuleDeriver deriver = new RuleDeriver();

    @Test
    @DisplayName("the first lookup derives and binds; the identical lookup reuses")
    void deriveThenReuse() {
        SemanticDna dna = RuleSetsFixture.dna();

        RuleLookup first = repository.findOrDerive(dna, deriver, T0);
        assertThat(first.reused()).isFalse();
        assertThat(first.versionConflict()).isNull();
        assertThat(repository.derivations()).isEqualTo(1);

        RuleLookup second = repository.findOrDerive(dna, deriver, T0);
        assertThat(second.reused()).isTrue();
        assertThat(second.ruleSet()).isEqualTo(first.ruleSet());
        assertThat(repository.hits()).isEqualTo(1);
    }

    @Test
    @DisplayName("a new DNA version derives a freshly bound set")
    void versionBumpRebinds() {
        SemanticDna v1 = RuleSetsFixture.dna();
        SemanticDna v2 = withVersion(v1, 2);
        repository.findOrDerive(v1, deriver, T0);

        RuleLookup lookup = repository.findOrDerive(v2, deriver, T0);

        assertThat(lookup.reused()).isFalse();
        assertThat(lookup.ruleSet().dnaVersion()).isEqualTo(2);
        assertThat(lookup.ruleSet().dnaSha256()).isEqualTo(DnaCanonical.integrityHash(v2));
        assertThat(lookup.versionConflict()).isNull();
    }

    @Test
    @DisplayName("the same version with different content is an explicit version conflict")
    void sameVersionDifferentContentConflicts() {
        SemanticDna original = RuleSetsFixture.dna();
        repository.findOrDerive(original, deriver, T0);

        SemanticDna tampered = summaryVariant(original);
        RuleLookup lookup = repository.findOrDerive(tampered, deriver, T0);

        assertThat(lookup.reused()).isFalse();
        assertThat(lookup.versionConflict()).isNotNull();
        assertThat(lookup.versionConflict().description())
                .contains("different DNA content");
        assertThat(repository.versionConflicts()).isEqualTo(1);
        assertThat(lookup.ruleSet().dnaSha256())
                .isEqualTo(DnaCanonical.integrityHash(tampered));
    }

    @Test
    @DisplayName("an explicitly saved set is served when its binding matches")
    void loaderPathThroughSave() {
        SemanticDna dna = RuleSetsFixture.dna();
        RuleSet handLoaded = new RuleSet(dna.objectId(), 1,
                DnaCanonical.integrityHash(dna), "sfs-rules/0.2",
                RuleSetsFixture.everyKind().rules());
        repository.save(handLoaded, T0);

        RuleLookup lookup = repository.findOrDerive(dna, deriver, T0);

        assertThat(lookup.reused()).isTrue();
        assertThat(lookup.ruleSet()).isEqualTo(handLoaded);
    }

    private SemanticDna withVersion(SemanticDna dna, int version) {
        return new SemanticDna(
                new com.sfs.core.dna.DnaIdentity(dna.objectId(), dna.schemaVersion(),
                        version, dna.identity().engineVersion(),
                        dna.identity().generatedAt()),
                dna.summary(), dna.concepts(), dna.topics(), dna.entities(),
                dna.facts(), dna.relationships(), dna.structure(), dna.embedding(),
                dna.behaviour(), dna.reconstructionRules(), dna.fidelity(),
                dna.security());
    }

    private SemanticDna summaryVariant(SemanticDna dna) {
        return new SemanticDna(
                dna.identity(), dna.summary() + " Changed.", dna.concepts(), dna.topics(),
                dna.entities(), dna.facts(), dna.relationships(), dna.structure(),
                dna.embedding(), dna.behaviour(), dna.reconstructionRules(),
                dna.fidelity(), dna.security());
    }
}
