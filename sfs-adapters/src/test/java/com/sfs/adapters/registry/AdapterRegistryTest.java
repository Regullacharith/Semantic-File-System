package com.sfs.adapters.registry;

import com.sfs.adapters.spi.AdapterDescriptor;
import com.sfs.adapters.spi.AdapterRequest;
import com.sfs.adapters.spi.AdapterResult;
import com.sfs.adapters.spi.FileTypeAdapter;
import com.sfs.adapters.spi.TextMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AdapterRegistry")
class AdapterRegistryTest {

    private final AdapterRegistry registry = new AdapterRegistry();

    private FileTypeAdapter adapter(String id) {
        return new FileTypeAdapter() {

            @Override
            public AdapterDescriptor descriptor() {
                return new AdapterDescriptor(id, id + " name", id + "/0.1",
                        Set.of("txt"), Set.of(), Set.of("text-extraction"));
            }

            @Override
            public AdapterResult adapt(AdapterRequest request) {
                return new AdapterResult(id, id + "/0.1", "text",
                        new TextMetrics(4, 1, 1, 1, 1.0), List.of());
            }
        };
    }

    @Test
    @DisplayName("a fresh registry is empty")
    void freshRegistryIsEmpty() {
        assertThat(registry.count()).isZero();
        assertThat(registry.all()).isEmpty();
        assertThat(registry.descriptors()).isEmpty();
        assertThat(registry.find("sfs-adapter-text")).isEmpty();
    }

    @Test
    @DisplayName("registration makes the adapter findable by id")
    void registrationMakesAdapterFindable() {
        FileTypeAdapter adapter = adapter("sfs-adapter-text");
        registry.register(adapter);

        assertThat(registry.count()).isEqualTo(1);
        assertThat(registry.find("sfs-adapter-text")).contains(adapter);
        assertThat(registry.descriptors().getFirst().id()).isEqualTo("sfs-adapter-text");
    }

    @Test
    @DisplayName("duplicate registration is refused with an explicit error")
    void duplicateRegistrationRefused() {
        registry.register(adapter("sfs-adapter-text"));
        assertThatThrownBy(() -> registry.register(adapter("sfs-adapter-text")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("adapters keep their registration order")
    void registrationOrderIsPreserved() {
        registry.register(adapter("sfs-adapter-a"));
        registry.register(adapter("sfs-adapter-b"));
        registry.register(adapter("sfs-adapter-c"));

        assertThat(registry.descriptors()).extracting(AdapterDescriptor::id)
                .containsExactly("sfs-adapter-a", "sfs-adapter-b", "sfs-adapter-c");
    }

    @Test
    @DisplayName("lookups of unknown or blank identifiers are empty")
    void unknownLookupsAreEmpty() {
        registry.register(adapter("sfs-adapter-text"));
        assertThat(registry.find("sfs-adapter-other")).isEmpty();
        assertThat(registry.find("")).isEmpty();
        assertThat(registry.find(null)).isEmpty();
    }
}
