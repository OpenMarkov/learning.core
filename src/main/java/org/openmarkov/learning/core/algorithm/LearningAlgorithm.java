/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.algorithm;

import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.annotation.ImplementationRequirements;
import org.openmarkov.core.annotation.RequiredConstructor;
import org.openmarkov.core.annotation.ToCheck;
import org.openmarkov.core.exception.*;
import org.openmarkov.core.io.database.CaseDatabase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.learning.core.util.LearningEditMotivation;
import org.openmarkov.learning.core.util.LearningEditProposal;
import org.openmarkov.learning.core.util.ModelNetUse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ImplementationRequirements(requiresOneOfTheseConstructors = @RequiredConstructor({ProbNet.class, CaseDatabase.class}))
/**
 * Abstract learning algorithm.
 */
public abstract class LearningAlgorithm {
    
    /**
     * Parameter for the parametric learning.
     */
    protected double alpha;
    
    /**
     * Net to learn
     */
    protected ProbNet probNet;
    
    /**
     * Case database
     */
    protected CaseDatabase caseDatabase;
    
    /**
     * List of blocked edits
     */
    protected List<LearningEditProposal> blockedEdits = new ArrayList<>();
    
    protected String classVariableName;
    
    public String getClassVariableName() {
        return classVariableName;
    }
    
    public void setClassVariableName(String classVariableName) {
        this.classVariableName = classVariableName;
    }
    
    protected int phase = 0;
    
    // Constructor
    public LearningAlgorithm(ProbNet probNet, CaseDatabase caseDatabase, double alpha) {
        this.probNet = probNet;
        this.caseDatabase = caseDatabase;
        this.alpha = alpha;
    }
    
    /**
     * Method invoked to run the algorithm.
     *
     * @param modelNetUse ModelNetUse
     *
     * @throws CannotNormalizePotentialException
     */
    public void run(ModelNetUse modelNetUse) throws CannotNormalizePotentialException, IncompatibleEvidenceException.EvidenceIsIncompatibleWithOther, NonProjectablePotentialException, NotEvaluableNetworkException.NotApplicableNetwork, NotEvaluableNetworkException.UnsatisfiedContraints {
        init(modelNetUse);
        /* Main loop */
        LearningEditProposal bestEdition = getBestEdit(true, true);
        while (bestEdition != null) {
            PNEdit bestEdition1 = bestEdition.getEdit();
            @ToCheck(reasonKind = ToCheck.ReasonKind.PROBABLE_BUG,
                    reasonDescription = "Does this code work as the comment is telling it does?")
            var check = false;
            /* If there have been any improvements on the score, we update
             * the learnedNet. */
            try {
                bestEdition1.doEdit(probNet);
            } catch (DoEditException exception) {
                /* If the edition was not allowed (ModelNetworkconstraint)
                 * the algorithm just goes through the next iteration of the
                 * loop, asking the cache for the next best edition.
                 */
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
            System.out.println(bestEditProposal);
            PNEdit bestEdition = bestEditProposal.getEdit();
            @ToCheck(reasonKind = ToCheck.ReasonKind.PROBABLE_BUG,
                    reasonDescription = "Does this code work as the comment is telling it does?")
            var check = false;
            /* If there have been any improvements on the score, we update
             * the learnedNet. */
            try {
                bestEdition.doEdit(probNet);
            } catch (DoEditException exception) {
                /* If the edition was not allowed (ModelNetworkconstraint)
                 * the algorithm just goes through the next iteration of the
                 * loop, asking the cache for the next best edition.
                 */
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
     *                          that do not provoke a ConstraintViolated are returned
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
     *                          that do not provoke a ConstraintViolated are returned
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
     *
     * @throws CannotNormalizePotentialException
     */
    public ProbNet parametricLearning() throws CannotNormalizePotentialException, IncompatibleEvidenceException.EvidenceIsIncompatibleWithOther, NonProjectablePotentialException, NotEvaluableNetworkException.NotApplicableNetwork, NotEvaluableNetworkException.UnsatisfiedContraints {
        for (Node node : probNet.getNodes()) {
            if (node.getNumPotentials() == 0) {    // Remove all the potentials of the node if any exists.
                probNet.removePotentials(node);
            }
            TablePotential absoluteFrequencies = getAbsoluteFrequencies(node);
            for (int j = 0; j < absoluteFrequencies.getTableSize(); j++)
                absoluteFrequencies.values[j] += alpha;
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
     * Blocks edit
     *
     * @param edit to block
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
     * Blocks edit
     *
     * @param edit to block
     */
    public boolean isBlocked(LearningEditProposal edit) {
        return blockedEdits.contains(edit);
    }
    
    public boolean isBlocked(PNEdit edit) {
        for (LearningEditProposal editProposal : blockedEdits) {
            if (editProposal.getEdit().equals(edit)) {
                return true;
            }
        }
        return false;
    }
    
    protected static boolean isAllowed(PNEdit edit) {
        boolean isAllowed = true;
        //Announce edit to check whether it is allowed or not
        try {
            edit.checkConstraintsWillBeMet();
        } catch (DoEditException.ConstraintViolated e) {
            isAllowed = false;
        }
        return isAllowed;
    }
    
    public int getPhase() {
        return phase;
    }
    
    /**
     * Retrieves whether the LearningAlgorithm is in the last phase.
     * True by default; the method must be overrided in derived classes.
     */
    public boolean isLastPhase() {
        return true;
    }
    
    /**
     * Calculate the absolute frequencies in the database of each of the
     * configurations of the given node and its parents.
     *
     * @param node {@code Node} whose frequencies we want to
     *             calculate.
     *
     * @return {@code TablePotential(node,parents)} with the absolute frequencies in
     * the database of each of the configurations of the given node and its
     * parents.
     */
    private TablePotential getAbsoluteFrequencies(Node node) {
        
        List<Node> parents = node.getParents();
        int numParents = parents.size();
        int[] indexesOfParents = new int[numParents];
        int[] parentsStateNum = new int[numParents];
        List<Variable> potentialVariables = new ArrayList<Variable>(numParents + 1);
        Variable variableNode = node.getVariable();
        
        potentialVariables.add(variableNode);
        if (numParents > 0) {
            int indexOfParent = 0;
            for (Node parent : parents) {
                Variable parentVariable = parent.getVariable();
                potentialVariables.add(parentVariable);
                indexesOfParents[indexOfParent] = caseDatabase.getVariables().indexOf(parentVariable);
                parentsStateNum[indexOfParent] = parentVariable.getNumStates();
                indexOfParent++;
            }
        }
        
        int numValues = variableNode.getNumStates();
        TablePotential absoluteFreqPotential = new TablePotential(potentialVariables, PotentialRole.CONDITIONAL_PROBABILITY);
        double[] absoluteFreqs = absoluteFreqPotential.getValues();
        int iNode = caseDatabase.getVariables().indexOf(variableNode);
        
        // Initialize the table
        Arrays.fill(absoluteFreqs, 0);
        
        potentialVariables.remove(0);
        // Compute the absolute frequencies
        int[][] cases = caseDatabase.getCases();
        for (int i = 0; i < cases.length; i++) {
            int iCPT = 0;
            for (int j = numParents - 1; j >= 0; --j) {
                iCPT = iCPT * parentsStateNum[j] + cases[i][indexesOfParents[j]];
            }
            absoluteFreqs[numValues * iCPT + cases[i][iNode]]++;
        }
        
        return absoluteFreqPotential;
    }
}
