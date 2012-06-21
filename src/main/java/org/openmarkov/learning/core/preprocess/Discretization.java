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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NotEnoughMemoryException;
import org.openmarkov.core.exception.ProbNodeNotFoundException;
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
 * @version 1.0
 * @since OpenMarkov 1.0 */
public class Discretization {

    /** Discretization options */
    private static final String[] options = {"No discretizar",
                "Según red modelo", "Igual frecuencia", "Igual anchura"};

    public static final String defaultOption = "Especificar para cada variable";
    public static final int NOTHING = 0;
    public static final int MODELNET = 1;
    public static final int EQUAL_FREQ = 2;
    public static final int EQUAL_WIDTH = 3;

    /** Array to know wether the variables are numeric or not */
    public static boolean[] isNumeric;

    /** Array to know wether the variables have ausent values or not */
    public static boolean[] hasAusentValues;

    /** Arrays to store the min and max values of the numeric variables */
    static double[] max;
    static double[] min;

    /** ArrayList with the number of intervals to use in the discretization of
     * each variable */
    static ArrayList<Integer> selectedNumIntervals;

    static ProbNet probNet = null;
    static HashMap<String, String> newIONet;

    /** database cases adaptated after preprocessing */
    static int[][] newCases = null;
    
    /**
     * This function find the numeric variables of a probNet
     * @param probNet <code>ProbNet</code>
     * @return <code>boolean[]</code> the i-th field of the array is true if
     * the i-th variable of the probNet is numeric
     */
    public static boolean[] numericAttributes(ProbNet probNet){
        int i = 0;
        isNumeric = new boolean[probNet.getNumNodes()];
        hasAusentValues = new boolean[probNet.getNumNodes()];
        max = new double[probNet.getNumNodes()];
        min = new double[probNet.getNumNodes()];

        for (int j = 0; j < probNet.getNumNodes(); j++)
            hasAusentValues[i] = false;

        for (Variable var : probNet.getChanceAndDecisionVariables()){
            isNumeric[i] = isNumeric(var.getStates(), i); 
            i++;
        }
        return isNumeric;
    }

    /**
     * This function takes a set of states and determines wether they are
     * numeric or not
     * @param states <code>String[]</code> states to verify
     * @param index <code>int</code> variable index
     * @return true if the states are numeric
     */
    private static boolean isNumeric(State[] states, int index){
        double value;
        double maxValue = Double.NEGATIVE_INFINITY;
        double minValue = Double.POSITIVE_INFINITY;

        for (int i = 0; i < states.length; i++){
            try{
                value = Double.parseDouble(states[i].getName());
                if (value > maxValue)
                    maxValue = value;
                if (value < minValue)
                    minValue = value;
            } catch (NumberFormatException e){
                if (!states[i].equals(new State("?")))
                    return false; 
                else
                    hasAusentValues[index] = true;
            }
        }

        /* If the attribute only has two states, we state that
         * it is not numeric.*/
        if (((hasAusentValues[index]) && (states.length < 4)) ||
                (states.length < 3))
            return false;

        max[index] = maxValue;
        min[index] = minValue;
        return true;
    }

