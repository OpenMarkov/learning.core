/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.algorithm;

import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.NormalizeNullVectorException;
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
import java.util.List;

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
	protected List<LearningEditProposal> blockedEdits = new ArrayList<LearningEditProposal>();

	protected int phase = 0;

	// Constructor
	public LearningAlgorithm(ProbNet probNet, CaseDatabase caseDatabase, double alpha) {
		this.probNet = probNet;
		this.caseDatabase = caseDatabase;
		this.alpha = alpha;
	}

	/**
	 * Method invoked to run the algorithm.
	 * @param modelNetUse
	 * @throws NormalizeNullVectorException
	 */
	public void run(ModelNetUse modelNetUse) throws NormalizeNullVectorException {
		init(modelNetUse);
		/* Main loop */
		LearningEditProposal bestEdition = getBestEdit(true, true);
		while (bestEdition != null) {
			step(bestEdition.getEdit());
			bestEdition = getBestEdit(true, true);
		}
		/* Parametric Learning */
		parametricLearning();
	}

	/**
	 * Tells the learning algorithm to advance until the next phase
	 */
	public void runTillNextPhase() throws NormalizeNullVectorException {
		int currentPhase = getPhase();
		LearningEditProposal bestEditProposal = getBestEdit(true, true);
		while ((bestEditProposal != null) && (currentPhase == getPhase())) {
			step(bestEditProposal.getEdit());
			bestEditProposal = getBestEdit(true, true);
		}
	}

	/**
	 * Initializes the algorithm
	 *
	 * @param modelNetUse
	 */
	public void init(ModelNetUse modelNetUse) {
		// Do nothing
	}

	/**
	 * This method returns the best edition (and its associated score)
	 * that can be done to the network that is being learnt.
	 *
	 * @param onlyAllowedEdits  If this parameter is true, only those editions
	 *                          that do not provoke a ConstraintViolationException are returned
	 * @param onlyPositiveEdits If this parameter is true, only those
	 *                          editions with a positive associated score are returned.
	 * @return <code>LearningEditProposal</code> with the best edition and its score.
	 */
	public abstract LearningEditProposal getBestEdit(boolean onlyAllowedEdits, boolean onlyPositiveEdits);

	/**
	 * This method returns the next best edition (and its associated score)
	 * that can be done to the network that is being learnt.
	 *
	 * @param onlyAllowedEdits  If this parameter is true, only those editions
	 *                          that do not provoke a ConstraintViolationException are returned
	 * @param onlyPositiveEdits If this parameter is true, only those
	 *                          editions with a positive associated score are returned.
	 * @return <code>LearningEditProposal</code> with the best edition and its score.
	 */
	public LearningEditProposal getNextEdit(boolean onlyAllowedEdits, boolean onlyPositiveEdits) {
		return null;
	}

	/**
	 * Calculates the score associated to the given edit.
	 *
	 * @param edit <code>PNEdit</code>
	 * @return <code>LearningEditMotivation</code> motivation for the given edit
	 */
	public LearningEditMotivation getMotivation(PNEdit edit) {
		return null;
	}

	/**
	 * Takes a step in the algorithm
	 */
	protected ProbNet step(PNEdit bestEdition) throws NormalizeNullVectorException {

		/* If there have been any improvements on the score, we update
		 * the learnedNet. */
		try {
			probNet.doEdit(bestEdition);
		} catch (ConstraintViolationException ex) {
			/* If the edition was not allowed (ModelNetworkconstraint)
			 * the algorithm just goes through the next iteration of the
			 * loop, asking the cache for the next best edition.
			 */
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return probNet;
	}

	/**
	 * This function creates the Potentials associated to each node,
	 * normalizing the absolute frequencies of the configurations of
	 * the parents.
	 *
	 * @throws NormalizeNullVectorException
	 */
	public ProbNet parametricLearning() throws NormalizeNullVectorException {

		for (Node node : probNet.getNodes()) {
			if (!node.getPotentials().isEmpty()) {
				probNet.removePotential(node.getPotentials().get(0));
			}
			TablePotential absoluteFrequencies = getAbsoluteFrequencies(caseDatabase, node);
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

	protected boolean isAllowed(PNEdit edit) {
		boolean isAllowed = true;
		try {
			//Announce edit to check whether it is allowed or not
			try {
				edit.getProbNet().getPNESupport().announceEdit(edit);
			} catch (ConstraintViolationException e) {
				isAllowed = false;
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return isAllowed;
	}

	public int getPhase() {
		return phase;
	}

	/**
	 * Retrieves whether the LearningAlgorithm is in the last phase.
	 * True by default
	 */
	public boolean isLastPhase() {
		return true;
	}

	/**
	 * Calculate the absolute frequencies in the database of each of the
	 * configurations of the given node and its parents.
	 *
	 * @param caseDatabase database of cases
	 * @param node         <code>Node</code> whose frequencies we want to
	 *                     calculate.
	 * @return <code>TablePotential</code> with the absolute frequencies in
	 * the database of each of the configurations of the given node and its
	 * parents.
	 */
	private TablePotential getAbsoluteFrequencies(CaseDatabase caseDatabase, Node node) {
		Variable variable = node.getVariable();
		List<Node> parents = node.getParents();
		int numParents = parents.size();
		int[] indexesOfParents = new int[numParents];
		int[] parentsStateNum = new int[numParents];
		List<Variable> variables = new ArrayList<Variable>();
		variables.add(variable);
		if (!parents.isEmpty()) {
			int indexOfParent = 0;
			for (Node parent : parents) {
				Variable parentVariable = parent.getVariable();
				variables.add(parentVariable);
				indexesOfParents[indexOfParent] = caseDatabase.getVariables().indexOf(parentVariable);
				parentsStateNum[indexOfParent] = parentVariable.getNumStates();
				indexOfParent++;
			}
		}

		int numValues = variable.getNumStates();
		TablePotential absoluteFreqPotential = new TablePotential(variables, PotentialRole.CONDITIONAL_PROBABILITY);
		double[] absoluteFreqs = absoluteFreqPotential.getValues();
		int iNode = caseDatabase.getVariables().indexOf(variable);

		// Initialize the table
		for (int i = 0; i < absoluteFreqs.length; i++) {
			absoluteFreqs[i] = 0;
		}

		variables.remove(0);
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
