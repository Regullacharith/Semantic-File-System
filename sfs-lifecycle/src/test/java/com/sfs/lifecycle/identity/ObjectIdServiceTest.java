package com.sfs.lifecycle.identity;

import com.sfs.core.identity.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObjectIdService")
class ObjectIdServiceTest {

    private final ObjectIdService service = new ObjectIdService();

    @Test
    @DisplayName("produces identifiers that pass Object ID validation")
    void producesValidIdentifiers() {
        ObjectId id = service.next();
        assertThat(ObjectId.isValid(id.value())).isTrue();
        assertThat(id.value()).startsWith("sfs-obj-");
    }

    @Test
    @DisplayName("produces a distinct identifier on every allocation")
    void producesDistinctIdentifiers() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            seen.add(service.next().value());
        }
        assertThat(seen).hasSize(1000);
    }

    @Test
    @DisplayName("sequence block increases monotonically; suffix is eight hex characters")
    void sequenceIncreasesAndSuffixIsEightHexCharacters() {
        ObjectId first = service.next();
        ObjectId second = service.next();
        int firstSequence = Integer.parseInt(first.value().split("-")[2]);
        int secondSequence = Integer.parseInt(second.value().split("-")[2]);
        assertThat(secondSequence).isGreaterThan(firstSequence);

        String suffix = second.value().split("-")[3];
        assertThat(suffix).hasSize(8).matches("[0-9a-f]{8}");
    }

    @Test
    @DisplayName("nextUnique skips identifiers already taken")
    void skipsTakenIdentifiers() {
        Set<String> taken = new HashSet<>();
        ObjectId first = service.nextUnique(taken::contains);
        taken.add(first.value());
        ObjectId second = service.nextUnique(taken::contains);
        assertThat(second.value()).isNotEqualTo(first.value());
    }

    @Test
    @DisplayName("allocated identifiers are not derived from any file attribute")
    void identifiersAreIndependentOfNamesakes() {
        String a = service.next().value();
        String b = service.next().value();
        assertThat(a).isNotEqualTo(b);
        assertThat(service.allocatedCount()).isEqualTo(2);
    }
}
