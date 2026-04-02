/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.preprocess;

import org.jetbrains.annotations.NotNull;
import org.openmarkov.core.io.database.CaseDatabase;
import org.openmarkov.core.localize.Localizable;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.stringformat.LocalizationFormatter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class implements the routines to manage the discretization of the
 * variables.
 *
 * @author joliva
 * @author manuel
 * @author fjdiez
 * @author ibermejo
 * @version 1.0
 * @since OpenMarkov 1.0
 */
public class Discretization {
    
    /**
     * Returns all available discretization options.
     *
     * @return an array of all {@code Option} enum values
     */
    public static Option[] getOptions() {
        return Option.values();
    }
    
    /**
     * This function determines whether a variable is numeric or not
     *
     * @param variable {@code Variable}
     * @return true if the variable is numeric
     */
    public static boolean isNumeric(Variable variable) {
        
        State[] states = variable.getStates();
        boolean hasMissingValues = false;
        for (int i = 0; i < states.length; i++) {
            try {
                if (!states[i].getName().equals("?")) {
                    Double.parseDouble(states[i].getName());
                } else {
                    hasMissingValues = true;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return states.length > 4 || (!hasMissingValues && states.length == 3);
    }
    
    /**
     * This function discretizes the database.
     *
     * @param database                 the case database to discretize
     * @param discretizeOptions        discretization option per variable name
     * @param numIntervalsPerVariable  number of intervals per variable name
     * @param modelNet                 optional model network for MODEL_NET discretization
     * @return {@code CaseDatabase} updated database
     */
    public static CaseDatabase process(CaseDatabase database, Map<String, Option> discretizeOptions,
                                       Map<String, Integer> numIntervalsPerVariable, ProbNet modelNet) {
        
        List<Variable> newVariables = new ArrayList<>();
        
        for (Variable variable : database.getVariables()) {
            int numIntervals = numIntervalsPerVariable.get(variable.getName());
            Variable newVariable = switch (discretizeOptions.get(variable.getName())) {
                case EQUAL_WIDTH -> discretizeEqualWidth(variable, numIntervals);
                case EQUAL_FREQ -> discretizeEqualFreq(variable, database, numIntervals);
                case MODEL_NET -> discretizeFromModelNet(variable, modelNet);
                default -> variable;
            };
            newVariables.add(newVariable);
        }
        
        /* construct the new cases array */
        int[][] newCases = discretizeCases(database, newVariables, discretizeOptions);
        
        return new CaseDatabase(newVariables, newCases);
    }
    
    /**
     * This function discretizes the database.
     *
     * @param database                 the case database to discretize
     * @param discretizeOptions        discretization option per variable name
     * @param numIntervalsPerVariable  number of intervals per variable name
     * @return {@code CaseDatabase} updated database
     */
    public static CaseDatabase process(CaseDatabase database, Map<String, Option> discretizeOptions,
                                       Map<String, Integer> numIntervalsPerVariable) {
        return process(database, discretizeOptions, numIntervalsPerVariable, null);
    }
    
    /**
     * This function discretizes the database.
     *
     * @param database             the case database to discretize
     * @param discretizationOption the discretization option to apply to all variables
     * @param numIntervals         the number of intervals for all variables
     * @return {@code CaseDatabase} updated database
     */
    public static CaseDatabase process(CaseDatabase database, Option discretizationOption,
                                       int numIntervals) {
        Map<String, Option> discretizeOptions = new HashMap<>();
        Map<String, Integer> numIntervalsPerVariable = new HashMap<>();
        
        for (Variable variable : database.getVariables()) {
            discretizeOptions.put(variable.getName(), discretizationOption);
            numIntervalsPerVariable.put(variable.getName(), numIntervals);
        }
        
        return process(database, discretizeOptions, numIntervalsPerVariable, null);
    }
    
    /**
     * This function discretizes the database.
     *
     * @param database the case database to discretize
     * @param modelNet the model network whose discretization intervals are used
     * @return {@code CaseDatabase} updated database
     */
    public static CaseDatabase process(CaseDatabase database, ProbNet modelNet) {
        Map<String, Option> discretizeOptions = new HashMap<>();
        Map<String, Integer> numIntervalsPerVariable = new HashMap<>();
        
        for (Variable variable : database.getVariables()) {
            discretizeOptions.put(variable.getName(), Option.MODEL_NET);
            numIntervalsPerVariable.put(variable.getName(), -1);
        }
        
        return process(database, discretizeOptions, numIntervalsPerVariable, modelNet);
    }
    
    /**
     * This function makes the discretization of a variable taking the
     * information from a model net
     *
     * @param oldVariable {@code Variable} variable to discretize
     * @param modelNet    {@code ProbNet} net from which to tak the
     *                    information of the discretization
     */
    private static Variable discretizeFromModelNet(Variable oldVariable, ProbNet modelNet) {
        
        Variable newVariable = oldVariable;
        
        if (modelNet != null) {
            Variable modelNetVariable = modelNet.getVariable(oldVariable.getName());
            
            boolean missingValuesInDB = oldVariable.containsState("?");
            boolean missingValuesInModelNet = modelNetVariable.containsState("?");
            State[] newStates;
            if (missingValuesInDB && !missingValuesInModelNet) {
                // Add "missing value" state
                newStates = new State[modelNetVariable.getNumStates() + 1];
                for (int i = 0; i < modelNetVariable.getNumStates(); ++i) {
                    newStates[i] = modelNetVariable.getStates()[i];
                }
                newStates[newStates.length - 1] = new State("?");
            } else {
                newStates = modelNetVariable.getStates();
            }
            
            PartitionedInterval modelNetInterval = modelNetVariable.getPartitionedInterval();
            if (modelNetInterval != null) {
                double[] limits = modelNetInterval.getLimits();
                boolean[] belongsToLeftSide = modelNetInterval.getBelongsToLeftSide();
                
                newVariable = new Variable(oldVariable.getName(), newStates,
                                           new PartitionedInterval(limits, belongsToLeftSide), 0.001);
            } else {
                newVariable = new Variable(oldVariable.getName(), newStates);
            }
        }
        
        return newVariable;
    }
    
    private static Variable discretizeEqualWidth(Variable variable, int numIntervals) {
        
        //Create a new discretized variable
        boolean containsMissingValues = variable.containsState("?");
        
        int numStates = (containsMissingValues) ? numIntervals + 1 : numIntervals;
        State[] states = new State[numStates];
        boolean[] belongsToLeftSide = new boolean[numIntervals + 1];
        double[] limits = new double[numIntervals + 1];
        double max = calculateVariableMax(variable);
        double min = calculateVariableMin(variable);
        double step = (max - min) / numIntervals;
        for (int i = 0; i < numIntervals; i++) {
            states[i] = new State("(" + (min + (i * step)) + " , " + (min + ((i + 1) * step)) + "]");
            belongsToLeftSide[i] = true;
            limits[i] = min + (i * step);
        }
        //Minimum and Maximum must be in the interval
        states[0].setName(states[0].getName().replace('(', '['));
        belongsToLeftSide[0] = false;
        belongsToLeftSide[numIntervals] = true;
        limits[numIntervals] = max;
        if (containsMissingValues) {
            states[numStates - 1] = new State("?");
        }
        Variable newVariable = new Variable(variable.getName(), states, new PartitionedInterval(limits, belongsToLeftSide),
                                            0.001);
        
        return newVariable;
    }
    
    /**
     * This function makes the discretization of a variable using equal
     * frequency intervals. If the distribution along the states is not
     * approximately uniform, the frequency of each interval could be really
     * different. For example, if we have three states with frequencies: 200, 3,
     * 4, making two intervals of "equal frequency" would lead to an interval
     * of frequency 200 and an interval of frequency 7.
     *
     * @param variable     {@code Variable} variable to discretize
     * @param database     {@code int[][]} database cases
     * @param numIntervals the num intervals
     */
    private static Variable discretizeEqualFreq(Variable variable, CaseDatabase database, int numIntervals) {
        State[] states = variable.getStates();
        List<Double> intervalLimits = new ArrayList<Double>();
        double accruedFreq = 0, stateFreq;
        int stateIndex;
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en"));
        DecimalFormat decimalFormat = (DecimalFormat) nf;
        decimalFormat.applyPattern("###.########");
        String stateName;
        
        // Order the numerical states
        List<Double> orderedStates = new ArrayList<Double>();
        for (int i = 0; i < states.length; i++) {
            if (!states[i].getName().equals("?"))
                orderedStates.add(Double.parseDouble(states[i].getName()));
        }
        Collections.sort(orderedStates);
        
        int[] casesForVariable = database.getCases(variable);
        int[] histogram = new int[variable.getStates().length];
        for (int i = 0; i < casesForVariable.length; ++i) {
            ++histogram[casesForVariable[i]];
        }
        
        // number of cases with valid data, i.e. all minus the missing values
        double validCaseNum = casesForVariable.length;
        int missingStateIndex = variable.getStateIndex("?");
        if (missingStateIndex != -1) {
            validCaseNum = casesForVariable.length - histogram[missingStateIndex];
        }
        
        //calculate approximate frequency of each interval
        double intervalFreq = validCaseNum / numIntervals;
        intervalLimits.add(Double.NEGATIVE_INFINITY);
        
        for (Double state : orderedStates) {
            //check whether the state is integer or double
            stateName = state.toString();
            String stateToSearch = stateName.contains("E") ?
                    decimalFormat.format(state.doubleValue()) :
                    state.toString();
            stateIndex = variable.getStateIndex(stateToSearch);
            if (stateIndex == -1) {
                stateIndex = variable.getStateIndex("" + state.intValue());
            }
            stateFreq = histogram[stateIndex];
            if ((accruedFreq + stateFreq) >= intervalFreq) {
                intervalLimits.add(state);
                accruedFreq = 0;
            } else
                accruedFreq += stateFreq;
        }
        intervalLimits.add(Double.POSITIVE_INFINITY);
        
        //Create a new discretized variable
        boolean containsMissingValues = variable.containsState("?");
        int numStates = (containsMissingValues) ? numIntervals + 1 : numIntervals;
        State[] newStates = new State[numStates];
        double[] limits = new double[numIntervals + 1];
        boolean[] belongsToLeftSide = new boolean[numIntervals + 1];
        for (int i = 0; i < numIntervals; i++) {
            newStates[i] = new State("(" + intervalLimits.get(i) + " , " + intervalLimits.get(i + 1) + "]");
            belongsToLeftSide[i] = true;
            limits[i] = intervalLimits.get(i);
        }
        limits[limits.length - 1] = intervalLimits.get(limits.length - 1);
        // open the last interval
        newStates[numIntervals - 1] = new State(newStates[numIntervals - 1].getName().replace(']', ')'));
        // Minimum and Maximum must be in the interval
        belongsToLeftSide[0] = false;
        belongsToLeftSide[numIntervals] = true;
        
        if (containsMissingValues) {
            newStates[numStates - 1] = new State("?");
        }
        
        Variable newVariable = new Variable(variable.getName(), newStates, new PartitionedInterval(limits, belongsToLeftSide),
                                            0.001);
        
        return newVariable;
    }
    
    /**
     * This function updates the database cases to adapt them to the new
     * states of the discretized variables.
     *
     * @param database          {@code int[][]} original database cases
     * @param newVariables the new variables
     * @param discretizeOptions {@code ArrayList} discretization option
     *                          selected for each variable.
     */
    private static int[][] discretizeCases(CaseDatabase database, List<Variable> newVariables,
                                           Map<String, Option> discretizeOptions) {
        int[][] oldCases = database.getCases();
        int[][] newCases = new int[oldCases.length][newVariables.size()];
        
        for (int j = 0; j < database.getVariables().size(); j++) {
            Variable oldVariable = database.getVariables().get(j);
            State[] oldStates = oldVariable.getStates();
            int indexOfNewVariable = getIndexOfNewVariable(newVariables, oldVariable);
            Variable newVariable = newVariables.get(indexOfNewVariable);
            PartitionedInterval partitionedInterval = newVariable.getPartitionedInterval();
            double[] newIntervals = (partitionedInterval != null) ? partitionedInterval.getLimits() : null;
            boolean[] belongsToLeft = (partitionedInterval != null) ? partitionedInterval.getBelongsToLeftSide() : null;
            boolean isNumeric = isNumeric(oldVariable);
            int missingValeStateIndex = newVariable.getStateIndex("?");
            
            for (int i = 0; i < oldCases.length; i++) {
                
                if (isNumeric) {
                    switch (discretizeOptions.get(oldVariable.getName())) {
                        case NONE:
                            State state = oldVariable.getStates()[oldCases[i][j]];
                            int stateIndex = newVariable.getStateIndex(state.getName());
                            if(stateIndex!=-1){
                                newCases[i][indexOfNewVariable] = stateIndex;
                            }
                            break;
                        default:
                            if (oldStates[oldCases[i][j]].getName().equals("?")) {
                                newCases[i][j] = missingValeStateIndex;
                            } else {
                                double value = Double.parseDouble(oldStates[oldCases[i][j]].getName());
                                // We search for the interval in which the value is contained
                                int k = 1;
                                boolean matched = false;
                                while (!matched && k < newIntervals.length) {
                                    if (value <= newIntervals[k]) {
                                        newCases[i][j] = (value < newIntervals[k] || belongsToLeft[k]) ? k - 1 : k;
                                        matched = true;
                                    }
                                    ++k;
                                }
                            }
                            break;
                    }
                } else {
                    State state = oldVariable.getStates()[oldCases[i][j]];
                    int stateIndex = newVariable.getStateIndex(state.getName());
                    if(stateIndex!=-1){
                        newCases[i][indexOfNewVariable] = stateIndex;
                    }
                }
            }
        }
        
        return newCases;
    }
    
    private static int getIndexOfNewVariable(List<Variable> newVariables, Variable oldVariable) {
        int i = 0;
        int index = -1;
        
        while (i < newVariables.size() && index == -1) {
            if (newVariables.get(i).getName().equals(oldVariable.getName())) {
                index = i;
            }
            ++i;
        }
        return index;
    }
    
    private static double calculateVariableMax(Variable variable) {
        double max = Double.NEGATIVE_INFINITY;
        
        for (State state : variable.getStates()) {
            if (!state.getName().equals("?") && max < Double.parseDouble(state.getName())) {
                max = Double.parseDouble(state.getName());
            }
        }
        
        return max;
    }
    
    private static double calculateVariableMin(Variable variable) {
        double min = Double.POSITIVE_INFINITY;
        
        for (State state : variable.getStates()) {
            if (!state.getName().equals("?") && min > Double.parseDouble(state.getName())) {
                min = Double.parseDouble(state.getName());
            }
        }
        
        return min;
    }
    
    public enum Option implements Localizable {
        NONE, EQUAL_FREQ, EQUAL_WIDTH, MODEL_NET;
        
        
        @Override public @NotNull String path() {
            return "";
        }
        
        @Override public @NotNull String localize(LocalizationFormatter formatter) {
            return switch (this){
                case NONE -> "Do not discretize";
                case EQUAL_FREQ -> "Equal frequency intervals";
                case EQUAL_WIDTH -> "Equal width intervals";
                case MODEL_NET -> "Use model network";
            };
        }
    }
    
}
