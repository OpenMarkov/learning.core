/*
 * Copyright (c) CISIAD, UNED, Spain,  2026. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.preprocess;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.model.network.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutliersTest {

	CaseDatabase database;

	@BeforeEach public void setUp() {
		// X states "1".."10" plus the extreme "100"; 11 distinct values qualifies as numeric.
		Variable x = new Variable("X", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "100");
		List<Variable> vars = new ArrayList<>();
		vars.add(x);
		int[][] cases = new int[11][1];
		for (int i = 0; i < 11; i++) cases[i][0] = i;
		database = new CaseDatabase(vars, cases);
	}

	@Test public void testIQRRemoveDropsOutlierCase() {
		// IQR bounds with values 1..10,100: Q1=3.5, Q3=8.5, IQR=5 → [-4, 16]. 100 is an outlier.
		Map<String, Outliers.Option> opts = new HashMap<>();
		opts.put("X", Outliers.Option.IQR_REMOVE);

		CaseDatabase result = Outliers.process(database, opts);

		Assertions.assertEquals(10, result.getCases().length);
		// "?" must NOT have been added when removing cases.
		Assertions.assertFalse(result.getVariable("X").containsState("?"));
	}

	@Test public void testIQRMarkMissingReplacesOutlierAndAddsMissingState() {
		Map<String, Outliers.Option> opts = new HashMap<>();
		opts.put("X", Outliers.Option.IQR_MARK_MISSING);

		CaseDatabase result = Outliers.process(database, opts);

		Assertions.assertEquals(11, result.getCases().length);
		Variable newX = result.getVariable("X");
		Assertions.assertTrue(newX.containsState("?"));
		int missingIdx = newX.getStateIndex("?");
		// The outlier case (X=100, index 10) now points to "?".
		Assertions.assertEquals(missingIdx, result.getCases()[10][0]);
		// Non-outlier cases are untouched.
		Assertions.assertEquals(0, result.getCases()[0][0]);
		Assertions.assertEquals(9, result.getCases()[9][0]);
	}

	@Test public void testZScoreRemoveDropsOutlierCase() {
		// With values 1..10 + 100: mean ≈ 14.09, sd ≈ 27.3, bounds ≈ [-67.7, 95.9]. 100 outside.
		Map<String, Outliers.Option> opts = new HashMap<>();
		opts.put("X", Outliers.Option.ZSCORE_REMOVE);

		CaseDatabase result = Outliers.process(database, opts);

		Assertions.assertEquals(10, result.getCases().length);
	}

	@Test public void testNoneKeepsDatabaseUnchanged() {
		Map<String, Outliers.Option> opts = new HashMap<>();
		opts.put("X", Outliers.Option.NONE);

		CaseDatabase result = Outliers.process(database, opts);

		Assertions.assertEquals(11, result.getCases().length);
		Assertions.assertFalse(result.getVariable("X").containsState("?"));
	}

	@Test public void testIQRWinsorizeClampsOutlierToNearestInRangeState() {
		// IQR bounds [-4, 16]; the value 100 is > 16. Nearest in-range max state is "10" (idx 9).
		Map<String, Outliers.Option> opts = new HashMap<>();
		opts.put("X", Outliers.Option.IQR_WINSORIZE);

		CaseDatabase result = Outliers.process(database, opts);

		Assertions.assertEquals(11, result.getCases().length);
		Variable newX = result.getVariable("X");
		Assertions.assertFalse(newX.containsState("?"));
		// Outlier cell (originally index 10 = state "100") winsorized to index 9 = state "10".
		Assertions.assertEquals(9, result.getCases()[10][0]);
		Assertions.assertEquals(9, newX.getStateIndex("10"));
		// Non-outlier rows preserved.
		Assertions.assertEquals(0, result.getCases()[0][0]);
	}

	@Test public void testNonNumericVariableIsIgnored() {
		Variable cat = new Variable("Cat", "a", "b", "c", "d", "e");
		List<Variable> vars = new ArrayList<>();
		vars.add(cat);
		int[][] cases = { { 0 }, { 1 }, { 2 }, { 3 }, { 4 } };
		CaseDatabase db = new CaseDatabase(vars, cases);

		Map<String, Outliers.Option> opts = new HashMap<>();
		opts.put("Cat", Outliers.Option.IQR_REMOVE);

		CaseDatabase result = Outliers.process(db, opts);
		Assertions.assertEquals(5, result.getCases().length);
		Assertions.assertFalse(result.getVariable("Cat").containsState("?"));
	}
}
