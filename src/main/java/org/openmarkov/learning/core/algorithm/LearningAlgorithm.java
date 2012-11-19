/*
* Copyright 2011 CISIAD, UNED, Spain
*
* Licensed under the European Union Public Licence, version 1.1 (EUPL)
*
* Unless required by applicable law, this code is distributed
* on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
*/

package org.openmarkov.learning.core.algorithm;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.NormalizeNullVectorException;
import org.openmarkov.core.exception.NotEnoughMemoryException;
import org.openmarkov.core.io.database.CaseDatabase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNode;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.learning.core.editionsgenerator.EditionsGenerator;
import org.openmarkov.learning.core.editionsgenerator.LearningEditMotivation;
import org.openmarkov.learning.core.editionsgenerator.LearningEditProposal;
import org.openmarkov.learning.core.util.ModelNetUse;

/**
 * Abstract learning algorithm.
 */
public abstract class LearningAlgorithm {
    
    /** Edition generator */
    protected EditionsGenerator editionsGenerator;
    /** Parameter for the parametric learning. */
    protected double alpha;    
    
    /** Net to learn */
    protected ProbNet probNet;
    
    /** Case database */
    protected CaseDatabase caseDatabase;
    
    
    // Constructor
    /**
     * @param editionsGenerator <code>EditionsGenerator</code> The object that
     * gives the best operation in each iteration of the algorithm.
     **/
    public LearningAlgorithm (ProbNet probNet, CaseDatabase caseDatabase, EditionsGenerator editionsGenerator, double alpha)
    {
        this.probNet = probNet;
        this.caseDatabase = caseDatabase;
        this.editionsGenerator = editionsGenerator;
        this.alpha = alpha;
    }
    
    /** Method invoked to run the algorithm.
     * @param modelNetUse 
     * 
     * @return <code>ProbNet</code> learned.
     * @throws NotEnoughMemoryException
     * @throws NormalizeNullVectorException
     */
    public void run (ModelNetUse modelNetUse)
        throws NotEnoughMemoryException,
        NormalizeNullVectorException
    {
        init(modelNetUse);
        /* Main loop */
       LearningEditProposal bestEdition = editionsGenerator.getBest(true,true);
        while (bestEdition != null)
        {
            step (bestEdition.getEdition ());
            bestEdition = editionsGenerator.getBest (true, true);
        }
       /* Parametric Learning */
       parametricLearning();
    }
    
    /**
     * Init algorithm
     * @param modelNetUse 
     */
    public abstract void init (ModelNetUse modelNetUse);
    
    /**
     * Score the network. 
     * @param probNet
     * @param cases
     * @return <code>double</code> score of the net 
     */    
    public abstract double getScore (ProbNet probNet, int[][] cases);

    /**
     * Scores the associated network with the given edition.
     * @param probNet
     * @param cases
     * @param edit <code>PNEdit</code> 
     * @return <code>double</code> score of the net with the given edition
     */    
    public abstract LearningEditMotivation getMotivation (ProbNet probNet, int[][] cases, PNEdit edit); 
    
    /** Takes a step in the algorithm
     * 
     * @throws openmarkov.exceptions.NotEnoughMemoryException
     * @throws java.lang.Exception
     */
    private ProbNet step(PNEdit bestEdition) throws NotEnoughMemoryException, 
            NormalizeNullVectorException {

    /* If there have been any improvements on the score, we update
     * the learnedNet. */
        try{
            probNet.doEdit(bestEdition);
        } catch (ConstraintViolationException ex){
            /* If the edition was not allowed (ModelNetworkconstraint)
             * the algorithm just goes through the next iteration of the
             * loop, asking the cache for the next best edition.
             */
        }
        catch (Exception exception){
            exception.printStackTrace();
        }
        return probNet;
    }
            
    /**
     * This function creates the Potentials associated to each node,
     * normalizing the absolute frequencies of the configurations of 
     * the parents.
     * @throws openmarkov.exceptions.NotEnoughMemoryException
     * @throws NormalizeNullVectorException 
     */
    public ProbNet parametricLearning() 
            throws NotEnoughMemoryException, NormalizeNullVectorException{
        int[][] cases = caseDatabase.getCases ();
        TablePotential absoluteFrequencies;
        
        for (ProbNode node : probNet.getProbNodes()) {
            if(!node.getPotentials ().isEmpty ())
            {
                probNet.removePotential (node.getPotentials ().get (0));
            }
            absoluteFrequencies = calculateAbsoluteFrequencies(probNet, cases, node);
            for (int j = 0; j < absoluteFrequencies.getTableSize(); j++)
                absoluteFrequencies.values[j] += alpha;
            probNet.addPotential (DiscretePotentialOperations.normalize(absoluteFrequencies));
        }
        
        return probNet;
    }
    
