/*
 * Copyright (c) CISIAD, UNED, Spain,  2018. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.preprocess;

import org.junit.jupiter.api.*;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.io.database.CaseDatabase;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.test.TestSpeed;
import org.openmarkov.learning.core.preprocess.exception.WrongDiscretizationLimitException;

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

	@Test public void testNoDiscretize()
			throws InvalidStateException, NodeNotFoundException, WrongDiscretizationLimitException {
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

	@Test public void testDiscretizeEqualWidth()
			throws InvalidStateException, NodeNotFoundException, WrongDiscretizationLimitException {
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

	@Test public void testDiscretizeEqualFreq()
			throws InvalidStateException, NodeNotFoundException, WrongDiscretizationLimitException {
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
	
	@Tag(TestSpeed.SLOW)
	@Test public void testDiscretizeModelNet()
			throws InvalidStateException, NodeNotFoundException, WrongDiscretizationLimitException {

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

	@Test public void testDiscretizeModelNetFS()
			throws InvalidStateException, NodeNotFoundException, WrongDiscretizationLimitException {

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