    /**
     * This function makes all the preprocessing related to discretization.
     *
     * @param variables <code>ArrayList</code> variables to preprocess
     * @param discretizeOption <code>ArrayList</code> containing the
     * discretization option selected for each variable
     * @param preprocessOption <code>ArrayList</code> containing the preprocess
     * option selected for each variable
     * @param numIntervals <code>ArrayList</code> containing the number of
     * interval selected for each variable
     * @param oldProbNet <code>ProbNet</code> original probNet
     * @param cases <code>int[][]</code> original database cases.
     * @param modelNet <code>ProbNet</code> net from wich to take the
     * discretization
     * @return <code>ProbNet</code> updated probNet
     * @throws ProbNodeNotFoundException 
     * @throws InvalidStateException 
     * @throws NotEnoughMemoryException 
     * @throws WrongDiscretizationLimitException 
     * @throws java.lang.Exception
     */
    public static ProbNet discretize(ArrayList<Variable> variables,
            ArrayList<Integer> discretizeOption, ArrayList<Integer>
            preprocessOption, ArrayList<Integer> numIntervals,
            ProbNet oldProbNet, int[][] cases, ProbNet modelNet) throws 
            NotEnoughMemoryException, InvalidStateException, 
            ProbNodeNotFoundException, WrongDiscretizationLimitException{

        ArrayList<String> variableNames = new ArrayList<String>();
        int index = 0;
        selectedNumIntervals = numIntervals;
        newIONet = new HashMap<String, String>();

        probNet = oldProbNet.copy();

        if ((variables == null) || (variables.size() == 0))
            return null;

        for (Variable var : variables){
            variableNames.add(var.getName());
        }

        for (Variable oldVariable : probNet.getVariables(NodeType.CHANCE)){
            if(!variables.contains(oldVariable)){
                probNet.removeProbNode(probNet.getProbNode(oldVariable));
            }
            else {
                index = variables.indexOf(oldVariable);

                /* If the user selected to discretize and keep absent values,
                 * show an error message and don't discretize the variable*/
                if((preprocessOption.get(index) ==
                        AbsentValues.INPUT) && (discretizeOption.get(index) != NOTHING)){

                	Logger.getLogger(Discretization.class).error("La variable " + 
                			variableNames.get(index) +
                			" no ha sido discretizada. No se " +
                            "permite la discretización manteniendo los " +
                            "valores ausentes.");
                    noDiscretize(oldVariable, oldProbNet);
                    continue;
                }
                switch(discretizeOption.get(index)){
                    case (EQUAL_WIDTH):
                        discretizeEqualWidth(oldVariable, oldProbNet, index,
                                preprocessOption.get(index));
                        break;
                    case (EQUAL_FREQ):
                        discretizeEqualFreq(oldVariable, oldProbNet, cases,
                                index, preprocessOption.get(index));
                        break;
                    case (MODELNET):
                        discretizeFromModelNet(oldVariable, modelNet,
                                oldProbNet);
                        break;
                    default:
                        noDiscretize(oldVariable, oldProbNet);
                        break;
                }
            }
        }

        for (Entry<String, String> property : (Set<Entry<String, String>>)
                oldProbNet.additionalProperties.entrySet())
                newIONet.put((String)property.getKey(), 
                        property.getValue().toString());

        for (int i = 0; i < variableNames.size(); i++) {
            newIONet.put("VariableOrder[" + i + "]", variableNames.get(i)); 
        }
        probNet.additionalProperties = newIONet;

        /* construct the new cases array */
        setCases(cases, oldProbNet, discretizeOption);

        return probNet;
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
    private static void setCases(int[][] cases, ProbNet oldProbNet,
            ArrayList<Integer> discretizeOption) throws 
                ProbNodeNotFoundException, WrongDiscretizationLimitException{
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
    private static void discretizeFromModelNet(Variable oldVariable,
            ProbNet modelNet, ProbNet oldProbNet) throws ProbNodeNotFoundException{
        HashMap<String, String> ioNode = new HashMap<String, String>();
        ProbNode newNode;

        if (modelNet != null){
            Variable modelVariable = modelNet.getProbNode(oldVariable.
                    getName()).getVariable();
            if (modelVariable.getVariableType() == 
                    VariableType.DISCRETIZED){
                probNet.removeProbNode(probNet.getProbNode(oldVariable));
                newNode = probNet.addVariable(modelVariable, 
                        NodeType.CHANCE);
                ioNode = oldProbNet.getProbNode(oldVariable).
                        additionalProperties;
                newNode.additionalProperties = ioNode;
                return;
            }
        }
    }

    /**
     * This function makes the discretization of a variable using equal width
     * intervals.
     * @param oldVariable <code>Variable</code> variable to discretize
     * @param oldProbNet <code>ProbNet</code> original probNet
     * @param ind <code>int</code> index of the variable in the array of
     * variables of interest
     * @param ausentValOp <code>int</code> selected option to manage absent
     * values
     * @throws java.lang.Exception
     */
    public static void discretizeEqualWidth(Variable oldVariable,
            ProbNet oldProbNet, int ind, int ausentValOp){
        HashMap<String, String> ioNode = new HashMap<String, String>();
        ProbNode newNode;
        Variable newVariable;
        int index = oldProbNet.getVariables(NodeType.CHANCE).
                indexOf(oldVariable);
        int numStates = selectedNumIntervals.get(ind);
        State[] states = new State[numStates];
        boolean[] belongsToLeftSide;
        double[] limits;

        //Create a new discretized variable
        if((hasAusentValues[oldProbNet.getVariables(NodeType.CHANCE).
                indexOf(oldVariable)]) &&
                (ausentValOp != AbsentValues.ELIMINATE)){
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

        probNet.removeProbNode(probNet.getProbNode(oldVariable));
        newNode = probNet.addVariable(newVariable, NodeType.CHANCE);
        newNode.getVariable().setStates(states);
        newNode.getVariable().setPartitionedInterval(
                newVariable.getPartitionedInterval());
        ioNode = (HashMap<String,String>) oldProbNet.getProbNode(oldVariable)
            .additionalProperties.clone();
        newNode.additionalProperties = ioNode;
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
    public static void discretizeEqualFreq(Variable oldVariable, ProbNet
            oldProbNet, int[][] cases, int ind, int ausentValOp)
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
        if((hasAusentValues[index]) && (ausentValOp != AbsentValues.ELIMINATE)){
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
        
        ioNode = (HashMap<String,String>) oldProbNet.getProbNode(oldVariable.
                getName()).additionalProperties.clone();
        newNode.additionalProperties = ioNode;
    }

    /**
     * This function just adds the old variable in the new probNet
     * @param oldVariable <code>Variable</code> variable to add
     * @param oldProbNet <code>ProbNet</code> probNet to take the info of
     * the variable
     * @throws java.lang.Exception
     */
    public static void noDiscretize(Variable oldVariable, ProbNet oldProbNet){
        HashMap<String, String> ioNode = new HashMap<String, String>();
        ProbNode newNode;

        newNode = probNet.addVariable(oldVariable, NodeType.CHANCE);
        ioNode = oldProbNet.getProbNode(oldVariable).additionalProperties;
        newNode.additionalProperties = ioNode;
    }

    public static String[] getOptions(){
        return options;
    }

    public static int[][] getCases(){
        return newCases;
    }
}
