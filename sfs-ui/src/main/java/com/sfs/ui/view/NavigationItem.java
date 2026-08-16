package com.sfs.ui.view;

import java.util.Arrays;
import java.util.List;

public enum NavigationItem {

    /** Dashboard / landing view. Owned by Task 01.1. */
    HOME("Dashboard", "/", "Overview of the Semantic File System", true),

    /** File import, analyze and delete controls. Owned by Task 01.2. */
    FILES("Files", "/files", "Import, analyze and delete files", false),

    /** Semantic Search view. Owned by Task 01.3. */
    SEARCH("Search", "/search", "Find semantic records by meaning", false),

    /** Object / Semantic DNA inspection view. Owned by Task 01.4. */
    OBJECTS("Objects", "/objects", "Inspect Object IDs and Semantic DNA", false),

    /** Single-click reconstruction flow. Owned by Task 01.5. */
    RECONSTRUCTION("Reconstruction", "/reconstruction", "Reconstruct artifacts from semantic memory", false),

    /** Evaluation / fidelity view. Owned by Task 01.6. */
    EVALUATION("Evaluation", "/evaluation", "Semantic, structural and factual fidelity reports", false),

    /** Security and policy settings. Owned by Task 01.7. */
    SETTINGS("Settings", "/settings", "Security and privacy policy configuration", false);

    private final String label;
    private final String path;
    private final String description;
    private final boolean available;

    NavigationItem(String label, String path, String description, boolean available) {
        this.label = label;
        this.path = path;
        this.description = description;
        this.available = available;
    }

    public String getLabel() {
        return label;
    }

    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Whether the destination has an implemented route.
     *
     * @return {@code true} if the screen exists and may be linked to
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * All navigation destinations in display order.
     *
     * @return an immutable list in declaration order
     */
    public static List<NavigationItem> all() {
        return List.of(values());
    }

    /**
     * Destinations that are not yet implemented.
     *
     * @return an immutable list of planned destinations, in declaration order
     */
    public static List<NavigationItem> planned() {
        return Arrays.stream(values())
                .filter(item -> !item.available)
                .toList();
    }
}
