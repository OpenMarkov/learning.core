/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.algorithm;

import org.openmarkov.core.action.base.ConstraintChecker;
import org.openmarkov.core.action.base.PNEdit;
import org.openmarkov.core.developmentStaticAnalysis.requirements.ImplementationRequirements;
import org.openmarkov.core.developmentStaticAnalysis.requirements.RequiredConstructor;
import org.openmarkov.core.exception.*;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.learning.core.util.LearningEditMotivation;
import org.openmarkov.learning.core.util.LearningEditProposal;
import org.openmarkov.learning.core.util.ModelNetUse;
import org.openmarkov.learning.core.util.Util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract learning algorithm.
 */
@ImplementationRequirements(requiresOneOfTheseConstructors = @RequiredConstructor({ProbNet.class, CaseDatabase.class}))
public abstract class LearningAlgorithm {

    private static final Logger logger = LogManager.getLogger(LearningAlgorithm.class);

    /**
     * Parameter for the parametric learning.
     */
    protected final double alpha;
    
    /**
     * Net to learn
     */
    protected final ProbNet probNet;
    
    /**
     * Case database
     */
    protected final CaseDatabase caseDatabase;
    
    /**
     * List of blocked edits
     */
    protected final List<LearningEditProposal> blockedEdits = new ArrayList<>();
    
    protected String classVariableName;
    
    public String getClassVariableName() {
        return classVariableName;
    }
    
    public void setClassVariableName(String classVariableName) {
        this.classVariableName = classVariableName;
    }
    
    // Constructor
    /**
     * Constructs a learning algorithm with the given network, database, and Laplace smoothing parameter.
     *
     * @param probNet      the probabilistic network to learn
     * @param caseDatabase the case database to learn from
     * @param alpha        the Laplace smoothing parameter for parametric learning
     */
    public LearningAlgorithm(ProbNet probNet, CaseDatabase caseDatabase, double alpha) {
        this.probNet = probNet;
        this.caseDatabase = caseDatabase;
        this.alpha = alpha;
    }
    
    /**
     * Method invoked to run the algorithm.
     *
     * @param modelNetUse ModelNetUse
     */
    public void run(ModelNetUse modelNetUse) throws IncompatibleEvidenceException.EvidenceIsIncompatibleWithOther, NonProjectablePotentialException, CannotNormalizePotentialException, NotEvaluableNetworkException.NotApplicableNetwork, ConstraintViolatedException {
        init(modelNetUse);
        /* Main loop */
        LearningEditProposal bestEdition = getBestEdit(true, true);
        while (bestEdition != null) {
            PNEdit bestEdit = bestEdition.getEdit();
            try {
                bestEdit.executeEdit();
            } catch (DoEditException exception) {
                logger.debug("Edit not allowed by constraint, skipping: {}", bestEdit, exception);
            }
            bestEdition = getBestEdit(true, true);
        }
        /* Parametric Learning */
        parametricLearning();
    }
    
    /**
     * Tells the learning algorithm to advance until the next phase
     */
    public void runTillNextPhase() {
        int currentPhase = getPhase();
        LearningEditProposal bestEditProposal = getBestEdit(true, true);
        while ((bestEditProposal != null) && (currentPhase == getPhase())) {
            logger.debug("{}", bestEditProposal);
            PNEdit bestEdit = bestEditProposal.getEdit();
            try {
                bestEdit.executeEdit();
            } catch (DoEditException exception) {
                logger.debug("Edit not allowed by constraint, skipping: {}", bestEdit, exception);
            }
            bestEditProposal = getBestEdit(true, true);
        }
    }
    
    /**
     * Initializes the algorithm
     *
     * @param modelNetUse nodelNetUse
     */
    public void init(ModelNetUse modelNetUse) {
        // Do nothing
    }
    
