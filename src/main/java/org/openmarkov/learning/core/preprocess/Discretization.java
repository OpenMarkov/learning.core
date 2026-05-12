/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.preprocess;

import org.jetbrains.annotations.NotNull;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.localize.Localizable;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.stringformat.LocalizationFormatter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        return process(database, discretizeOptions, numIntervalsPerVariable, modelNet, null);
    }

    /**
     * Discretizes the database. Supervised discretization options (MDLP) require
     * a class variable.
     *
     * @param database                 the case database to discretize
     * @param discretizeOptions        discretization option per variable name
     * @param numIntervalsPerVariable  number of intervals per variable name (ignored for MDLP/MODEL_NET)
     * @param modelNet                 optional model network for MODEL_NET discretization
     * @param classVariable            class variable used by supervised discretization (MDLP); may be null otherwise
     * @return {@code CaseDatabase} updated database
     */
    public static CaseDatabase process(CaseDatabase database, Map<String, Option> discretizeOptions,
                                       Map<String, Integer> numIntervalsPerVariable, ProbNet modelNet,
                                       Variable classVariable) {

        List<Variable> newVariables = new ArrayList<>();

        for (Variable variable : database.getVariables()) {
            int numIntervals = numIntervalsPerVariable.get(variable.getName());
            Option opt = discretizeOptions.get(variable.getName());
            Variable newVariable = switch (opt) {
                case EQUAL_WIDTH -> discretizeEqualWidth(variable, numIntervals);
                case EQUAL_FREQ -> discretizeEqualFreq(variable, database, numIntervals);
                case MODEL_NET -> discretizeFromModelNet(variable, modelNet);
                case MDLP -> discretizeMDLP(variable, database, classVariable);
                case CHIMERGE -> discretizeChiMerge(variable, database, classVariable);
                case KMEANS -> discretizeKMeans(variable, database, numIntervals);
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
     * Supervised discretization using the Fayyad &amp; Irani MDLP criterion
     * (Multi-Interval Discretization, 1993). Recursively splits the value range
     * of the variable at the cut point that maximizes the information gain
     * w.r.t. the class variable, accepting the cut only when the MDL stopping
     * criterion is satisfied.
     *
     * @param variable      the numeric variable to discretize
     * @param database      case database providing the joint observations
     * @param classVariable the class (target) variable; must belong to the database
     */
    private static Variable discretizeMDLP(Variable variable, CaseDatabase database, Variable classVariable) {
        if (classVariable == null) {
            throw new IllegalArgumentException(
                    "MDLP discretization requires a class variable; none was provided for " + variable.getName());
        }
        if (variable.getName().equals(classVariable.getName())) {
            throw new IllegalArgumentException(
                    "MDLP class variable cannot be the variable being discretized: " + variable.getName());
        }

        int varIdx = -1;
        int classIdx = -1;
        List<Variable> vars = database.getVariables();
        for (int j = 0; j < vars.size(); j++) {
            if (vars.get(j).getName().equals(variable.getName())) varIdx = j;
            if (vars.get(j).getName().equals(classVariable.getName())) classIdx = j;
        }
        if (varIdx < 0 || classIdx < 0) {
            throw new IllegalArgumentException("Variable or class variable not found in database");
        }

        int[][] cases = database.getCases();
        int missingIdx = variable.getStateIndex("?");
        int classMissingIdx = classVariable.getStateIndex("?");

        // Gather (value, classState) pairs, skipping cases with missing value on either variable.
        List<double[]> pairs = new ArrayList<>();
        State[] states = variable.getStates();
        for (int[] c : cases) {
            int vIdx = c[varIdx];
            int cIdx = c[classIdx];
            if (missingIdx >= 0 && vIdx == missingIdx) continue;
            if (classMissingIdx >= 0 && cIdx == classMissingIdx) continue;
            try {
                double value = Double.parseDouble(states[vIdx].getName());
                pairs.add(new double[] { value, cIdx });
            } catch (NumberFormatException ignored) {
            }
        }

        boolean containsMissingValues = variable.containsState("?");
        if (pairs.isEmpty()) {
            return variable; // Nothing observable; leave the variable untouched.
        }

        pairs.sort(Comparator.comparingDouble(a -> a[0]));
        int n = pairs.size();
        double[] values = new double[n];
        int[] classesArr = new int[n];
        for (int i = 0; i < n; i++) {
            values[i] = pairs.get(i)[0];
            classesArr[i] = (int) pairs.get(i)[1];
        }

        List<Double> cuts = new ArrayList<>();
        mdlpRecurse(values, classesArr, 0, n, cuts);
        Collections.sort(cuts);

        double min = values[0];
        double max = values[n - 1];
        int numIntervals = cuts.size() + 1;
        int numStates = containsMissingValues ? numIntervals + 1 : numIntervals;

        State[] newStates = new State[numStates];
        double[] limits = new double[numIntervals + 1];
        boolean[] belongsToLeftSide = new boolean[numIntervals + 1];

        limits[0] = min;
        for (int i = 0; i < cuts.size(); i++) limits[i + 1] = cuts.get(i);
        limits[numIntervals] = max;

        for (int i = 0; i < numIntervals; i++) {
            newStates[i] = new State("(" + limits[i] + " , " + limits[i + 1] + "]");
            belongsToLeftSide[i] = true;
        }
        newStates[0].setName(newStates[0].getName().replace('(', '['));
        belongsToLeftSide[0] = false;
        belongsToLeftSide[numIntervals] = true;
        if (containsMissingValues) {
            newStates[numStates - 1] = new State("?");
        }

        return new Variable(variable.getName(), newStates,
                new PartitionedInterval(limits, belongsToLeftSide), 0.001);
    }

    private static void mdlpRecurse(double[] values, int[] classesArr, int from, int to, List<Double> cuts) {
        int n = to - from;
        if (n < 2) return;

        double entS = entropy(classesArr, from, to);
        int bestIdx = -1;
        double bestGain = Double.NEGATIVE_INFINITY;
        double bestEnt1 = 0;
        double bestEnt2 = 0;

        for (int i = from; i < to - 1; i++) {
            if (values[i] == values[i + 1]) continue;
            int n1 = i + 1 - from;
            int n2 = to - (i + 1);
            double ent1 = entropy(classesArr, from, i + 1);
            double ent2 = entropy(classesArr, i + 1, to);
            double weighted = ((double) n1 / n) * ent1 + ((double) n2 / n) * ent2;
            double gain = entS - weighted;
            if (gain > bestGain) {
                bestGain = gain;
                bestIdx = i;
                bestEnt1 = ent1;
                bestEnt2 = ent2;
            }
        }

        if (bestIdx < 0 || bestGain <= 0) return;

        int k = distinctClasses(classesArr, from, to);
        int k1 = distinctClasses(classesArr, from, bestIdx + 1);
        int k2 = distinctClasses(classesArr, bestIdx + 1, to);
        double log2 = Math.log(2);
        double delta = Math.log(Math.pow(3, k) - 2) / log2
                - (k * entS - k1 * bestEnt1 - k2 * bestEnt2);
        double threshold = Math.log(n - 1) / log2 / n + delta / n;

        if (bestGain <= threshold) return;

        double cutPoint = (values[bestIdx] + values[bestIdx + 1]) / 2.0;
        cuts.add(cutPoint);

        mdlpRecurse(values, classesArr, from, bestIdx + 1, cuts);
        mdlpRecurse(values, classesArr, bestIdx + 1, to, cuts);
    }

    private static double entropy(int[] classesArr, int from, int to) {
        int n = to - from;
        if (n == 0) return 0;
        Map<Integer, Integer> hist = new HashMap<>();
        for (int i = from; i < to; i++) {
            hist.merge(classesArr[i], 1, Integer::sum);
        }
        double h = 0;
        double log2 = Math.log(2);
        for (int count : hist.values()) {
            double p = (double) count / n;
            h -= p * Math.log(p) / log2;
        }
        return h;
    }

    private static int distinctClasses(int[] classesArr, int from, int to) {
        Set<Integer> seen = new HashSet<>();
        for (int i = from; i < to; i++) seen.add(classesArr[i]);
        return seen.size();
    }

    /**
     * ChiMerge supervised discretization (Kerber 1992): bottom-up merging of
     * adjacent intervals whose χ² statistic against the class variable is below
     * the critical value for α = 0.10 with (numClasses − 1) degrees of freedom
     * (Wilson-Hilferty approximation). Each initial interval is a distinct
     * observed value; merging stops when the smallest adjacent χ² reaches the
     * threshold or only one interval remains.
     */
    private static Variable discretizeChiMerge(Variable variable, CaseDatabase database, Variable classVariable) {
        if (classVariable == null) {
            throw new IllegalArgumentException(
                    "ChiMerge discretization requires a class variable; none was provided for " + variable.getName());
        }
        if (variable.getName().equals(classVariable.getName())) {
            throw new IllegalArgumentException(
                    "ChiMerge class variable cannot be the variable being discretized: " + variable.getName());
        }

        int varIdx = indexOfVariable(database, variable);
        int classIdx = indexOfVariable(database, classVariable);
        if (varIdx < 0 || classIdx < 0) {
            throw new IllegalArgumentException("Variable or class variable not found in database");
        }

        int[][] cases = database.getCases();
        int missingIdx = variable.getStateIndex("?");
        int classMissingIdx = classVariable.getStateIndex("?");
        int numClasses = classVariable.getNumStates();

        List<double[]> obs = new ArrayList<>();
        State[] states = variable.getStates();
        for (int[] c : cases) {
            int vIdx = c[varIdx];
            int cIdx = c[classIdx];
            if (missingIdx >= 0 && vIdx == missingIdx) continue;
            if (classMissingIdx >= 0 && cIdx == classMissingIdx) continue;
            try {
                obs.add(new double[] { Double.parseDouble(states[vIdx].getName()), cIdx });
            } catch (NumberFormatException ignored) {
            }
        }
        boolean containsMissingValues = variable.containsState("?");
        if (obs.isEmpty()) return variable;

        obs.sort(Comparator.comparingDouble(a -> a[0]));

        List<double[]> intervalRanges = new ArrayList<>();
        List<int[]> counts = new ArrayList<>();
        double curVal = obs.get(0)[0];
        int[] curCounts = new int[numClasses];
        for (double[] o : obs) {
            if (o[0] != curVal) {
                intervalRanges.add(new double[] { curVal, curVal });
                counts.add(curCounts);
                curVal = o[0];
                curCounts = new int[numClasses];
            }
            curCounts[(int) o[1]]++;
        }
        intervalRanges.add(new double[] { curVal, curVal });
        counts.add(curCounts);

        double threshold = chiSquareCritical(Math.max(1, numClasses - 1));

        while (counts.size() > 1) {
            int minPair = -1;
            double minChi = Double.POSITIVE_INFINITY;
            for (int i = 0; i < counts.size() - 1; i++) {
                double chi = chiSquared(counts.get(i), counts.get(i + 1));
                if (chi < minChi) {
                    minChi = chi;
                    minPair = i;
                }
            }
            if (minPair < 0 || minChi >= threshold) break;
            int[] a = counts.get(minPair);
            int[] b = counts.get(minPair + 1);
            int[] merged = new int[numClasses];
            for (int k = 0; k < numClasses; k++) merged[k] = a[k] + b[k];
            counts.set(minPair, merged);
            intervalRanges.set(minPair,
                    new double[] { intervalRanges.get(minPair)[0], intervalRanges.get(minPair + 1)[1] });
            counts.remove(minPair + 1);
            intervalRanges.remove(minPair + 1);
        }

        List<Double> cuts = new ArrayList<>();
        for (int i = 0; i < intervalRanges.size() - 1; i++) {
            cuts.add((intervalRanges.get(i)[1] + intervalRanges.get(i + 1)[0]) / 2.0);
        }
        double min = intervalRanges.get(0)[0];
        double max = intervalRanges.get(intervalRanges.size() - 1)[1];
        return buildIntervalVariable(variable.getName(), min, max, cuts, containsMissingValues);
    }

    private static double chiSquared(int[] r1, int[] r2) {
        int numClasses = r1.length;
        int n1 = 0;
        int n2 = 0;
        for (int k = 0; k < numClasses; k++) {
            n1 += r1[k];
            n2 += r2[k];
        }
        int n = n1 + n2;
        if (n == 0 || n1 == 0 || n2 == 0) return 0;
        double chi = 0;
        for (int k = 0; k < numClasses; k++) {
            int colTotal = r1[k] + r2[k];
            if (colTotal == 0) continue;
            double e1 = (double) n1 * colTotal / n;
            double e2 = (double) n2 * colTotal / n;
            if (e1 > 0) chi += (r1[k] - e1) * (r1[k] - e1) / e1;
            if (e2 > 0) chi += (r2[k] - e2) * (r2[k] - e2) / e2;
        }
        return chi;
    }

    /** Wilson-Hilferty critical value for χ²_α with α = 0.10 (z ≈ 1.2816). */
    private static double chiSquareCritical(int df) {
        if (df < 1) return 0;
        double z = 1.2815515655446004;
        double a = 1.0 - 2.0 / (9.0 * df);
        double b = z * Math.sqrt(2.0 / (9.0 * df));
        double term = a + b;
        return df * term * term * term;
    }

    /**
     * Unsupervised 1-D K-means discretization (Lloyd's algorithm). Centroids
     * are initialized at evenly-spaced quantiles, the assign/update loop runs
     * to convergence, and cut points are taken at the midpoints between
     * consecutive sorted centroids.
     */
    private static Variable discretizeKMeans(Variable variable, CaseDatabase database, int k) {
        if (k < 2) return variable;
        int varIdx = indexOfVariable(database, variable);
        if (varIdx < 0) return variable;

        int missingIdx = variable.getStateIndex("?");
        int[][] cases = database.getCases();
        State[] states = variable.getStates();

        List<Double> obs = new ArrayList<>();
        for (int[] c : cases) {
            int vIdx = c[varIdx];
            if (missingIdx >= 0 && vIdx == missingIdx) continue;
            try {
                obs.add(Double.parseDouble(states[vIdx].getName()));
            } catch (NumberFormatException ignored) {
            }
        }
        boolean containsMissingValues = variable.containsState("?");
        if (obs.size() < k) return variable;

        Collections.sort(obs);
        int n = obs.size();
        double[] values = new double[n];
        for (int i = 0; i < n; i++) values[i] = obs.get(i);

        double[] centroids = new double[k];
        for (int i = 0; i < k; i++) {
            int idx = (int) Math.round((n - 1) * (i + 0.5) / (double) k);
            centroids[i] = values[Math.min(idx, n - 1)];
        }

        int[] assign = new int[n];
        Arrays.fill(assign, -1);
        int maxIter = 100;
        for (int it = 0; it < maxIter; it++) {
            boolean changed = false;
            for (int i = 0; i < n; i++) {
                double bestDist = Double.POSITIVE_INFINITY;
                int best = 0;
                for (int c = 0; c < k; c++) {
                    double d = Math.abs(values[i] - centroids[c]);
                    if (d < bestDist) {
                        bestDist = d;
                        best = c;
                    }
                }
                if (assign[i] != best) {
                    assign[i] = best;
                    changed = true;
                }
            }
            double[] sum = new double[k];
            int[] cnt = new int[k];
            for (int i = 0; i < n; i++) {
                sum[assign[i]] += values[i];
                cnt[assign[i]]++;
            }
            for (int c = 0; c < k; c++) {
                if (cnt[c] > 0) centroids[c] = sum[c] / cnt[c];
            }
            if (!changed) break;
        }

        Arrays.sort(centroids);
        List<Double> cuts = new ArrayList<>();
        for (int i = 0; i < centroids.length - 1; i++) {
            double cut = (centroids[i] + centroids[i + 1]) / 2.0;
            if (cuts.isEmpty() || cut > cuts.get(cuts.size() - 1)) cuts.add(cut);
        }

        double min = values[0];
        double max = values[n - 1];
        return buildIntervalVariable(variable.getName(), min, max, cuts, containsMissingValues);
    }

    private static int indexOfVariable(CaseDatabase database, Variable v) {
        List<Variable> vars = database.getVariables();
        for (int j = 0; j < vars.size(); j++) {
            if (vars.get(j).getName().equals(v.getName())) return j;
        }
        return -1;
    }

    /** Builds a numeric Variable from a min/max range and an ordered list of cut points. */
    private static Variable buildIntervalVariable(String name, double min, double max,
                                                  List<Double> sortedCuts, boolean containsMissingValues) {
        int numIntervals = sortedCuts.size() + 1;
        int numStates = containsMissingValues ? numIntervals + 1 : numIntervals;

        State[] newStates = new State[numStates];
        double[] limits = new double[numIntervals + 1];
        boolean[] belongsToLeftSide = new boolean[numIntervals + 1];

        limits[0] = min;
        for (int i = 0; i < sortedCuts.size(); i++) limits[i + 1] = sortedCuts.get(i);
        limits[numIntervals] = max;

        for (int i = 0; i < numIntervals; i++) {
            newStates[i] = new State("(" + limits[i] + " , " + limits[i + 1] + "]");
            belongsToLeftSide[i] = true;
        }
        newStates[0].setName(newStates[0].getName().replace('(', '['));
        belongsToLeftSide[0] = false;
        belongsToLeftSide[numIntervals] = true;
        if (containsMissingValues) {
            newStates[numStates - 1] = new State("?");
        }
        return new Variable(name, newStates, new PartitionedInterval(limits, belongsToLeftSide), 0.001);
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
        NONE, EQUAL_FREQ, EQUAL_WIDTH, MODEL_NET, MDLP, CHIMERGE, KMEANS;


        @Override public @NotNull String path() {
            return "";
        }

        @Override public @NotNull String localize(LocalizationFormatter formatter) {
            return switch (this){
                case NONE -> "Do not discretize";
                case EQUAL_FREQ -> "Equal frequency intervals";
                case EQUAL_WIDTH -> "Equal width intervals";
                case MODEL_NET -> "Use model network";
                case MDLP -> "MDLP (Fayyad-Irani, supervised)";
                case CHIMERGE -> "ChiMerge (χ², supervised)";
                case KMEANS -> "K-means (unsupervised)";
            };
        }
    }
    
}
