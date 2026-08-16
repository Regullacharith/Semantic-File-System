package com.sfs.ui.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PageViewModel")
class PageViewModelTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("exposes the supplied title, active item and navigation")
        void exposesSuppliedValues() {
            PageViewModel page = PageViewModel.of("Dashboard", NavigationItem.HOME);

            assertThat(page.title()).isEqualTo("Dashboard");
            assertThat(page.activeItem()).isEqualTo(NavigationItem.HOME);
            assertThat(page.navigation()).containsExactly(NavigationItem.values());
        }

        @Test
        @DisplayName("rejects a null title")
        void rejectsNullTitle() {
            assertThatThrownBy(() -> PageViewModel.of(null, NavigationItem.HOME))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("title");
        }

        @Test
        @DisplayName("rejects a blank title")
        void rejectsBlankTitle() {
            assertThatThrownBy(() -> PageViewModel.of("   ", NavigationItem.HOME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("rejects a null active item")
        void rejectsNullActiveItem() {
            assertThatThrownBy(() -> PageViewModel.of("Dashboard", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("activeItem");
        }

        @Test
        @DisplayName("rejects a null navigation list")
        void rejectsNullNavigation() {
            assertThatThrownBy(() -> new PageViewModel("Dashboard", NavigationItem.HOME, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("navigation");
        }
    }

    @Nested
    @DisplayName("immutability")
    class Immutability {

        @Test
        @DisplayName("is unaffected by later mutation of the caller's list")
        void defensivelyCopiesNavigation() {
            List<NavigationItem> mutable = new ArrayList<>();
            mutable.add(NavigationItem.HOME);

            PageViewModel page = new PageViewModel("Dashboard", NavigationItem.HOME, mutable);
            mutable.add(NavigationItem.SEARCH);

            assertThat(page.navigation()).containsExactly(NavigationItem.HOME);
        }

        @Test
        @DisplayName("returns a navigation list that cannot be modified")
        void navigationListIsUnmodifiable() {
            PageViewModel page = PageViewModel.of("Dashboard", NavigationItem.HOME);

            assertThatThrownBy(() -> page.navigation().add(NavigationItem.FILES))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("active state")
    class ActiveState {

        @Test
        @DisplayName("reports the active item as active")
        void reportsActiveItem() {
            PageViewModel page = PageViewModel.of("Dashboard", NavigationItem.HOME);

            assertThat(page.isActive(NavigationItem.HOME)).isTrue();
        }

        @Test
        @DisplayName("reports every other item as inactive")
        void reportsOtherItemsInactive() {
            PageViewModel page = PageViewModel.of("Dashboard", NavigationItem.HOME);

            assertThat(page.isActive(NavigationItem.SEARCH)).isFalse();
            assertThat(page.isActive(null)).isFalse();
        }
    }
}