    /**
     * This method returns the best edition (and its associated score)
     * that can be done to the network that is being learnt.
     *
     * @param onlyAllowedEdits  If this parameter is true, only those editions
     *                          that do not provoke a ConstraintViolatedException are returned
     * @param onlyPositiveEdits If this parameter is true, only those
     *                          editions with a positive associated score are returned.
     *
     * @return {@code LearningEditProposal} with the best edition and its score.
     */
    public abstract LearningEditProposal getBestEdit(boolean onlyAllowedEdits, boolean onlyPositiveEdits);
    
    /**
     * This method returns the next best edition (and its associated score)
     * that can be done to the network that is being learnt.
     *
     * @param onlyAllowedEdits  If this parameter is true, only those editions
     *                          that do not provoke a ConstraintViolatedException are returned
     * @param onlyPositiveEdits If this parameter is true, only those
     *                          editions with a positive associated score are returned.
     *
     * @return {@code LearningEditProposal} with the best edition and its score.
     */
    public abstract LearningEditProposal getNextEdit(boolean onlyAllowedEdits, boolean onlyPositiveEdits);
    
    /**
     * Calculates the score associated to the given edit.
     *
     * @param edit {@code PNEdit}
     *
     * @return {@code LearningEditMotivation} motivation for the given edit
     */
    public abstract LearningEditMotivation getMotivation(PNEdit edit);
    
    /**
     * This function creates the Potentials associated to each node,
     * normalizing the absolute frequencies of the configurations of
     * the parents.
     */
    public ProbNet parametricLearning() throws NotEvaluableNetworkException.NotApplicableNetwork, ConstraintViolatedException, CannotNormalizePotentialException, IncompatibleEvidenceException.EvidenceIsIncompatibleWithOther, NonProjectablePotentialException {
        for (Node node : probNet.getNodes()) {
            if (node.getNumPotentials() > 0) {    // Remove all the potentials of the node if any exists.
                probNet.removePotentials(node);
            }
            TablePotential absoluteFrequencies = Util.getAbsoluteFreq(probNet, caseDatabase, node);
            for (int j = 0; j < absoluteFrequencies.getTableSize(); j++)
                absoluteFrequencies.getValues()[j] += alpha;
            probNet.addPotential(DiscretePotentialOperations.normalize(absoluteFrequencies));
        }
        return probNet;
    }
    
    /**
     * Blocks edit
     *
     * @param edit to block
     */
    public void blockEdit(LearningEditProposal edit) {
        blockedEdits.add(edit);
    }
    
    /**
     * Unblocks a previously blocked edit.
     *
     * @param edit the edit to unblock
     */
    public void unblockEdit(LearningEditProposal edit) {
        blockedEdits.remove(edit);
    }
    
    /**
     * @return the blockedEdits
     */
    public List<LearningEditProposal> getBlockedEdits() {
        return blockedEdits;
    }
    
    /**
     * Checks whether the given edit proposal is blocked.
     *
     * @param edit the edit proposal to check
     * @return true if the edit is in the blocked list
     */
    public boolean isBlocked(LearningEditProposal edit) {
        return blockedEdits.contains(edit);
    }
    
    /**
     * Checks whether the given edit is blocked.
     *
     * @param edit the edit to check
     * @return true if the edit is blocked
     */
    public boolean isBlocked(PNEdit edit) {
        for (LearningEditProposal editProposal : blockedEdits) {
            if (editProposal.getEdit().equals(edit)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks whether the given edit is allowed by the network's constraints.
     *
     * @param edit the edit to check
     * @return true if the edit does not violate any constraint
     */
    protected static boolean isAllowed(PNEdit edit) {
        //Announce edit to check whether it is allowed or not
        try {
            ConstraintChecker constraintChecker = new ConstraintChecker(edit.getProbNet());
            edit.checkConstraintsWillBeMet(constraintChecker);
            constraintChecker.buildAndThrow();
            return true;
        } catch (ConstraintViolatedException e) {
            return false;
        }
    }
    
    public int getPhase() {
        return 0;
    }
    
    /**
     * Retrieves whether the LearningAlgorithm is in the last phase.
     * True by default; the method must be overrided in derived classes.
     */
    public boolean isLastPhase() {
        return true;
    }
    
}
