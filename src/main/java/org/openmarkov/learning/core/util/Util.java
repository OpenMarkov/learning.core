/*
 * Copyright (c) CISIAD, UNED, Spain,  2018. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.util;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.io.database.CaseDatabase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * Calculates frequencies
 * @author Iñigo
 *
 */
public class Util
{
    /**
     * Calculate the absolute frequencies in the database of each of the
     * configurations of the given node and its parents.
     * @param node <code>Node</code> whose frequencies we want to 
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
     */
	private static TablePotential getAbsoluteFrequencies(ProbNet probNet,
			CaseDatabase caseDatabase, Node node, List<Variable> variables)
	{
        int parentsConfigurations = 1;
        int numValues = node.getVariable ().getNumStates ();
        // We miss the first one as it is the node itself, not one of its parents
        int[] indexesOfParents = new int[variables.size () -1];
        for (int i = 0; i < indexesOfParents.length; ++i)
        {
            indexesOfParents[i] = caseDatabase.getVariables().indexOf (variables.get (i + 1));
            parentsConfigurations *= variables.get (i + 1).getNumStates ();
        }
        TablePotential absoluteFreqPotential = new TablePotential (
                                                        variables,
                                                        PotentialRole.CONDITIONAL_PROBABILITY);
        double[] absoluteFreqs = absoluteFreqPotential.getValues ();
        // Initialize the table
        for (int i = 0; i < parentsConfigurations * numValues; i++)
        {
            absoluteFreqs[i] = 0;
        }
        variables.remove (0);
        // Compute the absolute frequencies
        int iCPT;
        int iParent, iNode = caseDatabase.getVariables().indexOf (node.getVariable ());
        if(iNode == -1)
        	System.out.println("fdx");
        int[][] cases = caseDatabase.getCases();
        List<Node> nodes = probNet.getNodes (variables);
        for (int i = 0; i < cases.length; i++)
        {
            iCPT = 0;
            for (int j = 0; j < nodes.size (); ++j)
            {
                iParent = indexesOfParents[j];
                iCPT = iCPT * nodes.get (j).getVariable ().getNumStates ()
                       + cases[i][iParent];
            }
            if(numValues * iCPT + cases[i][iNode] >= absoluteFreqs.length)
            	System.out.println("fdx");
            	
            absoluteFreqs[numValues * iCPT + cases[i][iNode]]++;
        }
        return absoluteFreqPotential;
    }

    
    /**
     * Calculate the absolute frequencies in the database of each of the
     * configurations of the given node and its parents and a given extra
     * parent.
     * @param node <code>Node</code> whose frequencies we want to calculate.
     * @param extraParent <code>Node</code>
     * @return <code>TablePotential</code> with the absolute frequencies in the
     *         database of each of the configurations of the given node and its
     *         parents and a given extra parent.
     */
    public static TablePotential getAbsoluteFreq (ProbNet probNet,
    		CaseDatabase caseDatabase,
                                                  Node node)
    {
        List<Variable> variables = new ArrayList<Variable> ();
        variables.add ((Variable) node.getVariable ());
        for (Node parent : node.getParents ())
        {
            variables.add ((Variable) parent.getVariable ());
        }
        return getAbsoluteFrequencies (probNet, caseDatabase, node, variables);
    }    
    /**
     * Calculate the absolute frequencies in the database of each of the
     * configurations of the given node and its parents and a given extra
     * parent.
     * @param node <code>Node</code> whose frequencies we want to calculate.
     * @param extraParent <code>Node</code>
     * @return <code>TablePotential</code> with the absolute frequencies in the
     *         database of each of the configurations of the given node and its
     *         parents and a given extra parent.
     */
	public static TablePotential getAbsoluteFreqExtraParent(ProbNet probNet,
			CaseDatabase caseDatabase, Node node, Node extraParent) 
	{
        List<Variable> variables = new ArrayList<Variable> ();
        variables.add ((Variable) node.getVariable ());
        
        for (Node parent : node.getParents ())
        {
            if(!variables.contains (parent.getVariable ()))
                variables.add (parent.getVariable ());
        }
        if ((extraParent != null) && (!variables.contains (extraParent.getVariable ())))
        {
            variables.add (extraParent.getVariable ());
        }
        return getAbsoluteFrequencies (probNet, caseDatabase, node, variables);
    }

    /**
     * Calculate the absolute frequencies in the database of each of the
     * configurations of the given node and its parents except one.
     * @param node <code>Node</code> whose frequencies we want to calculate.
     * @param removedParent <code>Node</code> that we do not want to include
     * in the calculations
     * @return <code>TablePotential</code> with the absolute frequencies in
     * the database of each of the configurations of the given node and its
     * parents except one.
     */
	public static TablePotential getAbsoluteFreqRemovingParent(ProbNet probNet,
			CaseDatabase caseDatabase, Node node, Node removedParent)
    {
        List<Variable> variables = new ArrayList<Variable> ();
        variables.add (node.getVariable ());
        
        List<Node> parents = node.getParents ();
        for (Node parent : parents)
        {
            if (parent.getVariable () != removedParent.getVariable ())
            {
                variables.add (parent.getVariable ());
            }
        }
        return getAbsoluteFrequencies (probNet, caseDatabase, node, variables);
    }
    
    
}
