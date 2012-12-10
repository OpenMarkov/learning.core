/*
 * Copyright 2011 CISIAD, UNED, Spain
 *
 * Licensed under the European Union Public Licence, version 1.1 (EUPL)
 *
 * Unless required by applicable law, this code is distributed
 * on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.algorithm.statistics;

import java.util.List;

import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;

/** Count the number of configurations of a set of variables of interest. */
public class ConfigurationsOperations {

	// Attributes
	private static int confStringLength;

	// Constants
	private static final String SEPARATION_STRING = " | ";

	private static final int SEPARATION_STRING_LENGTH = 3;

	private static final char MINUS_CHAR = '-';

	private static final char WHITE_CHAR = ' ';

	private static final String EOL = "\r\n";

	/**
	 * @param data
	 *            . <code>int[][]</code>
	 * @param interestVariables
	 *            . <code>ArrayList</code> of <code>Variable</code>
	 * @param allVariables
	 *            . <code>ArrayList</code> of <code>Variable</code>
	 * @return A <code>TablePotential</code> containing the conditional
	 *         probability table of the first variable of
	 *         <code>interestVariables</code> given the rest of the variables in
	 *         <code>interestVariables</code>.
	 */
	public static TablePotential getCPT(int[][] data,
			List<Variable> interestVariables, List<Variable> allVariables)
			throws Exception {
		TablePotential potential = new TablePotential(interestVariables,
				PotentialRole.CONDITIONAL_PROBABILITY);
		TablePotential count = count(data, interestVariables, allVariables);
		Variable conditionedVariable = interestVariables.get(0);
		int numStatesConditioned = conditionedVariable.getNumStates();
		int numConditioningConfigurations = potential.values.length
				/ numStatesConditioned;
		for (int i = 0; i < numConditioningConfigurations; i++) {
			double acc = 0.0;
			for (int j = 0; j < numStatesConditioned; j++) {
				acc += count.values[i * numStatesConditioned + j];
			}
			for (int j = 0; j < numStatesConditioned; j++) {
				potential.values[i * numStatesConditioned + j] = count.values[i
						* numStatesConditioned + j]
						/ acc;
			}
		}
		return potential;
	}

