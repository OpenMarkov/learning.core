/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core;

import org.openmarkov.core.action.base.PNEdit;
import org.openmarkov.core.exception.*;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.learning.core.algorithm.LearningAlgorithm;
import org.openmarkov.learning.core.algorithm.LearningAlgorithmManager;
import org.openmarkov.learning.core.algorithm.LearningAlgorithmType;
import org.openmarkov.core.model.network.constraint.ModelNetworkConstraint;
import org.openmarkov.learning.core.exception.EmptyModelNetException;
import org.openmarkov.learning.core.exception.UnobservedVariablesException;
import org.openmarkov.learning.core.util.ModelNetUse;

import java.util.*;

/**
 * This class launches the learning algorithm and receives the results of
 * the learning.
 *
 * @author joliva
 * @author manuel
 * @author fjdiez
 * @version 1.0
 * @since OpenMarkov 1.0
 */
public class LearningManager {
    
    /**
     * Learning algorithm
     */
    private LearningAlgorithm learningAlgorithm = null;
    
    /**
     * ProbNet to learn.
     */
    private final ProbNet learnedNet;
    
    /**
     * Structure that specifies use of model net
     */
    private final ModelNetUse modelNetUse;
    
    /**
     * Case database
     */
    private final CaseDatabase caseDatabase;
    
    // Constructor
    
    /**
     * @param caseDatabase  the case database to learn from
     * @param algorithmName {@code LearningAlgorithm} indicating the algorithm
     *                      selected by the user.
     * @param modelNet      {@code ProbNet} Net from which take the
     *                      information of the nodes and links
     * @param modelNetUse   {@code boolean[]} use the positions of the nodes,
     *                      use also the initial links or use them fixed
     *
     * @throws EmptyModelNetException if empty model net occurs
     * @throws UnobservedVariablesException if unobserved variables occurs
     */
    public LearningManager(CaseDatabase caseDatabase, Class<? extends LearningAlgorithm> algorithmName, ProbNet modelNet, ModelNetUse modelNetUse)
            throws EmptyModelNetException, UnobservedVariablesException {
        
        this.caseDatabase = caseDatabase;
        /* Check ModelNet is not null */
        if (modelNetUse != null && modelNetUse.isUseModelNet()) {
            if (modelNet == null) {
                throw new EmptyModelNetException();
            }
            this.learnedNet = applyModelNet(algorithmName, caseDatabase, modelNet,                                            modelNetUse);
        } else {
            this.learnedNet = new ProbNet();
            for (Variable variable : caseDatabase.getVariables()) {
                learnedNet.addNode(variable, NodeType.CHANCE);
            }
        }
        
        this.modelNetUse = modelNetUse;
    }
    
    /**
     * Initialize the learning algorithm.
     */
    public void init(LearningAlgorithm learningAlgorithm) {
        
        this.learningAlgorithm = learningAlgorithm;
        learningAlgorithm.init(modelNetUse);
    }
    
    /**
     * Main method to launch the learning process.
     */
    public void learn() throws IncompatibleEvidenceException.EvidenceIsIncompatibleWithOther, NonProjectablePotentialException, CannotNormalizePotentialException, NotEvaluableNetworkException.NotApplicableNetwork, ConstraintViolatedException {
        learningAlgorithm.run(modelNetUse);
    }
    
    /**
     * Returns learned net
     *
     * @return {@code ProbNet} containing learned net
     */
    public ProbNet getLearnedNet() {
        
        return this.learnedNet;
    }
    
    /**
     * Returns the learningAlgorithm.
     *
     * @return the learningAlgorithm.
     */
    public LearningAlgorithm getLearningAlgorithm() {
        
        return learningAlgorithm;
    }
    
    /**
     * Applies the edit passed to the learnedNet and updates parameters
     *
     * @param edit the edit to apply
     *
     * @throws DoEditException if the edit cannot be executed
     */
    public void applyEdit(PNEdit edit)
            throws DoEditException, NonProjectablePotentialException, NotEvaluableNetworkException.NotApplicableNetwork, IncompatibleEvidenceException.EvidenceIsIncompatibleWithOther, CannotNormalizePotentialException, ConstraintViolatedException {
        edit.executeEdit();
        learningAlgorithm.parametricLearning();
    }

    /**
     * Runs parametric learning on the current learned network structure.
     * Useful after interactive learning to ensure all nodes have potentials.
     */
    public ProbNet runParametricLearning() throws IncompatibleEvidenceException.EvidenceIsIncompatibleWithOther, NonProjectablePotentialException, CannotNormalizePotentialException, NotEvaluableNetworkException.NotApplicableNetwork, ConstraintViolatedException {
        return learningAlgorithm.parametricLearning();
    }
    
