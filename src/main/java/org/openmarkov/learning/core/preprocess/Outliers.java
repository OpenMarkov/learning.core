/*
 * Copyright (c) CISIAD, UNED, Spain,  2026. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.preprocess;

import org.jetbrains.annotations.NotNull;
import org.openmarkov.core.localize.Localizable;
import org.openmarkov.core.model.database.CaseDatabase;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.stringformat.LocalizationFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Detection and handling of outliers on numeric variables of a {@link CaseDatabase},
 * driven by a per-variable {@link Option}. Two detection criteria are supported:
 * Tukey's IQR rule (Q1-1.5·IQR, Q3+1.5·IQR) and the classical z-score rule
 * (mean ± 3·sd). For each detected outlier, the value can either be replaced by
 * the missing-value state {@code "?"} (adding it to the variable if it did not
 * exist) or the entire case can be dropped from the database.
 */
public class Outliers {

	private static final double IQR_K = 1.5;
	private static final double Z_K = 3.0;

	public static CaseDatabase process(CaseDatabase database, Map<String, Option> options) {
		List<Variable> oldVariables = database.getVariables();
		int n = oldVariables.size();
		int[][] oldCases = database.getCases();

		double[][] bounds = new double[n][];
		for (int j = 0; j < n; j++) {
			Option opt = options.get(oldVariables.get(j).getName());
			if (opt == null || opt == Option.NONE) continue;
			bounds[j] = computeBounds(oldVariables.get(j), oldCases, j, opt);
		}

		List<Variable> newVariables = new ArrayList<>(n);
		int[] missingIndexInNew = new int[n];
		int[][] winsorIdx = new int[n][];
		for (int j = 0; j < n; j++) {
			Variable v = oldVariables.get(j);
			Option opt = options.get(v.getName());
			if (bounds[j] != null && isMarkMissing(opt) && !v.containsState("?")) {
				State[] oldStates = v.getStates();
				State[] withMissing = Arrays.copyOf(oldStates, oldStates.length + 1);
				withMissing[oldStates.length] = new State("?");
				newVariables.add(new Variable(v.getName(), withMissing));
				missingIndexInNew[j] = oldStates.length;
			} else {
				newVariables.add(v);
				missingIndexInNew[j] = v.getStateIndex("?");
			}
			if (bounds[j] != null && isWinsorize(opt)) {
				winsorIdx[j] = nearestInRangeStates(v, bounds[j]);
			}
		}

		List<int[]> kept = new ArrayList<>(oldCases.length);
		for (int[] oldCase : oldCases) {
			boolean drop = false;
			int[] newCase = oldCase.clone();
			for (int j = 0; j < n; j++) {
				if (bounds[j] == null) continue;
				Variable v = oldVariables.get(j);
				int idx = oldCase[j];
				State[] states = v.getStates();
				String name = states[idx].getName();
				if (name.equals("?")) continue;
				double value;
				try {
					value = Double.parseDouble(name);
				} catch (NumberFormatException e) {
					continue;
				}
				if (value < bounds[j][0] || value > bounds[j][1]) {
					Option opt = options.get(v.getName());
					if (isRemove(opt)) {
						drop = true;
						break;
					} else if (isMarkMissing(opt)) {
						newCase[j] = missingIndexInNew[j];
					} else if (isWinsorize(opt) && winsorIdx[j] != null) {
						if (value < bounds[j][0] && winsorIdx[j][0] >= 0) {
							newCase[j] = winsorIdx[j][0];
						} else if (value > bounds[j][1] && winsorIdx[j][1] >= 0) {
							newCase[j] = winsorIdx[j][1];
						}
					}
				}
			}
			if (!drop) kept.add(newCase);
		}
		return new CaseDatabase(newVariables, kept.toArray(new int[0][]));
	}

