/*
 * Copyright (c) CISIAD, UNED, Spain,  2018. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.preprocess;

import org.junit.jupiter.api.*;
import org.openmarkov.core.io.database.CaseDatabase;
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

}