	/**
	 * Counts the number of configurations of a set of
	 * <code>interestVariables</code> that appears in <code>data</code>.
	 * 
	 * @param data
	 *            . <code>int[][]</code>.
	 * @param interestVariables
	 *            . <code>ArrayList</code> of <code>Variable</code>
	 * @param allVariables
	 *            . <code>ArrayList</code> of <code>Variable</code>
	 * @return A <code>TablePotential</code> containing in its table the number
	 *         of times that each configuration appears in <code>data</code>
	 */
	public static TablePotential count(int[][] data,
			List<Variable> interestVariables, List<Variable> allVariables)
			throws Exception {
		// Store number of configurations in a TablePotential
		TablePotential potential = new TablePotential(interestVariables,
				PotentialRole.CONDITIONAL_PROBABILITY);
		int numInterestVariables = interestVariables.size();
		int[] interestVariablesPositions = new int[numInterestVariables];
		for (int i = 0; i < numInterestVariables; i++) {
			interestVariablesPositions[i] = allVariables
					.indexOf(interestVariables.get(i));
		}

		int[] configuration = new int[numInterestVariables];
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < numInterestVariables; j++) {
				configuration[j] = data[i][interestVariablesPositions[j]];
			}
			int position = potential.getPosition(configuration);
			potential.values[position] += 1.0;
		}
		return potential;
	}

	/**
	 * Generates a long <code>String</code> that contains:
	 * <ul>
	 * <li>The list of the potential variables
	 * <li>All the configurations. Each configuration contains:
	 * <ul>
	 * <li>The states names of each variable
	 * <li>The value corresponding to that configuration.
	 * </ul>
	 * </ul>
	 * 
	 * @param potential
	 *            . <code>TablePotential</code>
	 * @param confTitle
	 *            . <code>String</code> corresponding to the column of the
	 *            values of the configurations.
	 * @param accuracy
	 *            . <code>double</code>
	 * @return <code>String</code>
	 */
	public static String getStringPotential(TablePotential potential,
			String confTitle, double accuracy) {
		String out = new String();
		confStringLength = confTitle.length();

		// Write variables names in a box
		int[] variablesNumChars = getVariablesNumChars(potential);
		String line = getSeparationLine(potential, variablesNumChars);
		List<Variable> variables = potential.getVariables();
		int numVariables = variables.size();
		out += line;
		for (int i = 0; i < numVariables; i++) {
			String variableName = variables.get(i).getName();
			out = out + variableName;
			int lengthVariableName = variableName.length();
			int numWhiteChars = variablesNumChars[i] - lengthVariableName + 1;
			for (int k = 0; k < numWhiteChars; k++) {
				out += WHITE_CHAR;
			}
			out = out + SEPARATION_STRING;
		}
		out = out + confTitle + EOL;
		out += line;

		// Write configurations
		for (int i = 0; i < potential.values.length; i++) {
			int[] configuration = potential.getConfiguration(i);
			for (int j = 0; j < numVariables; j++) {
				String stateName = variables.get(j).getStateName(
						configuration[j]);
				out += stateName;
				int lengthStateName = stateName.length();
				int numWhiteChars = variablesNumChars[j] - lengthStateName + 1;
				for (int k = 0; k < numWhiteChars; k++) {
					out += WHITE_CHAR;
				}
				out += SEPARATION_STRING;
			}
			out += new Double(Math.round(potential.values[i]
					* Math.pow(10, accuracy))
					/ Math.pow(10, accuracy));
			out += EOL;
		}
		out += line + EOL;

		return out;
	}

	/**
	 * @param potential
	 *            . <code>TablePotential</code>
	 * @return An array containing the maximum length of each variable state,
	 *         including its name. <code>int[]</code>. The array has a length =
	 *         numVariables + 1 to store the string configuration title.
	 */
	private static int[] getVariablesNumChars(TablePotential potential) {
		List<Variable> variables = potential.getVariables();
		int numVariables = variables.size();
		int[] variablesNumChars = new int[numVariables + 1];
		for (int i = 0; i < numVariables; i++) {
			variablesNumChars[i] = getMaxStringLength(variables.get(i))
					+ SEPARATION_STRING_LENGTH;
		}
		variablesNumChars[variablesNumChars.length - 1] = confStringLength;
		return variablesNumChars;
	}

	/**
	 * @param variable
	 *            . <code>Variable</code>
	 * @return An integer containing the maximum length of the names of the
	 *         states of the <code>variable</code> received including its name.
	 *         <code>int</code>
	 */
	private static int getMaxStringLength(Variable variable) {
		int maxLength = variable.getName().length();
		State[] states = variable.getStates();
		for (int i = 0; i < states.length; i++) {
			if (states[i].getName().length() > maxLength) {
				maxLength = states[i].getName().length();
			}
		}
		return maxLength;
	}

	/**
	 * @param potential
	 *            . <code>TablePotential</code>
	 * @param variablesNumChars
	 *            . <code>int[]</code>.
	 * @return A separation string used to display the potential.
	 *         <code>String</code>
	 */
	private static String getSeparationLine(TablePotential potential,
			int[] variablesNumChars) {
		List<Variable> variables = potential.getVariables();
		int numVariables = variables.size();
		String line = new String();
		for (int i = 0; i < numVariables; i++) {
			for (int j = 0; j < variablesNumChars[i] + SEPARATION_STRING_LENGTH; j++) {
				line += MINUS_CHAR;
			}
		}
		for (int i = 0; i < confStringLength + 2; i++) {
			line += MINUS_CHAR;
		}
		line += EOL;
		return line;
	}

}
