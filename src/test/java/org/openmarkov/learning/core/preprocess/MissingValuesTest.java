/*
 * Copyright (c) CISIAD, UNED, Spain,  2018. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.preprocess;

import org.junit.jupiter.api.*;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.testTags.TestSpeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class MissingValuesTest {

	CaseDatabase database = null;
	Variable varA, varB;

	@BeforeEach public void setUp() {
		List<Variable> variables = new ArrayList<>();
		varA = new Variable("A", "a1", "?", "a0");
		varB = new Variable("B", "b0", "b1");
		variables.add(varA);
		variables.add(varB);

		int[][] cases = { { 0, 0 }, { 0, 1 }, { 1, 1 }, { 2, 0 }, { 1, 0 } };

		database = new CaseDatabase(variables, cases);
	}
	
	@Tag(TestSpeed.MEDIUM)
	@Test public void testKeepMissingValues() {
		Map<String, MissingValues.Option> preprocessOption = new HashMap<>();
		preprocessOption.put("A", MissingValues.Option.KEEP);
		preprocessOption.put("B", MissingValues.Option.KEEP);

		CaseDatabase newDatabase = MissingValues.process(database, preprocessOption);

		Assertions.assertEquals(5, newDatabase.getCases().length);
		Assertions.assertEquals(3, newDatabase.getVariables().get(0).getStates().length);

	}

	@Test public void testRemoveMissingValues() {
		Map<String, MissingValues.Option> preprocessOption = new HashMap<>();
		preprocessOption.put("A", MissingValues.Option.ELIMINATE);
		preprocessOption.put("B", MissingValues.Option.ELIMINATE);
		CaseDatabase newDatabase = MissingValues.process(database, preprocessOption);

		Assertions.assertEquals(3, newDatabase.getCases().length);
		// When eliminating the records with missing values, delete missing state
		Assertions.assertEquals(2, newDatabase.getVariables().get(0).getStates().length);
		// When eliminating the records with missing values, update indexes of cases to match the new states
		Assertions.assertEquals(1, newDatabase.getCases()[2][0]);
		Assertions.assertEquals(0, newDatabase.getCases()[0][0]);

	}

	@Test public void testImputeModeCategorical() {
		// varA has states {a1 (idx 0), ? (idx 1), a0 (idx 2)}; cases: a1, a1, ?, a0, ?
		// Mode of A excluding ? is a1 (count 2 vs a0 count 1).
		Map<String, MissingValues.Option> preprocessOption = new HashMap<>();
		preprocessOption.put("A", MissingValues.Option.IMPUTE_MODE);
		preprocessOption.put("B", MissingValues.Option.KEEP);

		CaseDatabase newDatabase = MissingValues.process(database, preprocessOption);

		// All cases are kept.
		Assertions.assertEquals(5, newDatabase.getCases().length);
		// "?" is removed from variable A.
		Assertions.assertEquals(2, newDatabase.getVariables().get(0).getStates().length);
		Assertions.assertFalse(newDatabase.getVariables().get(0).containsState("?"));

		int[][] cases = newDatabase.getCases();
		// Imputed rows (originally A="?") now point to a1 (new idx 0).
		Assertions.assertEquals(0, cases[2][0]);
		Assertions.assertEquals(0, cases[4][0]);
		// Originally a1 (old idx 0) stays at new idx 0.
		Assertions.assertEquals(0, cases[0][0]);
		// Originally a0 (old idx 2) shifts to new idx 1.
		Assertions.assertEquals(1, cases[3][0]);
	}

	@Test public void testImputeMeanNumeric() {
		// Numeric variable C with explicit missing state. isNumeric requires
		// at least 5 states when "?" is present.
		Variable varC = new Variable("C", "1.0", "2.0", "3.0", "10.0", "?");
		Variable varD = new Variable("D", "d0", "d1");
		List<Variable> variables = new ArrayList<>();
		variables.add(varC);
		variables.add(varD);
		// Mean of observed values (1, 2, 3, 10) = 4 → nearest existing state = "3.0" (idx 2).
		int[][] cases = { { 0, 0 }, { 1, 1 }, { 2, 0 }, { 3, 1 }, { 4, 0 } };
		CaseDatabase numericDb = new CaseDatabase(variables, cases);

		Map<String, MissingValues.Option> preprocessOption = new HashMap<>();
		preprocessOption.put("C", MissingValues.Option.IMPUTE_MEAN);
		preprocessOption.put("D", MissingValues.Option.KEEP);

		CaseDatabase result = MissingValues.process(numericDb, preprocessOption);

		Assertions.assertEquals(5, result.getCases().length);
		Assertions.assertEquals(4, result.getVariables().get(0).getStates().length);
		// The "?" row (original C index 4) is replaced with the index of "3.0" (now idx 2).
		Assertions.assertEquals(2, result.getCases()[4][0]);
	}

	@Test public void testImputeMedianNumeric() {
		Variable varC = new Variable("C", "1.0", "2.0", "3.0", "100.0", "?");
		Variable varD = new Variable("D", "d0", "d1");
		List<Variable> variables = new ArrayList<>();
		variables.add(varC);
		variables.add(varD);
		// Observed values (1, 2, 3, 100); median (per cumulative > total/2 = 2) = 3 → state "3.0" (idx 2).
		int[][] cases = { { 0, 0 }, { 1, 1 }, { 2, 0 }, { 3, 1 }, { 4, 0 } };
		CaseDatabase numericDb = new CaseDatabase(variables, cases);

		Map<String, MissingValues.Option> preprocessOption = new HashMap<>();
		preprocessOption.put("C", MissingValues.Option.IMPUTE_MEDIAN);
		preprocessOption.put("D", MissingValues.Option.KEEP);

		CaseDatabase result = MissingValues.process(numericDb, preprocessOption);

		Assertions.assertEquals(2, result.getCases()[4][0]);
	}

}
