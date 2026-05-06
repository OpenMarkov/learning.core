/*
 * Copyright (c) CISIAD, UNED, Spain, 2026. Licensed under the GPLv3 licence
 */

package org.openmarkov.learning.core.preprocess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.model.network.Variable;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FilterDatabaseTest {

    private CaseDatabase originalDatabase;
    private Variable vA;
    private Variable vB;
    private Variable vC;

    @BeforeEach
    public void setUp() {
        vA = new Variable("A", "0", "1");
        vB = new Variable("B", "0", "1");
        vC = new Variable("C", "0", "1");

        List<Variable> variables = Arrays.asList(vA, vB, vC);

        // Cases: (A, B, C)
        // 0, 0, 0
        // 1, 0, 1
        // 0, 1, 0
        int[][] cases = {
                { 0, 0, 0 },
                { 1, 0, 1 },
                { 0, 1, 0 }
        };

        originalDatabase = new CaseDatabase(variables, cases);
    }

    @Test
    public void testFilterSubset() {
        List<Variable> selected = Arrays.asList(vA, vC);
        CaseDatabase filtered = FilterDatabase.filter(originalDatabase, selected);

        assertEquals(2, filtered.getVariables().size());
        assertEquals("A", filtered.getVariables().get(0).getName());
        assertEquals("C", filtered.getVariables().get(1).getName());

        int[][] fCases = filtered.getCases();
        assertEquals(3, fCases.length);

        // Check content
        // Should be (A, C) -> (0,0), (1,1), (0,0)
        assertArrayEquals(new int[] { 0, 0 }, fCases[0]);
        assertArrayEquals(new int[] { 1, 1 }, fCases[1]);
        assertArrayEquals(new int[] { 0, 0 }, fCases[2]);
    }

    @Test
    public void testFilterReorder() {
        // Filter and reorder: C, B
        List<Variable> selected = Arrays.asList(vC, vB);
        CaseDatabase filtered = FilterDatabase.filter(originalDatabase, selected);

        assertEquals(2, filtered.getVariables().size());
        assertEquals("C", filtered.getVariables().get(0).getName());
        assertEquals("B", filtered.getVariables().get(1).getName());

        int[][] fCases = filtered.getCases();
        // Should be (C, B) -> (0,0), (1,0), (0,1)
        assertArrayEquals(new int[] { 0, 0 }, fCases[0]);
        assertArrayEquals(new int[] { 1, 0 }, fCases[1]);
        assertArrayEquals(new int[] { 0, 1 }, fCases[2]);
    }

    @Test
    public void testFilterAll() {
        List<Variable> selected = Arrays.asList(vA, vB, vC);
        CaseDatabase filtered = FilterDatabase.filter(originalDatabase, selected);

        assertEquals(3, filtered.getVariables().size());
        int[][] fCases = filtered.getCases();
        assertArrayEquals(originalDatabase.getCases(), fCases);
    }
}
