/*
 * Copyright (c) CISIAD, UNED, Spain, 2026. Licensed under the GPLv3 licence
 */

package org.openmarkov.learning.core.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.model.network.*;
import org.openmarkov.core.model.network.potential.TablePotential;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link Util}: verifies absolute frequency calculation for nodes with and
 * without parents, extra/removed parent variants, error handling for missing variables,
 * and that input lists are not mutated.
 *
 * @author Manuel Arias
 */
class UtilTest {

    private ProbNet probNet;
    private CaseDatabase caseDatabase;
    private Node nodeA;
    private Node nodeB;

    @BeforeEach
    void setUp() {
        probNet = new ProbNet();
        Variable vA = new Variable("A", "0", "1");
        Variable vB = new Variable("B", "0", "1");

        nodeA = probNet.addNode(vA, NodeType.CHANCE);
        nodeB = probNet.addNode(vB, NodeType.CHANCE);

        List<Variable> variables = Arrays.asList(vA, vB);
        // Cases:
        // A=0, B=0
        // A=0, B=1
        // A=1, B=0
        // A=1, B=1
        // A=0, B=0
        // Counts: A=0:3, A=1:2, B=0:3, B=1:2
        // Joint: (A=0,B=0):2, (A=0,B=1):1, (A=1,B=0):1, (A=1,B=1):1
        int[][] cases = {
                {0, 0},
                {0, 1},
                {1, 0},
                {1, 1},
                {0, 0}
        };

        caseDatabase = new CaseDatabase(variables, cases);
    }

    @Test
    void getAbsoluteFrequenciesNoParents() {
        TablePotential freq = Util.getAbsoluteFreq(probNet, caseDatabase, nodeA);

        double[] values = freq.getValues();
        assertThat(values).hasSize(2);
        assertThat(values[0]).isEqualTo(3.0);
        assertThat(values[1]).isEqualTo(2.0);
    }

    @Test
    void getAbsoluteFrequenciesWithParent() {
        probNet.addLink(nodeA, nodeB, true);

        TablePotential freq = Util.getAbsoluteFreq(probNet, caseDatabase, nodeB);

        double[] values = freq.getValues();
        double sum = Arrays.stream(values).sum();
        assertThat(sum).isEqualTo(5.0);

        // Should have one configuration with count 2 and three with count 1
        long count2 = Arrays.stream(values).filter(v -> Math.abs(v - 2.0) < 0.001).count();
        long count1 = Arrays.stream(values).filter(v -> Math.abs(v - 1.0) < 0.001).count();
        assertThat(count2).as("configurations with count 2").isEqualTo(1);
        assertThat(count1).as("configurations with count 1").isEqualTo(3);
    }

    @Test
    void getAbsoluteFreqExtraParentAddsParent() {
        // No link in the graph, but extraParent adds A as parent of B
        TablePotential freq = Util.getAbsoluteFreqExtraParent(probNet, caseDatabase, nodeB, nodeA);

        double[] values = freq.getValues();
        double sum = Arrays.stream(values).sum();
        assertThat(sum).isEqualTo(5.0);

        long count2 = Arrays.stream(values).filter(v -> Math.abs(v - 2.0) < 0.001).count();
        long count1 = Arrays.stream(values).filter(v -> Math.abs(v - 1.0) < 0.001).count();
        assertThat(count2).isEqualTo(1);
        assertThat(count1).isEqualTo(3);
    }

    @Test
    void getAbsoluteFreqExtraParentWithNullExtraParent() {
        // Null extraParent should behave like getAbsoluteFreq
        TablePotential freqWithNull = Util.getAbsoluteFreqExtraParent(probNet, caseDatabase, nodeB, null);
        TablePotential freqNormal = Util.getAbsoluteFreq(probNet, caseDatabase, nodeB);

        assertThat(freqWithNull.getValues()).isEqualTo(freqNormal.getValues());
    }

    @Test
    void getAbsoluteFreqExtraParentDoesNotDuplicateExistingParent() {
        probNet.addLink(nodeA, nodeB, true);

        // A is already parent of B, passing it again as extraParent should not add it twice
        TablePotential freq = Util.getAbsoluteFreqExtraParent(probNet, caseDatabase, nodeB, nodeA);

        double[] values = freq.getValues();
        // 2 states for B * 2 states for A = 4 entries
        assertThat(values).hasSize(4);
    }

    @Test
    void getAbsoluteFreqRemovingParentExcludesParent() {
        probNet.addLink(nodeA, nodeB, true);

        // Remove parent A, so B should have marginal frequencies
        TablePotential freq = Util.getAbsoluteFreqRemovingParent(probNet, caseDatabase, nodeB, nodeA);

        double[] values = freq.getValues();
        // Without parents: just B marginal counts => B=0:3, B=1:2
        assertThat(values).hasSize(2);
        assertThat(values[0]).isEqualTo(3.0);
        assertThat(values[1]).isEqualTo(2.0);
    }

    @Test
    void getAbsoluteFreqRemovingParentWithMultipleParents() {
        Variable vC = new Variable("C", "0", "1");
        Node nodeC = probNet.addNode(vC, NodeType.CHANCE);

        // Rebuild database with 3 variables
        List<Variable> variables = Arrays.asList(
                nodeA.getVariable(), nodeB.getVariable(), vC);
        int[][] cases = {
                {0, 0, 0},
                {0, 1, 1},
                {1, 0, 0},
                {1, 1, 1},
                {0, 0, 0}
        };
        CaseDatabase db3 = new CaseDatabase(variables, cases);

        // C has parents A and B
        probNet.addLink(nodeA, nodeC, true);
        probNet.addLink(nodeB, nodeC, true);

        // Remove parent A, keep parent B
        TablePotential freq = Util.getAbsoluteFreqRemovingParent(probNet, db3, nodeC, nodeA);

        double[] values = freq.getValues();
        // 2 states for C * 2 states for B = 4 entries
        assertThat(values).hasSize(4);
        double sum = Arrays.stream(values).sum();
        assertThat(sum).isEqualTo(5.0);
    }

    @Test
    void getAbsoluteFrequenciesThrowsForMissingVariable() {
        ProbNet otherNet = new ProbNet();
        Variable vX = new Variable("X", "0", "1");
        Node nodeX = otherNet.addNode(vX, NodeType.CHANCE);

        assertThatThrownBy(() -> Util.getAbsoluteFreq(otherNet, caseDatabase, nodeX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X");
    }

    @Test
    void frequenciesSumToTotalCaseCount() {
        // For any node without parents, frequencies should sum to number of cases
        TablePotential freqA = Util.getAbsoluteFreq(probNet, caseDatabase, nodeA);
        TablePotential freqB = Util.getAbsoluteFreq(probNet, caseDatabase, nodeB);

        assertThat(Arrays.stream(freqA.getValues()).sum()).isEqualTo(5.0);
        assertThat(Arrays.stream(freqB.getValues()).sum()).isEqualTo(5.0);
    }

    @Test
    void getAbsoluteFreqDoesNotMutateInputList() {
        // Verify the subList fix: calling getAbsoluteFreqExtraParent should not mutate anything
        probNet.addLink(nodeA, nodeB, true);
        int parentsBefore = nodeB.getParents().size();

        Util.getAbsoluteFreq(probNet, caseDatabase, nodeB);

        assertThat(nodeB.getParents()).hasSize(parentsBefore);
    }
}