    /**
     * Adds links and constraints depending on the structure of the model net
     * and the option selected by the user.
     *
     * @param algorithmClass the learning algorithm class
     * @param database       the case database
     * @param modelNetUse    use of the model net selected by the user.
     * @param modelNet       structure of the net to add the constraints
     *
     * @throws UnobservedVariablesException if unobserved variables occurs
     *
     * @return the configured {@code ProbNet} with model net constraints applied
     */
    private ProbNet applyModelNet(Class<? extends LearningAlgorithm> algorithmClass, CaseDatabase database,
                                  ProbNet modelNet, ModelNetUse modelNetUse) throws UnobservedVariablesException {
        
        ProbNet probNet = null;
        List<Variable> missingVariables = getMissingVariables(database.getVariables(), modelNet.getVariables());
        if (!algorithmClass.getAnnotation(LearningAlgorithmType.class)
                           .supportsUnobservedVariables() && !missingVariables.isEmpty()) {
            List<Variable> latentVariables = new ArrayList<>(modelNet.getVariables());
            latentVariables.removeAll(database.getVariables());
            throw new UnobservedVariablesException(algorithmClass, latentVariables);
        }
        
        if (modelNetUse.isUseNodePositions()) {
            probNet = new ProbNet();
            for (Variable variable : database.getVariables()) {
                probNet.addNode(variable, NodeType.CHANCE);
            }
            copyNodePositionsFromModelNet(modelNet, probNet);
        }
        if (modelNetUse.isStartFromModelNet()) {
            probNet = modelNet.copy();
            
            // If the database includes variables that are not in the model net, add them
            for (Variable databaseVariable : database.getVariables()) {
                if (!probNet.containsVariable(databaseVariable.getName())) {
                    probNet.addNode(databaseVariable, NodeType.CHANCE);
                }
            }
            
            // ModelNetworkConstraint
            probNet.addConstraint(new ModelNetworkConstraint(modelNet, modelNetUse.isLinkAdditionAllowed(), modelNetUse.isLinkRemovalAllowed(), modelNetUse.isLinkInversionAllowed()));
            adaptDatabaseToModelNet(database, modelNet);
        }
        
        return probNet;
    }
    
    /**
     * Identifies and returns the list of variables that are present in the model network
     * but missing from the database.
     *
     * @param databaseVariables List of variables present in the database
     * @param modelNetVariables List of variables in the model network
     *
     * @return A list of variables that are in the model network but not in the database
     */
    private static List<Variable> getMissingVariables(List<Variable> databaseVariables, List<Variable> modelNetVariables) {
        
        List<Variable> missingVariables = new ArrayList<>(modelNetVariables);
        for (Variable databaseVariable : databaseVariables) {
            int i = 0;
            boolean found = false;
            while (i < missingVariables.size() && !found) {
                if (missingVariables.get(i).getName().equals(databaseVariable.getName())) {
                    found = true;
                    missingVariables.remove(i);
                }
                ++i;
            }
        }
        return missingVariables;
    }
    
    /**
     * Instantiates a learning algorithm using the learned network and the case database.
     *
     * @param algorithmClass the class of the algorithm to instantiate
     * @return the instantiated learning algorithm
     */
    public LearningAlgorithm instantiate(Class<? extends LearningAlgorithm> algorithmClass) {
        try {
            return LearningAlgorithmManager.INSTANCE.instantiateByClass(algorithmClass, List.of(this.learnedNet, this.caseDatabase));
        } catch (InvalidArgumentException e) {
            throw new UnreachableException(e);
        }
    }
    
    
    /**
     * Given a modelNet, applies the node positions and the order of the
     * states of the nodes of the modelNet to the nodes of the current probNet
     *
     * @param modelNet - the modelNet to copy the node positions from
     */
    private static void copyNodePositionsFromModelNet(ProbNet modelNet, ProbNet learntNet) {
        
        Node learntNetNode;
        
        /* Take the positions of the nodes */
        if (modelNet != null) {
            for (Node modelNetNode : modelNet.getNodes()) {
                learntNetNode = learntNet.getNode(modelNetNode.getVariable().getName());
                if (learntNetNode != null) {
                    double x = modelNetNode.getCoordinateX();
                    double y = modelNetNode.getCoordinateY();
                    learntNetNode.setCoordinateX(x);
                    learntNetNode.setCoordinateY(y);
                }
            }
        }
    }
    
    /**
     * Adapts the case database to match the model network's variable definitions,
     * including state ordering and variable types.
     *
     * @param database the case database to adapt
     * @param modelNet the model network providing variable definitions
     */
    private void adaptDatabaseToModelNet(CaseDatabase database, ProbNet modelNet) {
        
        for (Variable modelNetVariable : modelNet.getVariables()) {
            Variable caseDatabaseVariable = database.getVariable(modelNetVariable.getName());
            if (caseDatabaseVariable != null) {
                int variableIndex = database.getVariables().indexOf(caseDatabaseVariable);
                /* Check whether the variables are discretized or not before
                 * copying the states order. If both are discretized, they
                 * have to share the same intervals.
                 */
                if (caseDatabaseVariable.getVariableType() != VariableType.DISCRETIZED
                        && modelNetVariable.getVariableType() != VariableType.DISCRETIZED) {
                    updateCases(variableIndex, caseDatabaseVariable, modelNetVariable);
                }
                database.getVariables().set(variableIndex, modelNetVariable);
            }
        }
    }
    
    /**
     * Updates the cases in the case database for a specific variable by mapping the states
     * from the original variable to the corresponding states in the model network variable.
     *
     * @param variableIndex    index of the variable in the case database
     * @param originalVariable the original variable whose states are to be mapped
     * @param modelNetVariable the model network variable to which the states are mapped
     */
    private void updateCases(int variableIndex, Variable originalVariable, Variable modelNetVariable) {
        
        State state;
        
        for (int j = 0; j < caseDatabase.getCases().length; j++) {
            state = originalVariable.getStates()[caseDatabase.getCases()[j][variableIndex]];
            int stateIndex = modelNetVariable.getStateIndex(state.getName());
            if (stateIndex == -1) continue;
            caseDatabase.getCases()[j][variableIndex] = stateIndex;
        }
    }
    
}
