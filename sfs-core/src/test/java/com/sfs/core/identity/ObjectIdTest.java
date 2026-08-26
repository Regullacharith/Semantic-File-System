package com.sfs.core.identity;

import com.sfs.core.identity.ObjectId.InvalidObjectIdException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Object ID")
class ObjectIdTest {

    private static final String VALID = "sfs-obj-0001-a1b2c3d4";

    @Nested
    @DisplayName("acceptance")
    class Acceptance {

        @Test
        @DisplayName("accepts the canonical format")
        void acceptsCanonicalFormat() {
            assertThat(ObjectId.of(VALID).value()).isEqualTo(VALID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "sfs-obj-0001-a",
                "sfs-obj-9999-abcdefgh",
                "sfs-obj-0042-A1B2C3D4",
                "sfs-obj-0007-aaaa"})
        @DisplayName("accepts every valid seeded and generated shape")
        void acceptsValidShapes(String value) {
            assertThat(ObjectId.of(value).value()).isEqualTo(value);
        }

        @Test
        @DisplayName("trims surrounding whitespace")
        void trimsWhitespace() {
            assertThat(ObjectId.of("  " + VALID + "  ").value()).isEqualTo(VALID);
        }
    }

    @Nested
    @DisplayName("rejection")
    class Rejection {

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertThatThrownBy(() -> ObjectId.of(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t"})
        @DisplayName("rejects blank input")
        void rejectsBlank(String value) {
            assertThatThrownBy(() -> ObjectId.of(value))
                    .isInstanceOf(InvalidObjectIdException.class)
                    .hasMessageContaining("blank");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "sfs-obj-1-abcd",
                "sfs-obj-00001-abcd",
                "sfs-object-0001-abcd",
                "SFS-OBJ-0001-abcd",
                "sfs-obj-0001-",
                "sfs-obj-abcd-abcd",
                "0001-abcd",
                "sfs-obj-0001-abcd-extra"})
        @DisplayName("rejects a malformed identifier")
        void rejectsMalformed(String value) {
            assertThatThrownBy(() -> ObjectId.of(value))
                    .isInstanceOf(InvalidObjectIdException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "sfs-obj-0001-../../etc/passwd",
                "sfs-obj-0001-abcd/../../secret",
                "../sfs-obj-0001-abcd",
                "sfs-obj-0001-abcd/../..",
                "/etc/passwd",
                "..\\..\\windows\\system32"})
        @DisplayName("rejects path traversal in an identifier")
        void rejectsPathTraversal(String value) {
            assertThatThrownBy(() -> ObjectId.of(value))
                    .isInstanceOf(InvalidObjectIdException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "sfs-obj-0001-abcd'--",
                "sfs-obj-0001-abcd; DROP TABLE files",
                "sfs-obj-0001-<script>alert(1)</script>",
                "sfs-obj-0001-abcd%00",
                "sfs-obj-0001-abcd\n",
                "sfs-obj-0001-abcd\r\nX-Injected: yes"})
        @DisplayName("rejects injection and control characters")
        void rejectsInjectionAttempts(String value) {
            assertThatThrownBy(() -> ObjectId.of(value))
                    .isInstanceOf(InvalidObjectIdException.class);
        }

        @Test
        @DisplayName("rejects an over-long identifier")
        void rejectsOverLongValue() {
            String tooLong = "sfs-obj-0001-" + "a".repeat(200);

            assertThatThrownBy(() -> ObjectId.of(tooLong))
                    .isInstanceOf(InvalidObjectIdException.class);
        }

        @Test
        @DisplayName("reports the expected format without echoing the rejected value")
        void errorMessageDoesNotEchoInput() {
            String hostile = "sfs-obj-0001-<script>alert(1)</script>";

            assertThatThrownBy(() -> ObjectId.of(hostile))
                    .isInstanceOf(InvalidObjectIdException.class)
                    .hasMessageContaining(ObjectId.FORMAT_DESCRIPTION)
                    .hasMessageNotContaining("<script>");
        }
    }

    @Nested
    @DisplayName("validity check")
    class ValidityCheck {

        @Test
        @DisplayName("agrees with construction")
        void agreesWithConstruction() {
            assertThat(ObjectId.isValid(VALID)).isTrue();
            assertThat(ObjectId.isValid("nonsense")).isFalse();
            assertThat(ObjectId.isValid(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("value semantics")
    class ValueSemantics {

        @Test
        @DisplayName("treats equal identifiers as equal")
        void equalIdentifiersAreEqual() {
            assertThat(ObjectId.of(VALID)).isEqualTo(ObjectId.of(VALID));
            assertThat(ObjectId.of(VALID)).hasSameHashCodeAs(ObjectId.of(VALID));
        }

        @Test
        @DisplayName("treats different identifiers as different")
        void differentIdentifiersDiffer() {
            assertThat(ObjectId.of(VALID)).isNotEqualTo(ObjectId.of("sfs-obj-0002-e5f6a7b8"));
        }

        @Test
        @DisplayName("is not equal to its own string form")
        void notEqualToRawString() {
            assertThat(ObjectId.of(VALID)).isNotEqualTo(VALID);
        }

        @Test
        @DisplayName("renders as the identifier itself")
        void rendersAsIdentifier() {
            assertThat(ObjectId.of(VALID)).hasToString(VALID);
        }
    }
}
