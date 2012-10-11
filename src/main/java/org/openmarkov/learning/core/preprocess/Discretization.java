/*
* Copyright 2012 CISIAD, UNED, Spain
*
* Licensed under the European Union Public Licence, version 1.1 (EUPL)
*
* Unless required by applicable law, this code is distributed
* on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
*/


package org.openmarkov.learning.core.preprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NotEnoughMemoryException;
import org.openmarkov.core.exception.ProbNodeNotFoundException;
import org.openmarkov.core.io.database.CaseDatabase;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNode;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.learning.core.preprocess.exception.WrongDiscretizationLimitException;
import org.openmarkov.learning.core.util.Util;

/** This class implements the routines to manage the discretization of the
 * variables.
 * @author joliva
 * @author manuel
 * @author fjdiez
 * @author ibermejo
 * @version 1.0
 * @since OpenMarkov 1.0 */
public class Discretization {

    public enum Option {
        NOTHING,
        MODELNET,
        EQUAL_FREQ,
        EQUAL_WIDTH;
    }

    /**
     * Global number of intervals
     */
    private int numIntervals;

    /**
     * Discretization options
     */
    private Map<Variable, Option> discretizeOptions = null;

    /**
     * Num intervals per variable
     */
    private Map<Variable, Integer> numIntervalsPerVariable = null;
    
    /**
     * Constructor for Discretization.
     * @param numIntervals
     */
    public Discretization (int numIntervals)
    {
        this.numIntervals = numIntervals;
    }

    /**
     * 
     * Constructor for Discretization.
     * @param discretizeTreatments
     */
    public Discretization (Map<Variable, Option> discretizeOptions, Map<Variable, Integer> numIntervalsPerVariable)
    {
        this.discretizeOptions = discretizeOptions;
        this.numIntervalsPerVariable = numIntervalsPerVariable;
    }

    /**
     * This function takes a set of states and determines whether they are
     * numeric or not
     * @param states <code>String[]</code> states to verify
     * @param index <code>int</code> variable index
     * @return true if the states are numeric
     */
    public static boolean isNumeric(State[] states){

        boolean hasMissingValues = false;
        for (int i = 0; i < states.length; i++)
        {
            try
            {
                if (!states[i].getName ().equals ("?"))
                {
                    Double.parseDouble (states[i].getName ());
                }else
                {
                    hasMissingValues = true;
                }
            }
            catch (NumberFormatException e)
            {
                return false;
            }
        }

        return states.length > 4 || (!hasMissingValues && states.length == 3);
    }

    /**
     * This function discretizes the database.
     *
     * @return <code>CaseDatabase</code> updated database
     */
    public CaseDatabase discretize (CaseDatabase database,
                                    Map<Variable, MissingValues.Option> preprocessOptions,
                                    ProbNet modelNet)
        throws NotEnoughMemoryException,
        InvalidStateException,
        ProbNodeNotFoundException,
        WrongDiscretizationLimitException
    {

        List<Variable> newVariables = new ArrayList<> (); 
                
        for (Variable variable : database.getVariables()){
            Variable newVariable = null;
                int numIntervals = (numIntervalsPerVariable != null)? numIntervalsPerVariable.get (variable) : this.numIntervals;
                switch(discretizeOptions.get (variable)){
                    case EQUAL_WIDTH:
                        newVariable = discretizeEqualWidth(variable, numIntervals);
                        break;
                    case EQUAL_FREQ:
                        newVariable = discretizeEqualFreq(variable, numIntervals);
                        break;
                    case MODELNET:
                        newVariable = discretizeFromModelNet(variable, modelNet);
                        break;
                    default:
                        newVariable = variable;
                        break;
                }
        }

        /* construct the new cases array */
        int[][] newCases = discretizeCases(database, newVariables);

        return new CaseDatabase (newVariables, newCases);
    }

