/*
* Copyright 2011 CISIAD, UNED, Spain
*
* Licensed under the European Union Public Licence, version 1.1 (EUPL)
*
* Unless required by applicable law, this code is distributed
* on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
*/


package org.openmarkov.learning.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.CanNotDoEditException;
import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.NormalizeNullVectorException;
import org.openmarkov.core.exception.NotEnoughMemoryException;
import org.openmarkov.core.exception.ProbNodeNotFoundException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.io.database.CaseDatabase;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNode;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.learning.core.algorithm.LearningAlgorithm;
import org.openmarkov.learning.core.algorithm.annotation.LearningAlgorithmManager;
import org.openmarkov.learning.core.constraint.ModelNetworkConstraint;
import org.openmarkov.learning.core.editionsgenerator.EditAndScorePair;
import org.openmarkov.learning.core.exception.EmptyModelNetException;
import org.openmarkov.learning.core.util.ModelNetUse;

/** This class launches the learning algorithm and receives the results of
 * the learning.
 * @author joliva
 * @author manuel
 * @author fjdiez
 * @version 1.0
 * @since OpenMarkov 1.0 */
public class LearningManager {
    
    /**  Learning algorithm */
    private LearningAlgorithm learningAlgorithm = null;    
    
    /** Implemented independence tester. */
    public static final String[] independenceTesters = {"Entropía cruzada"};

    /** ProbNet to learn. */
    private ProbNet learnedNet = null;
    
    /** Structure that specifies use of model net */
    private ModelNetUse modelNetUse;
    
    /** Case database */
    private CaseDatabase caseDatabase = null;

    
    /**
     * Constructor
     * @param preprocessedNet <code>ProbNet</code> Net with the variables of
     * interest after preprocessing.
     * @param algorithm <code>LearningAlgorithm</code> indicating the algorithm
     *            selected by the user.
     * @param modelNet <code>ProbNet</code> Net from which take the
     *            information of the nodes and links
     * @param modelNetUse <code>boolean[]</code> use the positions of the nodes,
     *            use also the initial links or use them fixed
     * @throws NormalizeNullVectorException
     * @throws EmptyModelNetException
     * @throws ProbNodeNotFoundException
     * @throws NodeNotFoundException
     * @throws NotEnoughMemoryException
     */
    public LearningManager (ProbNet preprocessedNet,
                            CaseDatabase caseDatabase,
                            String algorithmName,
                            List<Object> parameters, 
                            ProbNet modelNet,
                            ModelNetUse modelNetUse
                            )
        throws NormalizeNullVectorException,
        EmptyModelNetException,
        NodeNotFoundException,
        ProbNodeNotFoundException,
        NotEnoughMemoryException
    {
        LearningAlgorithmManager learningAlgorithmManager = new LearningAlgorithmManager ();
        this.caseDatabase = caseDatabase;
        /* Maybe there's no modelNet to work with */
        if ((modelNetUse.isUseModelNet ()))
        {
            if (modelNet == null)
            {
                throw new EmptyModelNetException ();
            }
            this.learnedNet = applyModelNet (preprocessedNet, modelNet, modelNetUse);
        }
        else
        {
            this.learnedNet = preprocessedNet;
        }     
        parameters.add (0, learnedNet);
        parameters.add (1, caseDatabase.getCases ());
        this.learningAlgorithm = learningAlgorithmManager.getByName (algorithmName, parameters);
        this.addElviraProperties (learnedNet);
        this.modelNetUse = modelNetUse;
    }  

    /**
     * Initialize the learning algorithm.
     */
    public void init ()
    {
        learningAlgorithm.init (modelNetUse);
    }

    /**
     * Main method to launch the learning process.
     * @throws NotEnoughMemoryException
     * @throws NodeNotFoundException
     * @throws NormalizeNullVectorException
     * @throws ProbNodeNotFoundException
     */
    public void learn ()
        throws NotEnoughMemoryException,
        NodeNotFoundException,
        NormalizeNullVectorException,
        ProbNodeNotFoundException
    {
        learningAlgorithm.run (modelNetUse);
    }

    /**
     * Returns learned net
     * @return <code>ProbNet</code> containing learned net
     */
	public ProbNet getLearnedNet() {
		return this.learnedNet;
	}
	
    /**
     * Score of the associated network. 
     * @return <code>double</code> score of the net 
     */
    public double getScore()  {
			return learningAlgorithm.getScore(this.learnedNet, this.caseDatabase.getCases ());
    }
    
    /**
     * Scores the associated network with the given edition.
     * @param edit <code>PNEdit</code> 
     * @return <code>double</code> score of the net with the given edition
     */
    public double getScore(PNEdit edit)  {
        return learningAlgorithm.getScore (this.learnedNet, this.caseDatabase.getCases (), edit);
    }
    
