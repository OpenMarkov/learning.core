/*
* Copyright 2011 CISIAD, UNED, Spain
*
* Licensed under the European Union Public Licence, version 1.1 (EUPL)
*
* Unless required by applicable law, this code is distributed
* on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
*/


package org.openmarkov.learning.core.preprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NotEnoughMemoryException;
import org.openmarkov.core.exception.ProbNodeNotFoundException;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNode;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;

/** This class implements the routines to manage absent values in a database. 
 * @author joliva
 * @author manuel      
 * @author fjdiez          
 * @version 1.0
 * @since OpenMarkov 1.0 */
public class AbsentValues {

    /* Options to manage absent values*/
	// TODO Internacionalizar
    private static final String[] options = {"Mantener ausentes", 
                "Eliminar registros"};
    public static final String defaultOption = "Especificar para cada variable";
    public static final int INPUT = 0;
    public static final int ELIMINATE = 1;
   
    static ProbNet probNet = null;
    
    /** IONet with the new IONodes constructed */
    static HashMap<String, String> newIONet = new HashMap<String, String>();
    
    /** database cases adaptated after preprocessing */
    static int[][] newCases = null;
    
    /** 
     * This function makes all the preprocessing related to absent values. It 
     * removes some states of some variables if required and adapt the cases in
     * the database (removing some of them if necessary).
     * 
     * @param variables <code>ArrayList</code> variables to preprocess
     * @param preprocessOption <code>ArrayList</code> containing the preprocess
     * option selected for each variable
     * @param oldProbNet <code>ProbNet</code> original probNet
     * @param cases <code>int[][]</code> original database cases.
     * @throws openmarkov.exceptions.NotEnoughMemoryException
     * @throws ProbNodeNotFoundException 
     * @throws InvalidStateException 
     */
    public static ProbNet preprocess(ArrayList<Variable> variables, 
            ArrayList<Integer> preprocessOption, ProbNet oldProbNet, 
            int[][] cases) throws NotEnoughMemoryException, 
            ProbNodeNotFoundException, InvalidStateException{
        
        newCases = new int[cases.length][oldProbNet.getNumNodes()];
        ArrayList<Integer> variableIndex = new ArrayList<Integer>();
        ArrayList<Variable> probNetVariables = oldProbNet.
                getVariables(NodeType.CHANCE);
        
        for(int i = 0; i < cases.length; i++)
            for (int j = 0; j < cases[i].length; j++)
                newCases[i][j] = cases[i][j];
        
        if ((variables == null) || (variables.size() == 0))
            return null;
        
        for (Variable var : variables){
            variableIndex.add(probNetVariables.indexOf(var));
        }
        
        probNet = oldProbNet.copy();
        
        //remove the "?" state
        statesElimination(preprocessOption, variables, oldProbNet);
        
        /*Remove the cases with an absent value, if this option was selected.
         * If not, we have to put the correct value (remember we have removed
         * the "?" state) */
        for (int i = 0; i < newCases.length; i++){
            if(!verifyCasesElimination(preprocessOption, variables, 
                    variableIndex, newCases[i]))
                newCases[i]=null;
            else
                for(int j = 0; j < probNet.getNumNodes(); j++){
                    Variable var = probNet.getVariables(NodeType.CHANCE).get(j);
                    newCases[i][j] = var.getStateIndex(oldProbNet.getProbNode
                            (var.getName()).getVariable().
                            getStateName(newCases[i][j]));
                }
        }
        
        //update IONet
        for (Entry<String, String> property : 
        		(Set<Entry<String, String>>)oldProbNet.additionalProperties
                	.entrySet())
            newIONet.put((String)property.getKey(), property.getValue().
            		toString());
        
        probNet.additionalProperties = newIONet;
        return probNet;
    }
        
    /**
     * This function verifies whether the case must be eliminated or not.
     * @param preprocessOption <code>ArrayList</code> containing the preprocess
     * option selected for each variable
     * @param variables <code>ArrayList</code> variables to preprocess
     * @param variableIndex <code>ArrayList</code> indexes of the variables
     * @param example <code>int[]</code> example which we want to verify
     * @return <code>boolean</code> true if the case must be eliminated
     */
    private static boolean verifyCasesElimination
            (ArrayList<Integer> preprocessOption,
            ArrayList<Variable> variables, ArrayList<Integer> variableIndex,
            int[] example){

        int caseValue;
        int i = 0;
        
        for(Integer option : preprocessOption){
            caseValue = example[variableIndex.get(i)];
            if ((option == ELIMINATE) && (( caseValue == -1) || 
                    (variables.get(i).getStateName(caseValue).equals("?"))))
                return false;
            i++;
        }
        
        return true;
        
    }
    
    /**
     * This function remove the "?" of each variable whose preprocessOption
     * is ELIMINATE
     * @param preprocessOption <code>ArrayList</code> preprocess option for each
     * variable
     * @param variables <code>ArrayList</code> of variables
     * @param oldProbNet <code>ProbNet</code> original probNet
     * @throws ProbNodeNotFoundException 
     * @throws java.lang.Exception
     */
    private static void statesElimination(ArrayList<Integer> preprocessOption,
            ArrayList<Variable> variables, ProbNet oldProbNet) throws 
            ProbNodeNotFoundException{
        Variable oldVariable, newVariable;    
        State[] newStates;
        int i = 0;
        ProbNode newNode;
        HashMap<String, String> ioNode;
        //ProbNet probNet = oldProbNet.copy();
        
        for(Integer option : preprocessOption){
            oldVariable = variables.get(i);
            ioNode = oldProbNet.getProbNode(oldVariable.getName()).
            	additionalProperties;
            if (option == ELIMINATE){
                newStates = newStates(oldVariable.getStates());
                probNet.removeProbNode(probNet.getProbNode(oldVariable));
                newVariable = new Variable(oldVariable.getName(),
                    newStates); 
                newNode = probNet.addVariable(newVariable, NodeType.CHANCE);
                newNode.additionalProperties = ioNode;
            }
            else{
            	probNet.removeProbNode(probNet.getProbNode(oldVariable));
                newVariable = new Variable(oldVariable.getName(),
                   oldVariable.getStates()); 
                newNode = probNet.addVariable(newVariable, NodeType.CHANCE);
                newNode.additionalProperties = ioNode;
            }
            i++;
        }
    }
    
    /**
     * This function removes the "?" state
     * @param states <code>String[]</code> original states
     * @return <code>String[]</code> original states without "?"
     */
    private static State[] newStates(State[] states){
        ArrayList<State> newStates = new ArrayList<State>();
        State[] statesAux = new State[states.length - 1];
        
        for (int i= 0; i < states.length; i++){
            if(!states[i].getName().equals("?")){
                newStates.add(states[i]);
            }
        }
        
        return newStates.toArray(statesAux);
    }
    
    public static ProbNet getProbNet(){
        return probNet;
    }
    
    public static int[][] getNewCases(){
        return newCases;
    }
    
    public static String[] getOptions(){
        return options;
    }
    
}
