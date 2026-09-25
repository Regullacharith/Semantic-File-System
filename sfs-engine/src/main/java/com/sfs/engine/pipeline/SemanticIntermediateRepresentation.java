package com.sfs.engine.pipeline;

import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.core.dna.SemanticDna;
import com.sfs.contracts.semantic.ProtectedReferenceView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SemanticIntermediateRepresentation {

    private final String rawText;
    private final List<String> rawLines;

    private List<String> paragraphs = List.of();
    private List<String> sentences = List.of();
    private List<String> tokens = List.of();
    private Map<String, Long> wordFrequency = new LinkedHashMap<>();
    private Map<String, Long> bigramFrequency = new LinkedHashMap<>();

    private List<SemanticDnaView.StructureNodeView> structure = new ArrayList<>();
    private List<String> concepts = new ArrayList<>();
    private List<String> topics = new ArrayList<>();
    private List<SemanticDnaView.EntityView> entities = new ArrayList<>();
    private List<SemanticDnaView.FactView> facts = new ArrayList<>();
    private List<SemanticDnaView.RelationshipView> relationships = new ArrayList<>();
    private List<ProtectedReferenceView> protectedReferences = new ArrayList<>();
    private double[] embedding = new double[0];
    private String summary = "";
    private SemanticDna dnaDraft;

    public SemanticIntermediateRepresentation(String rawText) {
        this.rawText = Objects.requireNonNull(rawText, "rawText must not be null");
        this.rawLines = List.of(rawText.split("\n", -1));
    }

    public String rawText() {
        return rawText;
    }

    public List<String> rawLines() {
        return rawLines;
    }

    public List<String> paragraphs() {
        return paragraphs;
    }

    public List<String> sentences() {
        return sentences;
    }

    public List<String> tokens() {
        return tokens;
    }

    public Map<String, Long> wordFrequency() {
        return wordFrequency;
    }

    public Map<String, Long> bigramFrequency() {
        return bigramFrequency;
    }

    public List<SemanticDnaView.StructureNodeView> structure() {
        return structure;
    }

    public List<String> concepts() {
        return concepts;
    }

    public List<String> topics() {
        return topics;
    }

    public List<SemanticDnaView.EntityView> entities() {
        return entities;
    }

    public List<SemanticDnaView.FactView> facts() {
        return facts;
    }

    public List<SemanticDnaView.RelationshipView> relationships() {
        return relationships;
    }

    public List<ProtectedReferenceView> protectedReferences() {
        return protectedReferences;
    }

    public double[] embedding() {
        return embedding;
    }

    public String summary() {
        return summary;
    }

    public SemanticDna dnaDraft() {
        return dnaDraft;
    }

    void setParagraphs(List<String> paragraphs) {
        this.paragraphs = List.copyOf(paragraphs);
    }

    void setSentences(List<String> sentences) {
        this.sentences = List.copyOf(sentences);
    }

    void setTokens(List<String> tokens) {
        this.tokens = List.copyOf(tokens);
    }

    void setWordFrequency(Map<String, Long> wordFrequency) {
        this.wordFrequency = new LinkedHashMap<>(wordFrequency);
    }

    void setBigramFrequency(Map<String, Long> bigramFrequency) {
        this.bigramFrequency = new LinkedHashMap<>(bigramFrequency);
    }

    void setStructure(List<SemanticDnaView.StructureNodeView> structure) {
        this.structure = List.copyOf(structure);
    }

    void setConcepts(List<String> concepts) {
        this.concepts = List.copyOf(concepts);
    }

    void setTopics(List<String> topics) {
        this.topics = List.copyOf(topics);
    }

    void setEntities(List<SemanticDnaView.EntityView> entities) {
        this.entities = List.copyOf(entities);
    }

    void setFacts(List<SemanticDnaView.FactView> facts) {
        this.facts = List.copyOf(facts);
    }

    void setRelationships(List<SemanticDnaView.RelationshipView> relationships) {
        this.relationships = List.copyOf(relationships);
    }

    void setProtectedReferences(List<ProtectedReferenceView> protectedReferences) {
        this.protectedReferences = List.copyOf(protectedReferences);
    }

    void setEmbedding(double[] embedding) {
        this.embedding = embedding == null ? new double[0] : embedding.clone();
    }

    void setSummary(String summary) {
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
    }

    void setDnaDraft(SemanticDna dnaDraft) {
        this.dnaDraft = Objects.requireNonNull(dnaDraft, "dnaDraft must not be null");
    }
}
