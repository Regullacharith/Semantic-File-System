package com.sfs.ui.view;

import java.util.List;
import java.util.Objects;

public record PageViewModel(
        String title,
        NavigationItem activeItem,
        List<NavigationItem> navigation) {

    /**
     * Canonical constructor.
     */
    public PageViewModel {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(activeItem, "activeItem must not be null");
        Objects.requireNonNull(navigation, "navigation must not be null");

        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }

        navigation = List.copyOf(navigation);
    }

    public static PageViewModel of(String title, NavigationItem activeItem) {
        return new PageViewModel(title, activeItem, NavigationItem.all());
    }

    public boolean isActive(NavigationItem item) {
        return activeItem == item;
    }
}
