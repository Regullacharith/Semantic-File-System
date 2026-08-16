/**
 * SFS application contracts.
 *
 * <p><strong>Architectural boundary.</strong> Service interfaces and request/response models
 * shared between the user interface and the backend subsystems. This is the seam described
 * in the Milestone 01 specification as "API contracts; can begin with mocks".
 *
 * <p><strong>Dependency rule.</strong> Interfaces and immutable data carriers only. No
 * implementations, no framework annotations, no HTTP concerns, no persistence concerns.
 * Depends only on {@code sfs-core}.
 *
 * <p><strong>Ownership.</strong> Milestone 02 (Application &amp; API Layer) owns this module.
 * Milestone 01 introduces only the minimum contracts its seven views require, so that
 * Milestone 02 extends them rather than rewriting them. Any change Milestone 02 must make
 * to a contract introduced in Milestone 01 is reported as an integration dependency.
 *
 * <p><strong>Status.</strong> Intentionally empty at Task 01.0.
 */
package com.sfs.contracts;