    /**
     * Calculate the absolute frequencies in the database of each of the
     * configurations of the given node and its parents and a given extra
     * parent.
     * @param node <code>ProbNode</code> whose frequencies we want to calculate.
     * @return <code>TablePotential</code> with the absolute frequencies in
     * the database of each of the configurations of the given node and its
     * parents and a given extra parent.
     * @throws NotEnoughMemoryException
     */
    private TablePotential calculateAbsoluteFrequencies (ProbNet probNet,
                                                         int[][] cases,
                                                         ProbNode node)
        throws NotEnoughMemoryException
    {
        int parentsConfigurations = 1;
        int indexOfParent = 0;
        int numParents = node.getNode ().getNumParents ();
        int[] indexesOfParents = new int[numParents];
        ArrayList<Variable> variables = new ArrayList<Variable> ();
        variables.add ((Variable) node.getVariable ());
        if (numParents == 0)
        {
            parentsConfigurations = 1;
        }
        else
        {
            for (ProbNode parent : ProbNet.getProbNodesOfNodes (node.getNode ().getParents ()))
            {
                variables.add ((Variable) parent.getVariable ());
                indexesOfParents[indexOfParent] = probNet.getProbNodes ().indexOf (probNet.getProbNode (parent.getVariable ()));
                parentsConfigurations *= ((Variable) parent.getVariable ()).getNumStates ();
                indexOfParent++;
            }
        }
        return calculateAbsoluteFreqPotential (probNet,
                                               cases,
                                               node,
                                               parentsConfigurations,
                                               variables,
                                               indexesOfParents,
                                               node.getVariable ().getNumStates ());
    }
    
    /**
     * Calculate the absolute frequencies in the database of each of the
     * configurations of the given node and its parents.
     * @param probNode <code>ProbNode</code> whose frequencies we want to 
     * calculate.
     * @param parentsConfigurations product of the number of states of the
     * parents.
     * @param variables <code>ArrayList</code> formed by the variable associated
     * to the given node and the variables associated to its parents.
     * @param indexesOfParents <code>int[]</code> indexes of the parents in the
     * probNet list of nodes.
     * @param numValues number of states of the given node
     * @return <code>TablePotential</code> with the absolute frequencies in
     * the database of each of the configurations of the given node and its
     * parents.
     * @throws openmarkov.exceptions.NotEnoughMemoryException
     */
    private TablePotential calculateAbsoluteFreqPotential (ProbNet probNet,
                                                          int[][] cases,
                                                          ProbNode probNode,
                                                          int parentsConfigurations,
                                                          ArrayList<Variable> variables,
                                                          int[] indexesOfParents,
                                                          int numValues
                                                          )
            throws NotEnoughMemoryException {
        TablePotential absoluteFreqPotential = new TablePotential(
                variables, PotentialRole.CONDITIONAL_PROBABILITY);
        double[] absoluteFreqs = absoluteFreqPotential.getValues();
        double iCPT;
        int iNode = probNet.getProbNodes().indexOf(
                probNet.getProbNode(probNode.getVariable())); 

        // Initialize the table
        for (int i = 0; i < parentsConfigurations * numValues; i++) {
            absoluteFreqs[i] = 0;
        }
        
        variables.remove(0);
        // Compute the absolute frequencies
        for (int i = 0; i < cases.length; i++) {
            iCPT = 0;
            int j = 0;
            for (ProbNode parent : probNet.getProbNodes(variables)) {
                iCPT = iCPT * parent.getVariable().getNumStates() + cases[i][indexesOfParents[j]];
                j++;
            }
            absoluteFreqs[numValues * ((int) iCPT) + (int) cases[i][iNode]]++;
        }
        return absoluteFreqPotential;
    }

    /**
     * Returns best edition
     * @param onlyAllowedEdits
     * @param onlyPositiveEdits
     * @return
     */    
    public LearningEditProposal getBestEdition(boolean onlyAllowedEdits, boolean onlyPositiveEdits)
    {
    	return editionsGenerator.getBest(onlyAllowedEdits, onlyPositiveEdits);
    }
    
    /**
     * Returns next best edition
     * @param onlyAllowedEdits
     * @param onlyPositiveEdits
     * @return
     */    
    public LearningEditProposal getNextEdition(boolean onlyAllowedEdits, boolean onlyPositiveEdits)
    {
    	return editionsGenerator.getNext(onlyAllowedEdits, onlyPositiveEdits);    
    }
    
    /**
     * Blocks edit
     * @param edit to block
     */
    public void blockEdit(PNEdit edit)
    {
    	editionsGenerator.blockEdit(edit);
    }
    
    /**
     * Blocks edit
     * @param edit to block
     */
    public void unblockEdit(PNEdit edit)
    {
    	editionsGenerator.unblockEdit(edit);
    } 

	/**
	 * @return the blocked edits
	 */
	public List<PNEdit> getBlockedEdits() {
		return editionsGenerator.getBlockedEdits();
	}
}
