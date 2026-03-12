/*
 * Copyright (c) CISIAD, UNED, Spain, 2026. Licensed under the GPLv3 licence
 */

package org.openmarkov.learning.core.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmarkov.core.io.database.CaseDatabase;
import org.openmarkov.core.model.network.*;
import org.openmarkov.core.model.network.potential.TablePotential;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UtilTest {

    private ProbNet probNet;
    private CaseDatabase caseDatabase;
    private Node nodeA;
    private Node nodeB;

    @BeforeEach
    public void setUp() {
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
        // Counts:
        // A=0: 3, A=1: 2
        // B=0: 3, B=1: 2
        // (A=0, B=0): 2
        // (A=0, B=1): 1
        // (A=1, B=0): 1
        // (A=1, B=1): 1
        int[][] cases = {
                { 0, 0 },
                { 0, 1 },
                { 1, 0 },
                { 1, 1 },
                { 0, 0 }
        };

        caseDatabase = new CaseDatabase(variables, cases);
    }

    @Test
    public void testGetAbsoluteFrequenciesNoParents() {
        // Node A has no parents
        TablePotential freq = Util.getAbsoluteFreq(probNet, caseDatabase, nodeA);

        // Expecting [3.0, 2.0]
        double[] values = freq.getValues();
        assertEquals(3.0, values[0], 0.001);
        assertEquals(2.0, values[1], 0.001);
    }

    @Test
    public void testGetAbsoluteFrequenciesWithParent() {
        // Add link A -> B
        probNet.addLink(nodeA, nodeB, true);

        // Node B has parent A
        TablePotential freq = Util.getAbsoluteFreq(probNet, caseDatabase, nodeB);

        // Expected structure is [B|A=0, B|A=1] (depending on variable order in
        // Potential)
        // Util.getAbsoluteFrequencies puts childNode as first in list, then parents.
        // But TablePotential usually stores values in order of variables.
        // Let's check variables in potential
        // List<Variable> vars = freq.getVariables();
        // vars should contain B and A.

        // getAbsoluteFrequencies implementation:
        // parentsConfigurations *= ...
        // It iterates parents first?
        // Let's verify standard potential indexing in OpenMarkov:
        // usually strides are: first variable varies fastest (or last? need to check).
        // OpenMarkov standard: Last variable varies fastest (Little Endian vs Big
        // Endian).
        // Actually, let's just check the values.
        // We expect (A=0, B=0)=2, (A=0, B=1)=1, (A=1, B=0)=1, (A=1, B=1)=1

        double[] values = freq.getValues();
        // Sum should be 5
        double sum = 0;
        for (double v : values)
            sum += v;
        assertEquals(5.0, sum, 0.001);

        // We can check specific configurations if we knew the order.
        // Assuming strictly correctness of counts first:
        // There should be one '2.0' and three '1.0's.
        int count2 = 0;
        int count1 = 0;
        for (double v : values) {
            if (Math.abs(v - 2.0) < 0.001)
                count2++;
            if (Math.abs(v - 1.0) < 0.001)
                count1++;
        }
        assertEquals(1, count2, "Should have one configuration with count 2");
        assertEquals(3, count1, "Should have three configurations with count 1");
    }

    @Test
    public void testGetAbsoluteFreqExtraParent() {
        // Node B, extra parent A (not linked in graph)
        TablePotential freq = Util.getAbsoluteFreqExtraParent(probNet, caseDatabase, nodeB, nodeA);

        double[] values = freq.getValues();
        // Same expectations as previous test: one 2.0, three 1.0s
        double sum = 0;
        for (double v : values)
            sum += v;
        assertEquals(5.0, sum, 0.001);

        int count2 = 0;
        int count1 = 0;
        for (double v : values) {
            if (Math.abs(v - 2.0) < 0.001)
                count2++;
            if (Math.abs(v - 1.0) < 0.001)
                count1++;
        }
        assertEquals(1, count2);
        assertEquals(3, count1);
    }
}
