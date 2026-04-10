/*
 * Copyright (c) CISIAD, UNED, Spain, 2026. Licensed under the GPLv3 licence
 */

package org.openmarkov.learning.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ModelNetUse}: verifies the default constructor, the override logic
 * that forces {@code useModelNet} to {@code false} when neither node positions nor
 * start-from-model-net are enabled, and that link operation flags are preserved.
 *
 * @author Manuel Arias
 */
class ModelNetUseTest {

    @Test
    void defaultConstructorDisablesEverything() {
        var modelNetUse = new ModelNetUse();

        assertThat(modelNetUse.isUseModelNet()).isFalse();
        assertThat(modelNetUse.isUseNodePositions()).isFalse();
        assertThat(modelNetUse.isStartFromModelNet()).isFalse();
        assertThat(modelNetUse.isLinkAdditionAllowed()).isFalse();
        assertThat(modelNetUse.isLinkRemovalAllowed()).isFalse();
        assertThat(modelNetUse.isLinkInversionAllowed()).isFalse();
    }

    @Test
    void useModelNetIsForcedFalseWhenNeitherPositionsNorStartFromModelNet() {
        var modelNetUse = new ModelNetUse(true, false, false, true, true, true);

        assertThat(modelNetUse.isUseModelNet()).isFalse();
    }

    @Test
    void useModelNetIsTrueWhenUseNodePositionsIsTrue() {
        var modelNetUse = new ModelNetUse(true, true, false, false, false, false);

        assertThat(modelNetUse.isUseModelNet()).isTrue();
        assertThat(modelNetUse.isUseNodePositions()).isTrue();
        assertThat(modelNetUse.isStartFromModelNet()).isFalse();
    }

    @Test
    void useModelNetIsTrueWhenStartFromModelNetIsTrue() {
        var modelNetUse = new ModelNetUse(true, false, true, true, true, true);

        assertThat(modelNetUse.isUseModelNet()).isTrue();
        assertThat(modelNetUse.isStartFromModelNet()).isTrue();
    }

    @Test
    void linkOperationFlagsArePreserved() {
        var modelNetUse = new ModelNetUse(true, false, true, true, false, true);

        assertThat(modelNetUse.isLinkAdditionAllowed()).isTrue();
        assertThat(modelNetUse.isLinkRemovalAllowed()).isFalse();
        assertThat(modelNetUse.isLinkInversionAllowed()).isTrue();
    }

    @Test
    void useModelNetIsFalseWhenExplicitlyDisabledEvenWithPositions() {
        var modelNetUse = new ModelNetUse(false, true, false, false, false, false);

        assertThat(modelNetUse.isUseModelNet()).isFalse();
    }
}
