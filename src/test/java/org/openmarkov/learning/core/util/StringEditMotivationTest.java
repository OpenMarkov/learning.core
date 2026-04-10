/*
 * Copyright (c) CISIAD, UNED, Spain, 2026. Licensed under the GPLv3 licence
 */

package org.openmarkov.learning.core.util;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringEditMotivation}: verifies {@code toString}, {@code compareTo},
 * and the {@code equals}/{@code hashCode} contract.
 * Includes a jqwik property-based test for {@code toString} round-tripping.
 *
 * @author Manuel Arias
 */
class StringEditMotivationTest {

    @Test
    void toStringReturnsMotivationText() {
        var motivation = new StringEditMotivation("independence test p=0.03");

        assertThat(motivation.toString()).isEqualTo("independence test p=0.03");
    }

    @Test
    void compareToAlwaysReturnsZero() {
        var a = new StringEditMotivation("reason A");
        var b = new StringEditMotivation("reason B");

        assertThat(a.compareTo(b)).isZero();
        assertThat(b.compareTo(a)).isZero();
    }

    @Test
    void compareToReturnsZeroWhenComparedToScoreMotivation() {
        var string = new StringEditMotivation("reason");
        var score = new ScoreEditMotivation(42.0);

        assertThat(string.compareTo(score)).isZero();
    }

    @Test
    void equalMotivationsAreEqual() {
        var a = new StringEditMotivation("same reason");
        var b = new StringEditMotivation("same reason");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void differentMotivationsAreNotEqual() {
        var a = new StringEditMotivation("reason A");
        var b = new StringEditMotivation("reason B");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void notEqualToOtherTypes() {
        var motivation = new StringEditMotivation("reason");

        assertThat(motivation).isNotEqualTo(null);
        assertThat(motivation).isNotEqualTo("reason");
        assertThat(motivation).isNotEqualTo(new ScoreEditMotivation(0.0));
    }

    @Property
    void toStringAlwaysReturnsConstructorArgument(@ForAll String text) {
        var motivation = new StringEditMotivation(text);

        assertThat(motivation.toString()).isEqualTo(text);
    }
}