     /**
      * This function updates the database cases to adapt them to the new
      * states of the discretized variables.
      * @param cases <code>int[][]</code> original database cases
      * @param oldProbNet <code>ProbNet</code> original probNet
      * @param discretizeOption <code>ArrayList</code> discretization option
      * selected for each variable.
     * @throws ProbNodeNotFoundException 
     * @throws WrongDiscretizationLimitException 
      */
    private int[][] discretizeCases (int[][] cases, ProbNet oldProbNet, List<Integer> discretizeOption)
        throws ProbNodeNotFoundException,
        WrongDiscretizationLimitException
    {
        int index;
        String value = "";
        int countCases = 0;
        double[] intervals;
        Double doubleValue;
        Variable newVariable;
        int[][] casesAux = new int[cases.length][probNet.getNumNodes()];

        for(int i = 0; i < cases.length; i++){
            if (cases[i] != null){
                for(int j = 0; j < probNet.getNumNodes(); j++){
                    newVariable = probNet.getProbNodes().get(j).getVariable();
                    /* index is the index of the variable in the old net, and
                     * j is the index in the new net */
                    index = oldProbNet.getVariables(NodeType.CHANCE).
                            indexOf(oldProbNet.getVariable(probNet.
                            getProbNodes().get(j).getVariable().getName()));
                    if (isNumeric[index]){
                        switch(discretizeOption.get(index)){
                            case (NOTHING):
                                casesAux[countCases][j] = cases[i][index];
                                break;
                            default:
                                value = ((State[])oldProbNet.getProbNode(
                                        probNet.getProbNodes().get(j).
                                        getVariable().getName()).getVariable().
                                            getStates())
                                        [cases[i][index]].getName();
                                doubleValue = Double.parseDouble(value);
                                intervals = newVariable.
                                        getPartitionedInterval().
                                        getLimits();
                                /* If the minimum is under the left limit
                                 * of the first interval, or the maximum
                                 * is greater than the right limit of the
                                 * last interval, there's an error in the
                                 * discretization (maybe due to a bad
                                 * model net).*/
                                if((min[index] < intervals[0]) || (max[index] > intervals[intervals.length-1]))
                                    throw new WrongDiscretizationLimitException();
                                

                                /*We search for the interval in which the
                                 * value is contained */
                                for (int k = 1; k < intervals.length; k++){
                                    if (doubleValue < intervals[k]){
                                        casesAux[countCases][j] = k - 1;
                                        k = intervals.length;
                                    }
                                    else if (doubleValue == intervals[k]){
                                        if (newVariable.
                                                getPartitionedInterval().
                                                getBelongsToLeftSide()[k]){
                                            casesAux[countCases][j] = k - 1;
                                            k = intervals.length;
                                        }
                                        else{
                                            casesAux[countCases][j] = k;
                                            k = intervals.length;
                                        }
                                    }
                                }
                                break;
                        }
                    }
                    else
                        casesAux[countCases][j] = cases[i][index];
                }
                countCases++;
            }
        }

        newCases = new int[countCases][probNet.getNumNodes()];
        for (int i = 0; i < countCases; i++){
            if (casesAux[i] != null)
                for (int j = 0; j < probNet.getNumNodes(); j++){
                    newCases[i][j] = casesAux[i][j];
                }
        }
    }

    /**
     * This function makes the discretization of a variable taking the
     * information from a model net
     * @param oldVariable <code>Variable</code> variable to discretize
     * @param modelNet <code>ProbNet</code> net from which to tak the
     * information of the discretization
     * @param oldProbNet <code>ProbNet</code> original probNet
     * @throws java.lang.Exception
     */
    private Variable discretizeFromModelNet (Variable oldVariable,
                                                ProbNet modelNet)
        throws ProbNodeNotFoundException
    {
        
        Variable newVariable = oldVariable;

        if (modelNet != null){
            Variable modelNetVariable = modelNet.getVariable(oldVariable.getName());
            if (modelNetVariable.getVariableType() == VariableType.DISCRETIZED){
                newVariable = modelNetVariable;
            }
        }
        
        return newVariable;
    }