	/** Returns {lowIdx, highIdx}: the smallest and largest in-range state indices, or -1 if none. */
	private static int[] nearestInRangeStates(Variable v, double[] bounds) {
		State[] states = v.getStates();
		int lowIdx = -1;
		int highIdx = -1;
		double lowVal = Double.POSITIVE_INFINITY;
		double highVal = Double.NEGATIVE_INFINITY;
		for (int s = 0; s < states.length; s++) {
			String name = states[s].getName();
			if (name.equals("?")) continue;
			try {
				double val = Double.parseDouble(name);
				if (val >= bounds[0] && val <= bounds[1]) {
					if (val < lowVal) {
						lowVal = val;
						lowIdx = s;
					}
					if (val > highVal) {
						highVal = val;
						highIdx = s;
					}
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return new int[] { lowIdx, highIdx };
	}

	private static double[] computeBounds(Variable v, int[][] cases, int j, Option opt) {
		if (!Discretization.isNumeric(v)) return null;
		State[] states = v.getStates();
		List<Double> values = new ArrayList<>();
		for (int[] c : cases) {
			String name = states[c[j]].getName();
			if (name.equals("?")) continue;
			try {
				values.add(Double.parseDouble(name));
			} catch (NumberFormatException ignored) {
			}
		}
		if (values.isEmpty()) return null;

		if (opt == Option.IQR_REMOVE || opt == Option.IQR_MARK_MISSING || opt == Option.IQR_WINSORIZE) {
			Collections.sort(values);
			double q1 = percentile(values, 0.25);
			double q3 = percentile(values, 0.75);
			double iqr = q3 - q1;
			return new double[] { q1 - IQR_K * iqr, q3 + IQR_K * iqr };
		}
		// Z-score
		double mean = 0;
		for (double x : values) mean += x;
		mean /= values.size();
		double sqSum = 0;
		for (double x : values) sqSum += (x - mean) * (x - mean);
		double sd = Math.sqrt(sqSum / values.size());
		if (sd == 0) return null; // No spread → no outliers possible.
		return new double[] { mean - Z_K * sd, mean + Z_K * sd };
	}

	/** Type-7 linear interpolation percentile (R default). Caller must pre-sort. */
	private static double percentile(List<Double> sorted, double p) {
		int n = sorted.size();
		if (n == 1) return sorted.get(0);
		double h = (n - 1) * p;
		int lo = (int) Math.floor(h);
		int hi = (int) Math.ceil(h);
		return sorted.get(lo) + (h - lo) * (sorted.get(hi) - sorted.get(lo));
	}

	private static boolean isRemove(Option opt) {
		return opt == Option.IQR_REMOVE || opt == Option.ZSCORE_REMOVE;
	}

	private static boolean isMarkMissing(Option opt) {
		return opt == Option.IQR_MARK_MISSING || opt == Option.ZSCORE_MARK_MISSING;
	}

	private static boolean isWinsorize(Option opt) {
		return opt == Option.IQR_WINSORIZE || opt == Option.ZSCORE_WINSORIZE;
	}

	public static Option[] getOptions() {
		return Option.values();
	}

	public enum Option implements Localizable {
		NONE,
		IQR_REMOVE, IQR_MARK_MISSING, IQR_WINSORIZE,
		ZSCORE_REMOVE, ZSCORE_MARK_MISSING, ZSCORE_WINSORIZE;

		@Override public @NotNull String path() {
			return "";
		}

		@Override public @NotNull String localize(LocalizationFormatter formatter) {
			return switch (this) {
				case NONE -> "Do not handle outliers";
				case IQR_REMOVE -> "IQR rule: remove records with outliers";
				case IQR_MARK_MISSING -> "IQR rule: mark outliers as missing values";
				case IQR_WINSORIZE -> "IQR rule: winsorize outliers to nearest in-range state";
				case ZSCORE_REMOVE -> "Z-score rule: remove records with outliers";
				case ZSCORE_MARK_MISSING -> "Z-score rule: mark outliers as missing values";
				case ZSCORE_WINSORIZE -> "Z-score rule: winsorize outliers to nearest in-range state";
			};
		}
	}
}
