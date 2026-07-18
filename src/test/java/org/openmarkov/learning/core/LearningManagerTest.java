/*
 * Copyright (c) CISIAD, UNED, Spain, 2026. Licensed under the GPLv3 licence
 */

package org.openmarkov.learning.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.model.network.Variable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.openmarkov.core.action.base.linkEdits.AddLinkEdit;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.learning.core.exception.UnobservedVariablesException;
import org.openmarkov.learning.core.util.ModelNetUse;

public class LearningManagerTest {

    private CaseDatabase caseDatabase;
    private LearningManager learningManager;

    @BeforeEach
    public void setUp() throws Exception {
        Variable vA = new Variable("A", "0", "1");
        Variable vB = new Variable("B", "0", "1");
        List<Variable> variables = new ArrayList<>();
        variables.add(vA);
        variables.add(vB);
        int[][] cases = { { 0, 0 } };
        caseDatabase = new CaseDatabase(variables, cases);

        // We use algorithmName "Dummy" but since modelNetUse is null, it won't resolve
        // the class yet.
        learningManager = new LearningManager(caseDatabase, DummyLearningAlgorithm.class, null, null);
    }

    @Test
    public void testInitAndLearn() throws Exception {
        DummyLearningAlgorithm dummyAlgo = new DummyLearningAlgorithm(learningManager.getLearnedNet(), caseDatabase);

        learningManager.init(dummyAlgo);

        // init calls dummyAlgo.init(modelNetUse=null)
        assertTrue(dummyAlgo.initCalled, "Algorithm init should be called");

        learningManager.learn();
        assertTrue(dummyAlgo.runCalled, "Algorithm run should be called");
    }

    @Test
    public void testApplyEdit() throws Exception {
        DummyLearningAlgorithm dummyAlgo = new DummyLearningAlgorithm(learningManager.getLearnedNet(), caseDatabase);
        learningManager.init(dummyAlgo);

        ProbNet net = learningManager.getLearnedNet();
        AddLinkEdit edit = new AddLinkEdit(net, net.getVariables().get(0), net.getVariables().get(1), true);

        // Apply edit should execute it and call parametricLearning
        learningManager.applyEdit(edit);

        assertEquals(1, net.getLinks().size(), "Edit should be executed on the net");
        assertTrue(dummyAlgo.parametricLearningCalled, "Parametric learning should be called");
    }

    /**
     * Regression for B-Learn: when an algorithm that does not support unobserved variables
     * is given a model net with a variable absent from the database, the exception must
     * report only the missing variable (by name), not every model-net variable. The model
     * net and the database use distinct Variable instances, as happens when each is loaded
     * separately, so the previous identity-based removeAll reported all of them.
     */
    @Test
    public void testUnobservedVariablesReportsOnlyMissingByName() {
        Variable dbA = new Variable("A", "0", "1");
        Variable dbB = new Variable("B", "0", "1");
        List<Variable> dbVars = new ArrayList<>();
        dbVars.add(dbA);
        dbVars.add(dbB);
        CaseDatabase db = new CaseDatabase(dbVars, new int[][] { { 0, 0 } });

        ProbNet modelNet = new ProbNet();
        modelNet.addNode(new Variable("A", "0", "1"), NodeType.CHANCE);
        modelNet.addNode(new Variable("B", "0", "1"), NodeType.CHANCE);
        modelNet.addNode(new Variable("C", "0", "1"), NodeType.CHANCE);

        ModelNetUse use = new ModelNetUse(true, true, false, false, false, false);

        UnobservedVariablesException thrown = assertThrows(UnobservedVariablesException.class,
                () -> new LearningManager(db, DummyLearningAlgorithm.class, modelNet, use));

        List<String> names = thrown.unobservedVariables.stream().map(Variable::getName).toList();
        assertEquals(List.of("C"), names);
    }
}
