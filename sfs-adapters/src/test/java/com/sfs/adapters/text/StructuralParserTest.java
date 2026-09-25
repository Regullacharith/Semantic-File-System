package com.sfs.adapters.text;

import com.sfs.contracts.semantic.SemanticDnaView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StructuralParser")
class StructuralParserTest {

    private final StructuralParser parser = new StructuralParser();

    @Test
    @DisplayName("extracts markdown headings with their levels in order")
    void markdownHeadings() {
        List<SemanticDnaView.StructureNodeView> nodes = parser.parse("""
                # Summary

                Words about the summary here.

                ## Detail

                More words in the detail section.
                """);

        assertThat(nodes).hasSize(2);
        assertThat(nodes.get(0).heading()).isEqualTo("Summary");
        assertThat(nodes.get(0).level()).isEqualTo(1);
        assertThat(nodes.get(0).order()).isZero();
        assertThat(nodes.get(1).heading()).isEqualTo("Detail");
        assertThat(nodes.get(1).level()).isEqualTo(2);
        assertThat(nodes.get(1).order()).isEqualTo(1);
    }

    @Test
    @DisplayName("extracts numbered headings without sentence punctuation")
    void numberedHeadings() {
        List<SemanticDnaView.StructureNodeView> nodes = parser.parse("""
                1. Overview

                The overview section has words.

                2.1 Measurements

                The measurements section has words.
                """);

        assertThat(nodes).hasSize(2);
        assertThat(nodes.get(0).heading()).isEqualTo("Overview");
        assertThat(nodes.get(1).heading()).isEqualTo("Measurements");
        assertThat(nodes.get(1).level()).isEqualTo(2);
    }

    @Test
    @DisplayName("an unstructured document falls back to a single Body node")
    void bodyFallback() {
        List<SemanticDnaView.StructureNodeView> nodes =
                parser.parse("Just a plain line of words.\n");
        assertThat(nodes).hasSize(1);
        assertThat(nodes.getFirst().heading()).isEqualTo("Body");
        assertThat(nodes.getFirst().level()).isEqualTo(1);
    }

    @Test
    @DisplayName("an all-caps line is a level-one heading")
    void allCapsHeading() {
        List<SemanticDnaView.StructureNodeView> nodes = parser.parse(
                "INTRODUCTION\n\nSome words follow here in the text.\n");
        assertThat(nodes).hasSize(1);
        assertThat(nodes.getFirst().heading()).isEqualTo("INTRODUCTION");
    }

    @Test
    @DisplayName("a numbered sentence is not mistaken for a heading")
    void numberedSentenceIsBody() {
        List<SemanticDnaView.StructureNodeView> nodes = parser.parse(
                "3 releases shipped in 2026 for the platform.\n");
        assertThat(nodes).hasSize(1);
        assertThat(nodes.getFirst().heading()).isEqualTo("Body");
    }
}