    public Variable discretizeEqualWidth (Variable variable,
                                             int numInterval)
    {
        Variable newVariable = null;
        
        int numStates = numInterval;
        State[] states = new State[numStates];
        boolean[] belongsToLeftSide;
        double[] limits;

        //Create a new discretized variable
        if((hasMissingValues[oldProbNet.getVariables(NodeType.CHANCE).
                indexOf(oldVariable)]) &&
                (ausentValOp != MissingValues.ELIMINATE)){
            newVariable = new Variable(oldVariable.getName(), numStates + 1);
            try {
                newVariable.renameState("" + numStates, "?");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else{
            belongsToLeftSide = new boolean[numStates+1];
            limits = new double[numStates + 1];
            double step = (max[index] - min[index]) / (double)numStates;
            for (int i = 0; i < numStates; i++){
                states[i] = new State(new String("[" + (min[index] + (i * step))
                        + " - " + (min[index] + ((i + 1) * step)) + ")"));
                belongsToLeftSide[i] = true;
                limits[i] = min[index] + (i * step);
            }
            //close the last interval
            states[numStates - 1] = new State(states[numStates - 1].getName().
                    replace(')', ']'));
            limits[limits.length - 1] = max[index];
            //Minimum and Maximum must be in the interval
            belongsToLeftSide[0] = false;
            belongsToLeftSide[numStates] = true;
            newVariable = new Variable(oldVariable.getName(), states,
                    new PartitionedInterval(limits, belongsToLeftSide), 0.001);
        }

        newNode = probNet.addVariable(newVariable, NodeType.CHANCE);
        newNode.getVariable().setStates(states);
        newNode.getVariable().setPartitionedInterval(
                newVariable.getPartitionedInterval());
        
        return newVariable;
    }

    /**
     * This function makes the discretization of a variable using equal
     * frequency intervals. If the distribution along the states is not
     * aproximately uniform, the frequency of each interval could be really
     * diferent. For example, if we have three states with frequencies: 200, 3,
     * 4, making two intervals of "equal frequency" would lead to an interval
     * of frequency 200 and an interval of frequency 7.
     * @param oldVariable <code>Variable</code> variable to discretize
     * @param oldProbNet <code>ProbNet</code> original probNet
     * @param cases <code>int[][]</code> database cases
     * @param ind <code>int</code> index of the variable in the array of
     * variables of interest
     * @param ausentValOp <code>int</code> selected option to manage ausent
     * values
     * @throws InvalidStateException 
     * @throws ProbNodeNotFoundException 
     * @throws java.lang.Exception
     */
    public Variable discretizeEqualFreq (Variable oldVariable,
                                         ProbNet oldProbNet,
                                         int[][] cases,
                                         int ind,
                                         int ausentValOp)
            throws NotEnoughMemoryException, InvalidStateException, 
            ProbNodeNotFoundException{

        ArrayList<Integer> variableIndexes = new ArrayList<Integer>();
        ArrayList<Double> orderedStates = new ArrayList<Double>();
        double totalFreq = 0, actualFreq = 0, stateFreq = 0;
        int index = oldProbNet.getVariables(NodeType.CHANCE).
                indexOf(oldVariable), blankState = -1, checkIndex;
        ArrayList<Double> varIntervalLimit = new ArrayList<Double>();
        HashMap<String, String> ioNode = new HashMap<String, String>();
        ProbNode newNode;
        Variable newVariable;
        State[] states = new State[oldVariable.getNumStates()];
        double[] limits;
        boolean[] belongsToLeftSide;
        int[] newCase = new int[oldProbNet.getNumNodes()];

        //Order the numerical states
        for (int i = 0; i < oldVariable.getNumStates(); i++){
            if(!oldVariable.getStates()[i].equals(new State("?")))
                orderedStates.add(Double.parseDouble(oldVariable.
                        getStates()[i].getName()));
        }
        Collections.sort(orderedStates);

        variableIndexes.add(oldProbNet.getVariables(NodeType.CHANCE).
                indexOf(oldVariable));
        for (Variable var : oldProbNet.getVariables(NodeType.CHANCE)){
            if (!var.getName().equals(oldVariable.getName())){
                variableIndexes.add(oldProbNet.getVariables(NodeType.CHANCE).
                    indexOf(var));
            }
        }

        /*Calculate the frequencies of each state. To do this we use a metric.*/
        for (int i = 0; i < cases.length; i++){
            if (cases[i]!=null){
                newCase[0] = cases[i][variableIndexes.get(0)];
                for (int j = 0; j < cases[i].length; j++){
                    if(j < variableIndexes.get(0))
                        newCase[j+1] = cases[i][j];
                    else if (j > variableIndexes.get(0))
                        newCase[j] = cases[i][j];
                }
            }
        }
        TablePotential statesFrequencies = Util.getAbsoluteFreq (oldProbNet, cases, oldProbNet.getProbNode(oldVariable));

        try{
            blankState = oldVariable.getStateIndex("?");
        } catch (InvalidStateException e){
            blankState = -1;
        }
        
        for (int i = 0; i< statesFrequencies.getTableSize(); i++){
            if ((blankState == -1) || (i != blankState))
                totalFreq += statesFrequencies.values[i];
        }

        //aproximate frequency of each interval
        double intervalFreq = (double) totalFreq /
                (double) selectedNumIntervals.get(ind);

        varIntervalLimit.add(min[index]);
        for (Double state : orderedStates){
            //check wether the state is integer or double
            try{
                checkIndex = oldVariable.getStateIndex(""+state);
            }
            catch (Exception e){
                checkIndex = oldVariable.getStateIndex(""+state.intValue());
            }
            stateFreq = statesFrequencies.values[checkIndex];
            if ((actualFreq + stateFreq) >= intervalFreq){
                varIntervalLimit.add(state);
                actualFreq = 0;
            }
            else
                actualFreq += stateFreq;
        }
        if (actualFreq != 0)
            varIntervalLimit.add(max[index]);

        //Create a new discretized variable
        int numStates = varIntervalLimit.size() - 1;
        if((hasMissingValues[index]) && (ausentValOp != MissingValues.ELIMINATE)){
            newVariable = new Variable(oldVariable.getName(), numStates + 1);
            try {
                newVariable.renameState("" + numStates,
                        "?");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else{
            states = new State[numStates];
            belongsToLeftSide = new boolean[numStates + 1];
            limits = new double[numStates + 1];
            for (int i = 0; i < numStates; i++){
                states[i] = new State(new String("(" + varIntervalLimit.get(i) 
                        + " - " + varIntervalLimit.get(i+1) + "]"));
                belongsToLeftSide[i] = true;
                limits[i] = varIntervalLimit.get(i);
            }
            //open the first interval
            states[0] = new State(states[0].getName().replace('(', '['));
            limits[numStates] = varIntervalLimit.get(numStates);
            //Minimum and Maximum must be in the interval
            belongsToLeftSide[0] = false;
            belongsToLeftSide[numStates] = true;
            newVariable = new Variable(oldVariable.getName(), states,
                    new PartitionedInterval(limits, belongsToLeftSide), 0.001);
        }

        probNet.removeProbNode(probNet.getProbNode(oldVariable));
        newNode = probNet.addVariable(newVariable, NodeType.CHANCE);
        newNode.getVariable().setStates(states);
        newNode.getVariable().setPartitionedInterval(
                newVariable.getPartitionedInterval());
        
        ioNode = new HashMap<String,String> (oldProbNet.getProbNode(oldVariable.
                getName()).additionalProperties);
        newNode.additionalProperties = ioNode;
    }

    public static Discretization.Option[] getOptions(){
        return Discretization.Option.values ();
    }
}
