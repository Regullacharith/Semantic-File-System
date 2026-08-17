package com.sfs.ui.view;

import java.util.Arrays;
import java.util.List;

/**
 * The navigation destinations of the SFS user interface.
 */
public enum NavigationItem {

    /** Dashboard / landing view.  */
    HOME("Dashboard", "/", "Overview of the Semantic File System", true),

    /** File import, analyze and delete controls. */
    FILES("Files", "/files", "Import, analyze and delete files", true),

    /** Semantic Search view.  */
    SEARCH("Search", "/search", "Find semantic records by meaning", true),

    /** Object / Semantic DNA inspection view. */
    OBJECTS("Objects", "/objects", "Inspect Object IDs and Semantic DNA", false),

    /** Single-click reconstruction flow.  */
    RECONSTRUCTION("Reconstruction", "/reconstruction", "Reconstruct artifacts from semantic memory", false),

    /** Evaluation / fidelity view.  */
    EVALUATION("Evaluation", "/evaluation", "Semantic, structural and factual fidelity reports", false),

    /** Security and policy settings.  */
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

    
    public boolean isAvailable() {
        return available;
    }

    /**
     * All navigation destinations in display order.
     */
    public static List<NavigationItem> all() {
        return List.of(values());
    }

 
    public static List<NavigationItem> planned() {
        return Arrays.stream(values())
                .filter(item -> !item.available)
                .toList();
    }
}
