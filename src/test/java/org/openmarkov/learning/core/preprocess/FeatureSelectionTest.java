/*
 * Copyright (c) CISIAD, UNED, Spain,  2026. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.preprocess;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.model.network.Variable;

import java.util.ArrayList;
import java.util.List;

public class FeatureSelectionTest {

	/**
	 * Builds a 4-variable dataset where:
	 * - C is the class (2 states),
	 * - GOOD is perfectly correlated with C (same value),
	 * - WEAK is mildly correlated (matches C in 60% of rows),
	 * - NOISE is independent of C.
	 */
	private CaseDatabase syntheticDB() {
		Variable c = new Variable("C", "c0", "c1");
		Variable good = new Variable("GOOD", "g0", "g1");
		Variable weak = new Variable("WEAK", "w0", "w1");
		Variable noise = new Variable("NOISE", "n0", "n1");
		List<Variable> vars = new ArrayList<>();
		vars.add(c); vars.add(good); vars.add(weak); vars.add(noise);
		int[][] cases = new int[20][4];
		for (int i = 0; i < 20; i++) {
			int cls = i % 2;
			cases[i][0] = cls;
			cases[i][1] = cls; // perfect
			cases[i][2] = (i % 5 == 0) ? 1 - cls : cls; // ~80% agreement
			cases[i][3] = (i / 4) % 2; // independent
		}
		return new CaseDatabase(vars, cases);
	}

	@Test public void testMutualInformationPicksCorrelatedFeature() {
		CaseDatabase db = syntheticDB();
		Variable c = db.getVariable("C");
		CaseDatabase result = FeatureSelection.select(db, c, FeatureSelection.Method.MUTUAL_INFORMATION, 1);
		List<Variable> vars = result.getVariables();
		Assertions.assertEquals(2, vars.size());
		Assertions.assertNotNull(result.getVariable("C"));
		Assertions.assertNotNull(result.getVariable("GOOD"));
		Assertions.assertNull(result.getVariable("NOISE"));
	}

	@Test public void testMutualInformationTopTwoIncludesWeak() {
		CaseDatabase db = syntheticDB();
		CaseDatabase result = FeatureSelection.select(db, db.getVariable("C"),
				FeatureSelection.Method.MUTUAL_INFORMATION, 2);
		Assertions.assertEquals(3, result.getVariables().size());
		Assertions.assertNotNull(result.getVariable("GOOD"));
		Assertions.assertNotNull(result.getVariable("WEAK"));
	}

	@Test public void testChiSquaredPicksCorrelatedFeature() {
		CaseDatabase db = syntheticDB();
		CaseDatabase result = FeatureSelection.select(db, db.getVariable("C"),
				FeatureSelection.Method.CHI_SQUARED, 1);
		Assertions.assertEquals(2, result.getVariables().size());
		Assertions.assertNotNull(result.getVariable("GOOD"));
	}

	@Test public void testIAMBKeepsCorrelatedFeatureDropsNoise() {
		CaseDatabase db = syntheticDB();
		CaseDatabase result = FeatureSelection.select(db, db.getVariable("C"),
				FeatureSelection.Method.MARKOV_BLANKET_IAMB, 0);
		Assertions.assertNotNull(result.getVariable("C"));
		Assertions.assertNotNull(result.getVariable("GOOD"));
		Assertions.assertNull(result.getVariable("NOISE"));
	}

	@Test public void testNoneReturnsDatabaseUnchanged() {
		CaseDatabase db = syntheticDB();
		CaseDatabase result = FeatureSelection.select(db, db.getVariable("C"),
				FeatureSelection.Method.NONE, 0);
		Assertions.assertSame(db, result);
	}

	@Test public void testThrowsWithoutClassVariable() {
		CaseDatabase db = syntheticDB();
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> FeatureSelection.select(db, null, FeatureSelection.Method.MUTUAL_INFORMATION, 1));
	}
}
