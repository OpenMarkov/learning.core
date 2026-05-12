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
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.stringformat.LocalizationFormatter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * This class implements the routines to manage absent values in a database.
 *
 * @author joliva
 * @author manuel
 * @author fjdiez
 * @author ibermejo
 * @version 1.0
 * @since OpenMarkov 1.0
 */
public class MissingValues {

	/**
	 * This function removes the missing state of variables with missing values
	 * and removes the cases with missing values according to the preprocessOptions
	 *
     * @param database         {@code CaseDatabase} database to preprocess
     * @param preprocessOption {@code Map<Variable, MissingValues.Option>} containing the preprocess
	 *                         option selected for each variable
	 * @return a new {@code CaseDatabase} with missing values handled according to the selected options
	 */
	public static CaseDatabase process(CaseDatabase database, Map<String, Option> preprocessOption) {
		List<Variable> oldVariables = database.getVariables();
		int[] missingStatesIndices = getMissingStateIndices(oldVariables);
		int[][] oldCases = database.getCases();

		int[] imputationIndex = computeImputationIndices(oldVariables, oldCases,
				missingStatesIndices, preprocessOption);

		List<Variable> preprocessedVariables = removeMissingState(preprocessOption, oldVariables);

		boolean[] keepCase = new boolean[oldCases.length];
		int numCasesToKeep = 0;
		for (int i = 0; i < oldCases.length; i++) {
			keepCase[i] = true;
			for (int j = 0; j < oldVariables.size(); j++) {
				keepCase[i] &= preprocessOption.get(oldVariables.get(j).getName()) != Option.ELIMINATE
						|| !containsMissingValues(oldVariables, oldCases[i]);
			}
			if (keepCase[i]) {
				++numCasesToKeep;
			}
		}
		int[][] newCases = new int[numCasesToKeep][oldVariables.size()];

		int newIndex = 0;
		for (int i = 0; i < oldCases.length; i++) {
			if (!keepCase[i]) continue;
			for (int j = 0; j < oldVariables.size(); j++) {
				Option opt = preprocessOption.get(oldVariables.get(j).getName());
				int value = oldCases[i][j];
				int missing = missingStatesIndices[j];

				if (missing >= 0 && value == missing && isImpute(opt) && imputationIndex[j] >= 0) {
					value = imputationIndex[j];
				}
				if (missing >= 0 && removesMissingState(opt) && value > missing) {
					--value;
				}
				newCases[newIndex][j] = value;
			}
			++newIndex;
		}
		return new CaseDatabase(preprocessedVariables, newCases);
	}

	private static int[] getMissingStateIndices(List<Variable> variables) {
		int[] missingStateIndices = new int[variables.size()];
		for (int i = 0; i < variables.size(); ++i) {
            missingStateIndices[i] = variables.get(i).getStateIndex("?");
        }
		return missingStateIndices;
	}

	/**
	 * This function checks if a case contains missing values
	 *
     * @param variables {@code List} variables to preprocess
     * @param caseData  {@code int[]} case we want to verify
     * @return {@code boolean} true if the case contains missing values
	 */
	private static boolean containsMissingValues(List<Variable> variables, int[] caseData) {
		boolean containsMissingValues = false;

		for (int i = 0; i < caseData.length; ++i) {
			containsMissingValues |= variables.get(i).getStateName(caseData[i]).equals("?");
		}

		return containsMissingValues;
	}

	/**
	 * This function removes the "?" of each variable whose preprocessOption
	 * is ELIMINATE or one of the IMPUTE_* options (after imputation, the
	 * variable no longer contains missing values).
	 *
     * @param preprocessOptions {@code Map<Variable, MissingValues.Option>} preprocess option for each
	 *                          variable
     * @param variables         {@code List} of variables
	 */
	private static List<Variable> removeMissingState(Map<String, Option> preprocessOptions,
			List<Variable> variables) {
		List<Variable> preprocessedVariables = new ArrayList<>();

		for (Variable variable : variables) {
			Option opt = preprocessOptions.get(variable.getName());
			if (removesMissingState(opt) && variable.containsState("?")) {
				Variable newVariable = new Variable(variable.getName(), removeMissingState(variable.getStates()));
				preprocessedVariables.add(newVariable);
			} else {
				preprocessedVariables.add(variable);
			}
		}
		return preprocessedVariables;
	}

	/**
	 * This function removes the "?" state
	 *
     * @param states {@code String[]} original states
     * @return {@code String[]} original states without "?"
	 */
	private static State[] removeMissingState(State[] states) {
		ArrayList<State> newStates = new ArrayList<State>();
		State[] statesAux = new State[states.length - 1];

		for (int i = 0; i < states.length; i++) {
			if (!states[i].getName().equals("?")) {
				newStates.add(states[i]);
			}
		}

		return newStates.toArray(statesAux);
	}

