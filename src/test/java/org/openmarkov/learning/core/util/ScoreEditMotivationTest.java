/*
 * Copyright (c) CISIAD, UNED, Spain, 2026. Licensed under the GPLv3 licence
 */

package org.openmarkov.learning.core.util;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScoreEditMotivation}: verifies ordering via {@code compareTo},
 * string formatting, score retrieval, and the {@code equals}/{@code hashCode} contract.
 * Includes jqwik property-based tests for antisymmetry and consistency with {@code Double.compare}.
 *
 * @author Manuel Arias
 */
class ScoreEditMotivationTest {

    @Test
    void compareToReturnsNegativeWhenScoreIsSmaller() {
        var low = new ScoreEditMotivation(1.0);
        var high = new ScoreEditMotivation(5.0);

        assertThat(low.compareTo(high)).isNegative();
    }

    @Test
    void compareToReturnsPositiveWhenScoreIsLarger() {
        var high = new ScoreEditMotivation(5.0);
        var low = new ScoreEditMotivation(1.0);

        assertThat(high.compareTo(low)).isPositive();
    }

    @Test
    void compareToReturnsZeroForEqualScores() {
        var a = new ScoreEditMotivation(3.14);
        var b = new ScoreEditMotivation(3.14);

        assertThat(a.compareTo(b)).isZero();
    }

    @Test
    void compareToReturnsZeroWhenComparedToStringEditMotivation() {
        var score = new ScoreEditMotivation(42.0);
        var string = new StringEditMotivation("reason");

        assertThat(score.compareTo(string)).isZero();
    }

    @Test
    void toStringUsesDefaultDecimals() {
        var motivation = new ScoreEditMotivation(3.141592653589793);

        assertThat(motivation.toString()).isEqualTo("3.141592");
    }

    @Test
    void toStringRespectsCustomDecimals() {
        var motivation = new ScoreEditMotivation(3.141592653589793, 2);

        assertThat(motivation.toString()).isEqualTo("3.14");
    }

    @Test
    void getScoreReturnsConstructorValue() {
        var motivation = new ScoreEditMotivation(-7.5);

        assertThat(motivation.getScore()).isEqualTo(-7.5);
    }

    @Test
    void equalScoresAreEqual() {
        var a = new ScoreEditMotivation(3.14);
        var b = new ScoreEditMotivation(3.14);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void differentScoresAreNotEqual() {
        var a = new ScoreEditMotivation(1.0);
        var b = new ScoreEditMotivation(2.0);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void notEqualToOtherTypes() {
        var motivation = new ScoreEditMotivation(1.0);

        assertThat(motivation).isNotEqualTo(null);
        assertThat(motivation).isNotEqualTo("1.0");
        assertThat(motivation).isNotEqualTo(new StringEditMotivation("1.0"));
    }

    @Property
    void compareToIsConsistentWithDoubleCompare(
            @ForAll @DoubleRange(min = -1e6, max = 1e6) double a,
            @ForAll @DoubleRange(min = -1e6, max = 1e6) double b) {
        var ma = new ScoreEditMotivation(a);
        var mb = new ScoreEditMotivation(b);

        assertThat(Integer.signum(ma.compareTo(mb)))
                .isEqualTo(Integer.signum(Double.compare(a, b)));
    }

    @Property
    void compareToIsAntiSymmetric(
            @ForAll @DoubleRange(min = -1e6, max = 1e6) double a,
            @ForAll @DoubleRange(min = -1e6, max = 1e6) double b) {
        var ma = new ScoreEditMotivation(a);
        var mb = new ScoreEditMotivation(b);

        assertThat(Integer.signum(ma.compareTo(mb)))
                .isEqualTo(-Integer.signum(mb.compareTo(ma)));
    }

    @Property
    void equalsIsConsistentWithScore(
            @ForAll @DoubleRange(min = -1e6, max = 1e6) double a,
            @ForAll @DoubleRange(min = -1e6, max = 1e6) double b) {
        var ma = new ScoreEditMotivation(a);
        var mb = new ScoreEditMotivation(b);

        if (Double.compare(a, b) == 0) {
            assertThat(ma).isEqualTo(mb);
            assertThat(ma.hashCode()).isEqualTo(mb.hashCode());
        } else {
            assertThat(ma).isNotEqualTo(mb);
        }
    }
}