    /**
     * Retrieves the best edition suggested by the learning algorithm
     * @param onlyAllowedEdits
     * @param onlyPositiveEdits
     */
    public EditAndScorePair getBestEdition (boolean onlyAllowedEdits,
                                 boolean onlyPositiveEdits)
    {
        
        return this.learningAlgorithm.getBestEdition (onlyAllowedEdits,
                                                       onlyPositiveEdits);        
    }
    
    /**
     * Retrieves the next best edition suggested by the learning algorithm
     * @param onlyAllowedEdits
     * @param onlyPositiveEdits
     */
    public EditAndScorePair getNextEdition (boolean onlyAllowedEdits,
                                 boolean onlyPositiveEdits)
    {
        
        return this.learningAlgorithm.getNextEdition (onlyAllowedEdits,
                                                       onlyPositiveEdits);        
    }
    
    /**
     *  Applies the edit passed to the learnedNet and updates parameters
     * @param edit
     * @throws DoEditException 
     * @throws WrongCriterionException 
     * @throws NonProjectablePotentialException 
     * @throws CanNotDoEditException 
     * @throws ConstraintViolationException 
     * @throws NotEnoughMemoryException 
     * @throws NormalizeNullVectorException 
     */
    public void applyEdit (PNEdit edit)
        throws NotEnoughMemoryException,
        ConstraintViolationException,
        CanNotDoEditException,
        NonProjectablePotentialException,
        WrongCriterionException,
        DoEditException, NormalizeNullVectorException
    {
        this.learnedNet.doEdit (edit);
        learningAlgorithm.parametricLearning ();
    }
    
    /**
     * Adds elvira properties to the learned net.
     * @param learnedNet <code>ProbNet</code> which receives the elvira
     * properties.
     */
    private void addElviraProperties(ProbNet learnedNet) 
    {
                                
        HashMap<String, String> newIO = learnedNet.additionalProperties;
        State[] defaultNodeStates = {new State("present"), new State("absent")};
        learnedNet.setDefaultStates(defaultNodeStates);
        newIO.put("hasElviraProperties", new String("yes"));
        learnedNet.additionalProperties = newIO;
    }
    
    /**
     * Adds links and constraints depending on the structure of the model net
     * and the option selected by the user.
     * @param modelNetUse use of the model net selected by the user.
     * @param modelNet structure of the net to add the constraints
     * @throws ProbNodeNotFoundException
     * @throws NodeNotFoundException
     */
    private ProbNet applyModelNet (ProbNet learnedNet,
                                   ProbNet modelNet,
                                   ModelNetUse modelNetUse)
        throws ProbNodeNotFoundException,
        NodeNotFoundException
    {
        /*
         * If the option "Use only nodes" is not selected, we add the links of
         * the model net to the learnedNet we are going to learn.
         */
        if (modelNet != null && !modelNetUse.isOnlyUseNodes ())
        {
            // If the model net includes nodes/variables that are not in the database, add them along with their potentials 
            for (Variable modelNetVariable : modelNet.getVariables ())
            {
                ProbNode modelNetNode = modelNet.getProbNode (modelNetVariable);
                if(!learnedNet.containsVariable (modelNetVariable.getName ()))
                {
                    ProbNode newNode = learnedNet.addVariable (modelNetVariable,
                                                               modelNetNode.getNodeType ());
                    newNode.setPotentials (modelNetNode.getPotentials ());
                }
            }
            
            for (Link link : modelNet.getGraph ().getLinks ())
            {
                learnedNet.addLink (learnedNet.getVariable (((ProbNode) link.getNode1 ().getObject ()).getVariable ().getName ()),
                                    learnedNet.getVariable (((ProbNode) link.getNode2 ().getObject ()).getVariable ().getName ()),
                                    link.isDirected ());
            }
        }
        // ModelNetworkConstraint
        try
        {
            learnedNet.addConstraint (new ModelNetworkConstraint (modelNetUse,
                                                                  modelNet),
                                      false);
        }
        catch (ConstraintViolationException e)
        {
        }
        return learnedNet;
    }
    
    public static Set<String> getAlgorithmNames ()
    {
        LearningAlgorithmManager learningAlgorithmManager = new LearningAlgorithmManager ();
        
        return learningAlgorithmManager.getLearningAlgorithmNames ();
    }    
    
    /**
     * Blocks edit
     * @param edit to block
     */
    public void blockEdit(PNEdit edit)
    {
    	learningAlgorithm.blockEdit(edit);
    }
    
    /**
     * Blocks edit
     * @param edit to block
     */
    public void unblockEdit(PNEdit edit)
    {
    	learningAlgorithm.unblockEdit(edit);
    }
    
	/**
	 * @return the blocked edits
	 */
	public ArrayList<PNEdit> getBlockedEdits() {
		return learningAlgorithm.getBlockedEdits();
	}      
	
}