	private static boolean isImpute(Option opt) {
		return opt == Option.IMPUTE_MODE
				|| opt == Option.IMPUTE_MEAN
				|| opt == Option.IMPUTE_MEDIAN;
	}

	private static boolean removesMissingState(Option opt) {
		return opt == Option.ELIMINATE || isImpute(opt);
	}

	/**
	 * Computes, for each variable marked with an IMPUTE_* option, the index of
	 * the state in the OLD variable that should replace "?" occurrences in the
	 * cases array. Returns -1 for variables that should not be imputed or have
	 * no valid replacement (e.g. all observations are "?").
	 */
	private static int[] computeImputationIndices(List<Variable> variables, int[][] cases,
			int[] missingStatesIndices, Map<String, Option> options) {
		int n = variables.size();
		int[] result = new int[n];
		for (int j = 0; j < n; j++) {
			Variable v = variables.get(j);
			Option opt = options.get(v.getName());
			if (!isImpute(opt)) {
				result[j] = -1;
				continue;
			}
			int missing = missingStatesIndices[j];
			int[] hist = new int[v.getNumStates()];
			for (int[] c : cases) {
				++hist[c[j]];
			}

			if (opt == Option.IMPUTE_MODE || !Discretization.isNumeric(v)) {
				result[j] = argMaxIgnoring(hist, missing);
			} else if (opt == Option.IMPUTE_MEAN) {
				Double target = weightedMean(v, hist, missing);
				result[j] = (target == null) ? argMaxIgnoring(hist, missing)
						: nearestNumericStateIndex(v, target, missing);
			} else { // IMPUTE_MEDIAN
				Double target = weightedMedian(v, hist, missing);
				result[j] = (target == null) ? argMaxIgnoring(hist, missing)
						: nearestNumericStateIndex(v, target, missing);
			}
		}
		return result;
	}

	private static int argMaxIgnoring(int[] hist, int ignoreIndex) {
		int best = -1;
		int bestCount = -1;
		for (int k = 0; k < hist.length; k++) {
			if (k == ignoreIndex) continue;
			if (hist[k] > bestCount) {
				bestCount = hist[k];
				best = k;
			}
		}
		return best;
	}

	private static Double weightedMean(Variable v, int[] hist, int missingIndex) {
		double sum = 0;
		long count = 0;
		State[] states = v.getStates();
		for (int k = 0; k < hist.length; k++) {
			if (k == missingIndex || hist[k] == 0) continue;
			try {
				sum += Double.parseDouble(states[k].getName()) * hist[k];
				count += hist[k];
			} catch (NumberFormatException ignored) {
			}
		}
		return count == 0 ? null : sum / count;
	}

	private static Double weightedMedian(Variable v, int[] hist, int missingIndex) {
		List<double[]> pairs = new ArrayList<>();
		long total = 0;
		State[] states = v.getStates();
		for (int k = 0; k < hist.length; k++) {
			if (k == missingIndex || hist[k] == 0) continue;
			try {
				double value = Double.parseDouble(states[k].getName());
				pairs.add(new double[] { value, hist[k] });
				total += hist[k];
			} catch (NumberFormatException ignored) {
			}
		}
		if (pairs.isEmpty()) return null;
		pairs.sort(Comparator.comparingDouble(a -> a[0]));
		long half = total / 2;
		long cumulative = 0;
		for (double[] p : pairs) {
			cumulative += (long) p[1];
			if (cumulative > half) return p[0];
		}
		return pairs.get(pairs.size() - 1)[0];
	}

	private static int nearestNumericStateIndex(Variable v, double target, int missingIndex) {
		int best = -1;
		double bestDist = Double.POSITIVE_INFINITY;
		State[] states = v.getStates();
		for (int k = 0; k < states.length; k++) {
			if (k == missingIndex) continue;
			try {
				double val = Double.parseDouble(states[k].getName());
				double d = Math.abs(val - target);
				if (d < bestDist) {
					bestDist = d;
					best = k;
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return best;
	}

	/**
	 * Returns all available missing values handling options.
	 *
	 * @return an array of all {@code Option} enum values
	 */
	public static Option[] getOptions() {
		return Option.values();
	}

	/* Options to manage absent values*/
	public enum Option implements Serializable, Localizable {
		KEEP, ELIMINATE, IMPUTE_MODE, IMPUTE_MEAN, IMPUTE_MEDIAN;

		@Override public @NotNull String path() {
			return "";
		}

		@Override public @NotNull String localize(LocalizationFormatter formatter) {
			return switch (this){
                case KEEP -> "Keep records with missing values";
                case ELIMINATE -> "Erase records with missing values";
                case IMPUTE_MODE -> "Impute missing values with the mode (most frequent state)";
                case IMPUTE_MEAN -> "Impute missing values with the mean (numeric variables)";
                case IMPUTE_MEDIAN -> "Impute missing values with the median (numeric variables)";
            };
		}
	}

}
