/*
 * Copyright (c) CISIAD, UNED, Spain,  2018. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.preprocess;

import org.junit.jupiter.api.*;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.testTags.TestSpeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class DiscretizationTests {
	CaseDatabase database = null;
	Variable varA, varB;

	@BeforeEach public void setUp() {
		List<Variable> variables = new ArrayList<>();
		varA = new Variable("A", "3", "?", "2", "4", "7", "5");
		varB = new Variable("B", "2.6", "1.0", "-3.2", "0.01", "0.6", "1.2");
		variables.add(varA);
		variables.add(varB);

		int[][] cases = { { 0, 0 }, { 0, 1 }, { 1, 1 }, { 2, 3 }, { 3, 0 }, { 3, 4 }, { 4, 1 }, { 5, 5 } };

		database = new CaseDatabase(variables, cases);
	}

	@Test public void testNoDiscretize() {
		Map<String, Discretization.Option> discretizeOptions = new HashMap<>();
		discretizeOptions.put("A", Discretization.Option.NONE);
		discretizeOptions.put("B", Discretization.Option.NONE);

		Map<String, Integer> numIntervalsPerVariable = new HashMap<>();
		numIntervalsPerVariable.put("A", 5000);
		numIntervalsPerVariable.put("B", 1);

		CaseDatabase newDatabase = Discretization.process(database, discretizeOptions, numIntervalsPerVariable);

		Assertions.assertEquals(database.getVariables(), newDatabase.getVariables());
		Assertions.assertEquals(database.getCases().length, newDatabase.getCases().length);
	}

	@Test public void testDiscretizeEqualWidth() {
		Map<String, Discretization.Option> discretizeOptions = new HashMap<>();
		discretizeOptions.put("A", Discretization.Option.EQUAL_WIDTH);
		discretizeOptions.put("B", Discretization.Option.EQUAL_WIDTH);

		Map<String, Integer> numIntervalsPerVariable = new HashMap<>();
		numIntervalsPerVariable.put("A", 3);
		numIntervalsPerVariable.put("B", 4);

		CaseDatabase newDatabase = Discretization.process(database, discretizeOptions, numIntervalsPerVariable);

		Variable newVarA = newDatabase.getVariable("A");
		Variable newVarB = newDatabase.getVariable("B");
		Assertions.assertEquals(4, newVarA.getStates().length);
		Assertions.assertEquals(4, newVarB.getStates().length);
		Assertions.assertEquals("[2.0 , 3.666666666666667]", newVarA.getStates()[0].getName());
		Assertions.assertEquals("[-3.2 , -1.75]", newVarB.getStates()[0].getName());
		Assertions.assertEquals("?", newVarA.getStates()[3].getName());
		Assertions.assertEquals(0, newDatabase.getCases()[0][0]);
		Assertions.assertEquals(3, newDatabase.getCases()[0][1]);
		Assertions.assertEquals(1, newDatabase.getCases()[7][0]);
		Assertions.assertEquals(3, newDatabase.getCases()[7][1]);

	}

	@Test public void testDiscretizeEqualFreq() {
		Map<String, Discretization.Option> discretizeOptions = new HashMap<>();
		discretizeOptions.put("A", Discretization.Option.EQUAL_FREQ);
		discretizeOptions.put("B", Discretization.Option.EQUAL_FREQ);

		Map<String, Integer> numIntervalsPerVariable = new HashMap<>();
		numIntervalsPerVariable.put("A", 4);
		numIntervalsPerVariable.put("B", 2);

		CaseDatabase newDatabase = Discretization.process(database, discretizeOptions, numIntervalsPerVariable);

		Variable newVarA = newDatabase.getVariable("A");
		Variable newVarB = newDatabase.getVariable("B");
		Assertions.assertEquals(5, newVarA.getStates().length);
		Assertions.assertEquals(2, newVarB.getStates().length);
		Assertions.assertEquals("(-Infinity , 3.0]", newVarA.getStates()[0].getName());
		Assertions.assertEquals("(7.0 , Infinity)", newVarA.getStates()[3].getName());
		Assertions.assertEquals("(-Infinity , 1.0]", newVarB.getStates()[0].getName());
		Assertions.assertEquals("?", newVarA.getStates()[4].getName());
		int[][] newCases = newDatabase.getCases();
		Assertions.assertEquals(0, newCases[0][0]);
		Assertions.assertEquals(1, newCases[0][1]);
		Assertions.assertEquals(2, newCases[7][0]);
		Assertions.assertEquals(1, newCases[7][1]);
	}

	/**
	 * Regression for B-Discretize (too few cut points). A skewed distribution
	 * (values 1,2,3 with counts 98,1,1) yields fewer cut points than
	 * {@code numIntervals-1}, which used to overrun {@code intervalLimits} with an
	 * {@code IndexOutOfBoundsException}. The result must instead honour the cuts
	 * actually produced: the dominant value gets its own interval and the rest
	 * collapse into one, giving two intervals.
	 */
	@Test public void testDiscretizeEqualFreqSkewedDoesNotCrash() {
		Variable x = new Variable("X", "1", "2", "3");
		int[][] cases = new int[100][1];
		for (int i = 0; i < 98; i++) cases[i][0] = 0;
		cases[98][0] = 1;
		cases[99][0] = 2;
		CaseDatabase db = new CaseDatabase(List.of(x), cases);

		Map<String, Discretization.Option> discretizeOptions = new HashMap<>();
		discretizeOptions.put("X", Discretization.Option.EQUAL_FREQ);
		Map<String, Integer> numIntervalsPerVariable = new HashMap<>();
		numIntervalsPerVariable.put("X", 3);

		CaseDatabase newDatabase = Discretization.process(db, discretizeOptions, numIntervalsPerVariable);
		Variable newVarX = newDatabase.getVariable("X");
		Assertions.assertEquals(2, newVarX.getStates().length);
		Assertions.assertEquals("(-Infinity , 1.0]", newVarX.getStates()[0].getName());
		Assertions.assertEquals("(1.0 , Infinity)", newVarX.getStates()[1].getName());
	}

	/**
	 * Regression for B-Discretize (too many cut points). When the split produces
	 * more boundaries than {@code numIntervals} (values 1,2,3,4 uniformly, asking
	 * for 2 intervals), the trailing interval used to be dropped and the last bound
	 * clamped to a finite value instead of {@code +Infinity}. The natural boundaries
	 * are 2.0 and 4.0, so three intervals must survive with the last open to infinity.
	 */
	@Test public void testDiscretizeEqualFreqDoesNotDropTail() {
		Variable x = new Variable("X", "1", "2", "3", "4");
		int[][] cases = new int[120][1];
		int row = 0;
		for (int s = 0; s < 4; s++)
			for (int c = 0; c < 30; c++) cases[row++][0] = s;
		CaseDatabase db = new CaseDatabase(List.of(x), cases);

		Map<String, Discretization.Option> discretizeOptions = new HashMap<>();
		discretizeOptions.put("X", Discretization.Option.EQUAL_FREQ);
		Map<String, Integer> numIntervalsPerVariable = new HashMap<>();
		numIntervalsPerVariable.put("X", 2);

		CaseDatabase newDatabase = Discretization.process(db, discretizeOptions, numIntervalsPerVariable);
		Variable newVarX = newDatabase.getVariable("X");
		Assertions.assertEquals(3, newVarX.getStates().length);
		Assertions.assertEquals("(-Infinity , 2.0]", newVarX.getStates()[0].getName());
		Assertions.assertEquals("(2.0 , 4.0]", newVarX.getStates()[1].getName());
		Assertions.assertEquals("(4.0 , Infinity)", newVarX.getStates()[2].getName());
	}

	/**
	 * Regression for B-isNumeric: numeric-ness must depend only on whether the state names
	 * parse as numbers (ignoring the "?" marker), not on how many states there are. Numeric
	 * variables with exactly 2 or 4 states used to be misclassified as non-numeric.
	 */
	@Test public void testIsNumericIgnoresStateCount() {
		Assertions.assertTrue(Discretization.isNumeric(new Variable("X", "1.0", "2.0")));               // 2 states
		Assertions.assertTrue(Discretization.isNumeric(new Variable("X", "1.0", "2.0", "3.0", "4.0"))); // 4 states
		Assertions.assertTrue(Discretization.isNumeric(new Variable("X", "1.0", "2.0", "3.0")));        // 3 states
		Assertions.assertTrue(Discretization.isNumeric(new Variable("X", "1.0", "?", "2.0")));          // numeric + missing
		Assertions.assertFalse(Discretization.isNumeric(new Variable("X", "low", "high")));             // categorical
		Assertions.assertFalse(Discretization.isNumeric(new Variable("X", "?")));                       // only missing
	}

	/**
	 * Regression for B-isNumeric (end to end): discretizing a 2-state numeric variable used
	 * to route it through the non-numeric branch of discretizeCases, mapping every case to
	 * state 0. The cases must instead be mapped to their real intervals.
	 */
	@Test public void testDiscretizeTwoStateNumericDoesNotCollapse() {
		Variable x = new Variable("X", "1.0", "2.0");
		int[][] cases = { { 0 }, { 1 }, { 0 }, { 1 } };  // values 1.0, 2.0, 1.0, 2.0
		CaseDatabase db = new CaseDatabase(List.of(x), cases);

		Map<String, Discretization.Option> discretizeOptions = new HashMap<>();
		discretizeOptions.put("X", Discretization.Option.EQUAL_WIDTH);
		Map<String, Integer> numIntervalsPerVariable = new HashMap<>();
		numIntervalsPerVariable.put("X", 2);

		CaseDatabase newDatabase = Discretization.process(db, discretizeOptions, numIntervalsPerVariable);

		Assertions.assertEquals(2, newDatabase.getVariable("X").getStates().length);
		int[][] newCases = newDatabase.getCases();
		Assertions.assertEquals(0, newCases[0][0]);
		Assertions.assertEquals(1, newCases[1][0]);
		Assertions.assertEquals(0, newCases[2][0]);
		Assertions.assertEquals(1, newCases[3][0]);
	}

	@Tag(TestSpeed.SLOW)
	@Test public void testDiscretizeModelNet() {

		State[] statesA = { new State("(-Infinity, -2]"), new State("(-2, 2]"), new State("(2, +Infinity]"),
				new State("?") };
		State[] statesB = { new State("(-Infinity, 0]"), new State("(0, +Infinity]"), new State("?") };

		PartitionedInterval partitionedIntervalA = new PartitionedInterval(
				new double[] { Double.NEGATIVE_INFINITY, -2.0, 2.0, Double.POSITIVE_INFINITY },
				new boolean[] { true, true, true, true });
		PartitionedInterval partitionedIntervalB = new PartitionedInterval(
				new double[] { Double.NEGATIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY },
				new boolean[] { true, true, true });

		Variable modelVarA = new Variable("A", statesA, partitionedIntervalA, 0.001);
		Variable modelVarB = new Variable("B", statesB, partitionedIntervalB, 0.001);

		ProbNet modelNet = new ProbNet();
		modelNet.addNode(modelVarA, NodeType.CHANCE);
		modelNet.addNode(modelVarB, NodeType.CHANCE);

		CaseDatabase newDatabase = Discretization.process(database, modelNet);

		int[][] newCases = newDatabase.getCases();
		Assertions.assertEquals(2, newCases[0][0]);
		Assertions.assertEquals(1, newCases[0][1]);
		Assertions.assertEquals(3, newCases[2][0]);
		Assertions.assertEquals(2, newCases[7][0]);
		Assertions.assertEquals(1, newCases[7][1]);
	}

	@Test public void testDiscretizeMDLPCleanSplit() {
		// X = 1..4 → class 0, X = 5..8 → class 1. MDLP should find a single cut at 4.5.
		Variable varX = new Variable("X", "1", "2", "3", "4", "5", "6", "7", "8");
		Variable varY = new Variable("Y", "c0", "c1");
		List<Variable> variables = new ArrayList<>();
		variables.add(varX);
		variables.add(varY);
		int[][] cases = {
				{ 0, 0 }, { 1, 0 }, { 2, 0 }, { 3, 0 },
				{ 4, 1 }, { 5, 1 }, { 6, 1 }, { 7, 1 }
		};
		CaseDatabase db = new CaseDatabase(variables, cases);

		Map<String, Discretization.Option> opts = new HashMap<>();
		opts.put("X", Discretization.Option.MDLP);
		opts.put("Y", Discretization.Option.NONE);
		Map<String, Integer> numIntervals = new HashMap<>();
		numIntervals.put("X", -1);
		numIntervals.put("Y", -1);

		CaseDatabase result = Discretization.process(db, opts, numIntervals, null, varY);
		Variable newX = result.getVariable("X");

		Assertions.assertEquals(2, newX.getStates().length);
		double[] limits = newX.getPartitionedInterval().getLimits();
		Assertions.assertEquals(1.0, limits[0], 1e-9);
		Assertions.assertEquals(4.5, limits[1], 1e-9);
		Assertions.assertEquals(8.0, limits[2], 1e-9);

		int[][] newCases = result.getCases();
		Assertions.assertEquals(0, newCases[0][0]); // X=1 → bin 0
		Assertions.assertEquals(0, newCases[3][0]); // X=4 → bin 0
		Assertions.assertEquals(1, newCases[4][0]); // X=5 → bin 1
		Assertions.assertEquals(1, newCases[7][0]); // X=8 → bin 1
	}

	@Test public void testDiscretizeMDLPNoSignalKeepsSingleInterval() {
		// Class alternates with X: no informative cut survives the MDL criterion.
		Variable varX = new Variable("X", "1", "2", "3", "4", "5");
		Variable varY = new Variable("Y", "c0", "c1");
		List<Variable> variables = new ArrayList<>();
		variables.add(varX);
		variables.add(varY);
		int[][] cases = { { 0, 0 }, { 1, 1 }, { 2, 0 }, { 3, 1 }, { 4, 0 } };
		CaseDatabase db = new CaseDatabase(variables, cases);

		Map<String, Discretization.Option> opts = new HashMap<>();
		opts.put("X", Discretization.Option.MDLP);
		opts.put("Y", Discretization.Option.NONE);
		Map<String, Integer> numIntervals = new HashMap<>();
		numIntervals.put("X", -1);
		numIntervals.put("Y", -1);

		CaseDatabase result = Discretization.process(db, opts, numIntervals, null, varY);
		Variable newX = result.getVariable("X");
		Assertions.assertEquals(1, newX.getStates().length);
	}

	@Test public void testDiscretizeMDLPThrowsWithoutClassVariable() {
		Map<String, Discretization.Option> opts = new HashMap<>();
		opts.put("A", Discretization.Option.MDLP);
		opts.put("B", Discretization.Option.NONE);
		Map<String, Integer> numIntervals = new HashMap<>();
		numIntervals.put("A", -1);
		numIntervals.put("B", -1);

		Assertions.assertThrows(IllegalArgumentException.class,
				() -> Discretization.process(database, opts, numIntervals, null, null));
	}

	@Test public void testDiscretizeChiMergeCleanSplit() {
		// Same clean-signal pattern used for MDLP: X = 1..4 → c0, X = 5..8 → c1.
		Variable varX = new Variable("X", "1", "2", "3", "4", "5", "6", "7", "8");
		Variable varY = new Variable("Y", "c0", "c1");
		List<Variable> variables = new ArrayList<>();
		variables.add(varX);
		variables.add(varY);
		int[][] cases = {
				{ 0, 0 }, { 1, 0 }, { 2, 0 }, { 3, 0 },
				{ 4, 1 }, { 5, 1 }, { 6, 1 }, { 7, 1 }
		};
		CaseDatabase db = new CaseDatabase(variables, cases);

		Map<String, Discretization.Option> opts = new HashMap<>();
		opts.put("X", Discretization.Option.CHIMERGE);
		opts.put("Y", Discretization.Option.NONE);
		Map<String, Integer> numIntervals = new HashMap<>();
		numIntervals.put("X", -1);
		numIntervals.put("Y", -1);

		CaseDatabase result = Discretization.process(db, opts, numIntervals, null, varY);
		Variable newX = result.getVariable("X");
		Assertions.assertEquals(2, newX.getStates().length);
		double[] limits = newX.getPartitionedInterval().getLimits();
		Assertions.assertEquals(1.0, limits[0], 1e-9);
		Assertions.assertEquals(4.5, limits[1], 1e-9);
		Assertions.assertEquals(8.0, limits[2], 1e-9);
	}

	@Test public void testDiscretizeChiMergeThrowsWithoutClassVariable() {
		Map<String, Discretization.Option> opts = new HashMap<>();
		opts.put("A", Discretization.Option.CHIMERGE);
		opts.put("B", Discretization.Option.NONE);
		Map<String, Integer> numIntervals = new HashMap<>();
		numIntervals.put("A", -1);
		numIntervals.put("B", -1);
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> Discretization.process(database, opts, numIntervals, null, null));
	}

	@Test public void testDiscretizeKMeansTwoClusters() {
		// Two well-separated clusters of values.
		Variable varX = new Variable("X", "1", "2", "3", "10", "11", "12");
		List<Variable> variables = new ArrayList<>();
		variables.add(varX);
		int[][] cases = { { 0 }, { 1 }, { 2 }, { 3 }, { 4 }, { 5 } };
		CaseDatabase db = new CaseDatabase(variables, cases);

		Map<String, Discretization.Option> opts = new HashMap<>();
		opts.put("X", Discretization.Option.KMEANS);
		Map<String, Integer> numIntervals = new HashMap<>();
		numIntervals.put("X", 2);

		CaseDatabase result = Discretization.process(db, opts, numIntervals);
		Variable newX = result.getVariable("X");
		Assertions.assertEquals(2, newX.getStates().length);
		double[] limits = newX.getPartitionedInterval().getLimits();
		// Centroids should converge to ≈ 2 and 11; midpoint ≈ 6.5.
		Assertions.assertEquals(1.0, limits[0], 1e-9);
		Assertions.assertEquals(6.5, limits[1], 1e-6);
		Assertions.assertEquals(12.0, limits[2], 1e-9);
	}

	@Test public void testDiscretizeModelNetFS() {

		List<Variable> variables = new ArrayList<>();
		varA = new Variable("A", "st.quo", "higher", "lower");
		varB = new Variable("B", "-", "+", "0");
		variables.add(varA);
		variables.add(varB);

		int[][] cases = { { 0, 0 }, { 0, 1 }, { 1, 1 }, { 2, 1 }, { 0, 2 }, { 2, 0 }, { 1, 0 }, { 2, 2 } };

		database = new CaseDatabase(variables, cases);

		Variable modelVarA = new Variable("A", "lower", "st.quo", "higher");
		Variable modelVarB = new Variable("B", "-", "0", "+");

		ProbNet modelNet = new ProbNet();
		modelNet.addNode(modelVarA, NodeType.CHANCE);
		modelNet.addNode(modelVarB, NodeType.CHANCE);

		CaseDatabase newDatabase = Discretization.process(database, modelNet);

		int[][] newCases = newDatabase.getCases();
		Assertions.assertEquals(1, newCases[0][0]);
		Assertions.assertEquals(0, newCases[0][1]);
		Assertions.assertEquals(2, newCases[2][0]);
		Assertions.assertEquals(0, newCases[7][0]);
		Assertions.assertEquals(1, newCases[7][1]);
	}

}
