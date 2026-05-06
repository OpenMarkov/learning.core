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
		// remove the "?" state
		List<Variable> oldVariables = database.getVariables();
		int[] missingStatesIndices = getMissingStateIndices(oldVariables);
		List<Variable> preprocessedVariables = removeMissingState(preprocessOption, oldVariables);
		/* Remove the cases with an absent value, if this option was selected. */
		int[][] oldCases = database.getCases();
		boolean[] keepCase = new boolean[oldCases.length];
		int numCasesToKeep = 0;
		for (int i = 0; i < oldCases.length; i++) {
			keepCase[i] = true;
			for (int j = 0; j < database.getVariables().size(); j++) {
				keepCase[i] &= preprocessOption.get(oldVariables.get(j).getName()) != Option.ELIMINATE
						|| !containsMissingValues(oldVariables, oldCases[i]);
			}
			if (keepCase[i]) {
				++numCasesToKeep;
			}
		}
		int[][] newCases = new int[numCasesToKeep][database.getVariables().size()];

		int newIndex = 0;
		for (int i = 0; i < oldCases.length; i++) {
			if (keepCase[i]) {
				for (int j = 0; j < database.getVariables().size(); j++) {
					newCases[newIndex][j] = oldCases[i][j];
					// update the indices after deleting the missing state
					if (missingStatesIndices[j] >= 0 && newCases[newIndex][j] > missingStatesIndices[j]) {
						--newCases[newIndex][j];
					}
				}
				++newIndex;
			}
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
	 * is ELIMINATE
	 *
     * @param preprocessOptions {@code Map<Variable, MissingValues.Option>} preprocess option for each
	 *                          variable
     * @param variables         {@code List} of variables
	 */
	private static List<Variable> removeMissingState(Map<String, Option> preprocessOptions,
			List<Variable> variables) {
		List<Variable> preprocessedVariables = new ArrayList<>();

		for (Variable variable : variables) {
			if (preprocessOptions.get(variable.getName()) == Option.ELIMINATE) {
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
		KEEP, ELIMINATE;
		
		@Override public @NotNull String path() {
			return "";
		}
		
		@Override public @NotNull String localize(LocalizationFormatter formatter) {
			return switch (this){
                case KEEP -> "Keep records with missing values";
                case ELIMINATE -> "Erase records with missing values";
            };
		}
	}

}
