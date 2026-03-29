/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.preprocess;

import org.openmarkov.core.io.database.CaseDatabase;
import org.openmarkov.core.model.network.Variable;

import java.util.List;

/**
 * Utility class for projecting a case database onto a subset of variables.
 */
public class FilterDatabase {
	/**
	 * Filters the given database to include only the specified variables.
	 *
	 * @param database          the original case database
	 * @param selectedVariables the variables to retain in the filtered database
	 * @return a new {@code CaseDatabase} containing only the selected variables and their cases
	 */
	public static CaseDatabase filter(CaseDatabase database, List<Variable> selectedVariables) {
		int[][] oldCases = database.getCases();
		int[][] newCases = new int[oldCases.length][selectedVariables.size()];

		for (int j = 0; j < selectedVariables.size(); ++j) {
			int indexOfVariable = database.getVariables().indexOf(selectedVariables.get(j));
			for (int i = 0; i < oldCases.length; ++i) {
				newCases[i][j] = oldCases[i][indexOfVariable];
			}
		}

		return new CaseDatabase(selectedVariables, newCases);
	}
}
