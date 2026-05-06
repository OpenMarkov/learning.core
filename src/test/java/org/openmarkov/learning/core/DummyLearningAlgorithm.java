/*
 * Copyright (c) CISIAD, UNED, Spain, 2026. Licensed under the GPLv3 licence
 */

package org.openmarkov.learning.core;

import org.openmarkov.core.action.base.PNEdit;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.learning.core.algorithm.LearningAlgorithm;
import org.openmarkov.learning.core.algorithm.LearningAlgorithmType;
import org.openmarkov.learning.core.util.LearningEditMotivation;
import org.openmarkov.learning.core.util.LearningEditProposal;
import org.openmarkov.learning.core.util.ModelNetUse;

/**
 * Dummy algorithm for testing LearningManager.
 */
@LearningAlgorithmType(name = "Dummy", discriminative = false, supportsUnobservedVariables = false)
public class DummyLearningAlgorithm extends LearningAlgorithm {

    public boolean initCalled = false;
    public boolean runCalled = false;
    public boolean parametricLearningCalled = false;

    public DummyLearningAlgorithm(ProbNet probNet, CaseDatabase caseDatabase) {
        super(probNet, caseDatabase, 0.0);
    }

    // Add the 2-arg constructor that LearningManager expects (via params size
    // check)
    // Actually LearningManager passes 2 args, so this is enough.
    // Wait, LearningAlgorithm base has 3 args.
    // If I only provide this one, param count is 2. Perfect.

    @Override
    public void init(ModelNetUse modelNetUse) {
        this.initCalled = true;
    }

    @Override
    public void run(ModelNetUse modelNetUse) {
        this.runCalled = true;
    }

    @Override
    public ProbNet parametricLearning() {
        this.parametricLearningCalled = true;
        return this.probNet;
    }

    @Override
    public LearningEditProposal getBestEdit(boolean onlyAllowedEdits, boolean onlyPositiveEdits) {
        return null;
    }

    @Override
    public LearningEditProposal getNextEdit(boolean onlyAllowedEdits, boolean onlyPositiveEdits) {
        return null;
    }

    @Override
    public LearningEditMotivation getMotivation(PNEdit edit) {
        return null; // stub
    }
}
