/*
 * Copyright (c) CISIAD, UNED, Spain, 2026. Licensed under the GPLv3 licence
 */

package org.openmarkov.learning.core.algorithm;

import org.junit.jupiter.api.Test;
import org.openmarkov.core.exception.InvalidArgumentException;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.learning.core.DummyLearningAlgorithm;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link LearningAlgorithmManager}: verifies singleton initialization,
 * plugin discovery of annotated algorithms, annotation retrieval via {@code info()},
 * reflection-based instantiation, and error handling for invalid parameters.
 *
 * @author Manuel Arias
 */
class LearningAlgorithmManagerTest {

    @Test
    void singletonInstanceIsNotNull() {
        assertThat(LearningAlgorithmManager.INSTANCE).isNotNull();
    }

    @Test
    void findsDummyAlgorithmViaPluginDiscovery() {
        boolean found = LearningAlgorithmManager.INSTANCE.getLearningAlgorithms()
                .anyMatch(clazz -> clazz.equals(DummyLearningAlgorithm.class));

        assertThat(found).isTrue();
    }

    @Test
    void infoReturnAnnotationForDummy() {
        LearningAlgorithmType info = LearningAlgorithmManager.info(DummyLearningAlgorithm.class);

        assertThat(info).isNotNull();
        assertThat(info.name()).isEqualTo("Dummy");
        assertThat(info.discriminative()).isFalse();
        assertThat(info.supportsUnobservedVariables()).isFalse();
    }

    @Test
    void instantiateByClassCreatesDummyAlgorithm() {
        ProbNet probNet = new ProbNet();
        Variable vA = new Variable("A", "0", "1");
        probNet.addNode(vA, NodeType.CHANCE);
        CaseDatabase db = new CaseDatabase(List.of(vA), new int[][]{{0}});

        LearningAlgorithm instance = LearningAlgorithmManager.INSTANCE
                .instantiateByClass(DummyLearningAlgorithm.class, List.of(probNet, db));

        assertThat(instance).isInstanceOf(DummyLearningAlgorithm.class);
    }

    @Test
    void instantiateByClassThrowsForWrongParameterCount() {
        assertThatThrownBy(() ->
                LearningAlgorithmManager.INSTANCE.instantiateByClass(
                        DummyLearningAlgorithm.class, List.of("wrong")))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void instantiateByClassThrowsForNoParameters() {
        assertThatThrownBy(() ->
                LearningAlgorithmManager.INSTANCE.instantiateByClass(
                        DummyLearningAlgorithm.class, List.of()))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void getLearningAlgorithmsReturnsNonEmptyStream() {
        long count = LearningAlgorithmManager.INSTANCE.getLearningAlgorithms().count();

        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void allDiscoveredAlgorithmsHaveAnnotation() {
        LearningAlgorithmManager.INSTANCE.getLearningAlgorithms().forEach(clazz -> {
            LearningAlgorithmType annotation = clazz.getAnnotation(LearningAlgorithmType.class);
            assertThat(annotation)
                    .as("Algorithm %s should have @LearningAlgorithmType", clazz.getSimpleName())
                    .isNotNull();
        });
    }
}
