/**
 * SFS core domain model.
 *
 * <p><strong>Architectural boundary.</strong> This package and its subpackages contain the
 * pure domain model of the Semantic File System: Object ID, Semantic File, Semantic DNA
 * types, lifecycle types and domain exceptions.
 *
 * <p><strong>Dependency rule.</strong> This module has no production dependencies. It must
 * not import Spring, Jackson, a logging framework, a persistence API, or any other
 * third-party library. The domain model is depended upon by every other subsystem, so a
 * framework dependency here would leak into the adapter framework, the memory system, the
 * reconstruction model and the evaluation system, defeating the modularity requirement in
 * the SFS V1 specification.
 *
 * <p><strong>Ownership.</strong> Milestone 03 (File Lifecycle Manager) and Milestone 06
 * (Semantic Representation System) own the contents of this module. Milestone 01 creates
 * the module but adds no domain types to it.
 *
 * <p><strong>Status.</strong> Intentionally empty at Task 01.0. Types arrive with their
 * owning milestone; no speculative placeholder types are created.
 */
package com.sfs.core;
