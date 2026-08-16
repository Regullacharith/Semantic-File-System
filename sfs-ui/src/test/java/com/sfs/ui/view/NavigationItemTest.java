package com.sfs.ui.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NavigationItem")
class NavigationItemTest {

    @Test
    @DisplayName("covers the seven Milestone 01 destinations")
    void coversAllMilestoneOneDestinations() {
        assertThat(NavigationItem.values()).hasSize(7);
    }

    @Test
    @DisplayName("marks only the dashboard as implemented at Task 01.1")
    void onlyDashboardIsAvailable() {
        assertThat(NavigationItem.all())
                .filteredOn(NavigationItem::isAvailable)
                .containsExactly(NavigationItem.HOME);

        assertThat(NavigationItem.planned()).hasSize(6);
    }

    @ParameterizedTest
    @EnumSource(NavigationItem.class)
    @DisplayName("declares a label, path and description")
    void declaresCompleteMetadata(NavigationItem item) {
        assertThat(item.getLabel()).isNotBlank();
        assertThat(item.getDescription()).isNotBlank();
        assertThat(item.getPath())
                .isNotBlank()
                .startsWith("/");
    }

    @Test
    @DisplayName("declares a unique path per destination")
    void pathsAreUnique() {
        assertThat(NavigationItem.all())
                .extracting(NavigationItem::getPath)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("returns navigation in declaration order")
    void preservesDeclarationOrder() {
        assertThat(NavigationItem.all())
                .startsWith(NavigationItem.HOME)
                .containsExactly(NavigationItem.values());
    }

    @Test
    @DisplayName("returns an unmodifiable navigation list")
    void listsAreUnmodifiable() {
        assertThat(NavigationItem.all().getClass().getSimpleName())
                .as("List.of / toList produce immutable lists")
                .isNotEqualTo("ArrayList");
    }
}
