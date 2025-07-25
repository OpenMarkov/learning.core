/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core;

import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.CannotNormalizeNullVectorException;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.io.database.CaseDatabase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.learning.core.algorithm.LearningAlgorithm;
import org.openmarkov.learning.core.algorithm.LearningAlgorithmManager;
import org.openmarkov.learning.core.algorithm.LearningAlgorithmType;
import org.openmarkov.learning.core.constraint.ModelNetworkConstraint;
import org.openmarkov.learning.core.exception.EmptyModelNetException;
import org.openmarkov.learning.core.exception.UnobservedVariablesException;
import org.openmarkov.learning.core.util.LearningEditMotivation;
import org.openmarkov.learning.core.util.LearningEditProposal;
import org.openmarkov.learning.core.util.ModelNetUse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

	private static LearningAlgorithmManager learningAlgorithmManager = new LearningAlgorithmManager();

	/**
	 * Learning algorithm
	 */
	private LearningAlgorithm learningAlgorithm = null;

	/**
	 * ProbNet to learn.
	 */
	private ProbNet learnedNet = null;

	/**
	 * Structure that specifies use of model net
	 */
	private ModelNetUse modelNetUse;

	/**
	 * Case database
	 */
	private CaseDatabase caseDatabase = null;

	 // Constructor
	/**
	 * @param caseDatabase
	 * @param algorithmName <code>LearningAlgorithm</code> indicating the algorithm
	 *                      selected by the user.
	 * @param modelNet      <code>ProbNet</code> Net from which take the
	 *                      information of the nodes and links
	 * @param modelNetUse   <code>boolean[]</code> use the positions of the nodes,
	 *                      use also the initial links or use them fixed
	 * @throws EmptyModelNetException
	 * @throws UnobservedVariablesException
	 */
	public LearningManager(CaseDatabase caseDatabase, String algorithmName, ProbNet modelNet, ModelNetUse modelNetUse)
			throws EmptyModelNetException, UnobservedVariablesException {

		this.caseDatabase = caseDatabase;
		/* Check ModelNet is not null */
		if (modelNetUse != null && modelNetUse.isUseModelNet()) {
			if (modelNet == null) {
				throw new EmptyModelNetException();
			}
			this.learnedNet = applyModelNet(learningAlgorithmManager.getByName(algorithmName), caseDatabase, modelNet,
					modelNetUse);
		} else {
			this.learnedNet = new ProbNet();
			for (Variable variable : caseDatabase.getVariables()) {
				learnedNet.addNode(variable, NodeType.CHANCE);
			}
		}

		this.addElviraProperties(learnedNet);
		this.modelNetUse = modelNetUse;
	}

	public static Set<String> getAlgorithmNames() {

		return learningAlgorithmManager.getLearningAlgorithmNames();
	}
	
	public static Set<String> getDiscriminativeAlgorithmNames(){
		return learningAlgorithmManager.getDiscriminativeLearningAlgorithmNames().stream().sorted().collect(Collectors.toSet());
	}
	
	public static Set<String> getGenerativeAlgorithmNames(){
		return learningAlgorithmManager.getLearningAlgorithmNames()
									   .stream().filter(a-> !getDiscriminativeAlgorithmNames().contains(a)).collect(Collectors.toSet());
		
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
	 * @throws CannotNormalizeNullVectorException
	 */
	public void learn() throws CannotNormalizeNullVectorException {

		learningAlgorithm.run(modelNetUse);
	}

	/**
	 * Returns learned net
	 *
	 * @return <code>ProbNet</code> containing learned net
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
	 * Scores the associated network with the given edition.
	 *
	 * @param edit <code>PNEdit</code>
	 * @return <code>double</code> score of the net with the given edition
	 */
	public LearningEditMotivation getMotivation(PNEdit edit) {
		
		return learningAlgorithm.getMotivation(edit);
	}

	/**
	 * Retrieves the best edition suggested by the learning algorithm
	 *
	 * @param onlyAllowedEdits
	 * @param onlyPositiveEdits
	 */
	public LearningEditProposal getBestEdit(boolean onlyAllowedEdits, boolean onlyPositiveEdits) {

		return this.learningAlgorithm.getBestEdit(onlyAllowedEdits, onlyPositiveEdits);
	}

	/**
	 * Retrieves the next best edition suggested by the learning algorithm
	 *
	 * @param onlyAllowedEdits
	 * @param onlyPositiveEdits
	 */
	public LearningEditProposal getNextEdit(boolean onlyAllowedEdits, boolean onlyPositiveEdits) {

		return this.learningAlgorithm.getNextEdit(onlyAllowedEdits, onlyPositiveEdits);
	}

	/**
	 * Tells the learning algorithm to advance until the next phase
	 */
	public void goToNextPhase() {
		
		this.learningAlgorithm.runTillNextPhase();
	}

	/**
	 * Retrieves whether the LearningAlgorithm is in the last phase
	 */
	public boolean isLastPhase() {
		
		return this.learningAlgorithm.isLastPhase();
	}

	/**
	 * Applies the edit passed to the learnedNet and updates parameters
	 *
	 * @param edit
	 * @throws DoEditException
	 * @throws ConstraintViolationException
	 * @throws CannotNormalizeNullVectorException
	 */
	public void applyEdit(PNEdit edit)
			throws DoEditException, CannotNormalizeNullVectorException {
        
        edit.doEdit(this.learnedNet);
        learningAlgorithm.parametricLearning();
	}

	/**
	 * Adds elvira properties to the learned net.
	 *
	 * @param learnedNet <code>ProbNet</code> which receives the elvira
	 *                   properties.
	 */
	private void addElviraProperties(ProbNet learnedNet) {

		LinkedHashMap<String, String> newIO = learnedNet.additionalProperties;
		State[] defaultNodeStates = { new State("present"), new State("absent") };
		learnedNet.setDefaultStates(defaultNodeStates);
		newIO.put("hasElviraProperties", new String("yes"));
		learnedNet.additionalProperties = newIO;
	}

	/**
	 * Adds links and constraints depending on the structure of the model net
	 * and the option selected by the user.
	 *
	 * @param algorithmClass
	 * @param modelNetUse    use of the model net selected by the user.
	 * @param modelNet       structure of the net to add the constraints
	 * @throws UnobservedVariablesException
	 */
	private ProbNet applyModelNet(Class<? extends LearningAlgorithm> algorithmClass, CaseDatabase database,
			ProbNet modelNet, ModelNetUse modelNetUse) throws UnobservedVariablesException {

		ProbNet probNet = null;
		List<Variable> missingVariables = getMissingVariables(database.getVariables(), modelNet.getVariables());
		if (//!modelNetUse.isUseNodePositions() &&
				!algorithmClass.getAnnotation(LearningAlgorithmType.class).supportsUnobservedVariables()
						&& !missingVariables.isEmpty()) {
			List<Variable> latentVariables = new ArrayList<>(modelNet.getVariables());
			latentVariables.removeAll(database.getVariables());
			throw new UnobservedVariablesException(latentVariables);
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
			probNet.addConstraint(new ModelNetworkConstraint(modelNetUse, modelNet));
			adaptDatabaseToModelNet(database, modelNet);
		}

		return probNet;
	}

	/**
	 * Identifies and returns the list of variables that are present in the model network
	 * but missing from the database.
	 * @param databaseVariables List of variables present in the database
	 * @param modelNetVariables List of variables in the model network
	 * @return A list of variables that are in the model network but not in the database
	 */
	private List<Variable> getMissingVariables(List<Variable> databaseVariables, List<Variable> modelNetVariables) {

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

	public LearningAlgorithm getAlgorithmInstance(String name) {

		List<Object> parameters = new ArrayList<>();
		parameters.add(learnedNet);
		parameters.add(caseDatabase);
		return learningAlgorithmManager.getByName(name, parameters);
	}

	/**
	 * Blocks edit
	 *
	 * @param edit to block
	 */
	public void blockEdit(LearningEditProposal edit) {
		
		learningAlgorithm.blockEdit(edit);
	}

	/**
	 * Blocks edit
	 *
	 * @param edit to block
	 */
	public void unblockEdit(LearningEditProposal edit) {
		
		learningAlgorithm.unblockEdit(edit);
	}

	/**
	 * @return the blocked edits
	 */
	public List<LearningEditProposal> getBlockedEdits() {
		
		return learningAlgorithm.getBlockedEdits();
	}

	/**
	 * Given a modelNet, applies the node positions and the order of the
	 * states of the nodes of the modelNet to the nodes of the current probNet
	 *
	 * @param modelNet - the modelNet to copy the node positions from
	 */
	private void copyNodePositionsFromModelNet(ProbNet modelNet, ProbNet learntNet) {
		
		Node learntNetNode = null;

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
	 * Adapt case database to model network's variables
	 *
	 * @param database
	 * @param modelNet
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
	 * @param variableIndex index of the variable in the case database
	 * @param originalVariable the original variable whose states are to be mapped
	 * @param modelNetVariable the model network variable to which the states are mapped
	 */
	private void updateCases(int variableIndex, Variable originalVariable, Variable modelNetVariable) {
		
		State state;

		for (int j = 0; j < caseDatabase.getCases().length; j++) {
			state = originalVariable.getStates()[caseDatabase.getCases()[j][variableIndex]];
            int stateIndex = modelNetVariable.getStateIndex(state.getName());
            if(stateIndex == -1) continue;
            caseDatabase.getCases()[j][variableIndex] = stateIndex;
        }
	}

}
