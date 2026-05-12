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
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.stringformat.LocalizationFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Filter-based feature selection driven by a class variable. Three methods are
 * supported:
 * <ul>
 * <li>{@link Method#MUTUAL_INFORMATION}: top-k by I(X;C)</li>
 * <li>{@link Method#CHI_SQUARED}: top-k by Pearson χ²</li>
 * <li>{@link Method#MARKOV_BLANKET_IAMB}: Markov-blanket estimate using
 *     Incremental Association MB (Tsamardinos &amp; Aliferis, 2003); k is
 *     ignored because the algorithm decides the size of the blanket.</li>
 * </ul>
 * The class variable is always retained in the output even if it does not
 * appear among the selected features. Variables are kept in their original
 * order. Cases with missing values on the variable being scored or on the
 * class are dropped from the count for that score.
 */
public class FeatureSelection {

	private static final double IAMB_THRESHOLD = 0.01;
	private static final double LOG2 = Math.log(2);

	public static CaseDatabase select(CaseDatabase database, Variable classVariable, Method method, int k) {
		if (method == null || method == Method.NONE) return database;
		if (classVariable == null) {
			throw new IllegalArgumentException("Feature selection requires a class variable");
		}
		List<Variable> vars = database.getVariables();
		int classIdx = indexOf(vars, classVariable);
		if (classIdx < 0) {
			throw new IllegalArgumentException("Class variable not found in database: " + classVariable.getName());
		}

		int[][] cases = database.getCases();
		int n = vars.size();

		Set<Integer> selected;
		if (method == Method.MARKOV_BLANKET_IAMB) {
			selected = iamb(cases, vars, classIdx);
		} else {
			double[] scores = new double[n];
			boolean[] scored = new boolean[n];
			for (int j = 0; j < n; j++) {
				if (j == classIdx) continue;
				scores[j] = (method == Method.MUTUAL_INFORMATION)
						? mutualInformation(cases, vars, j, classIdx)
						: chiSquared(cases, vars, j, classIdx);
				scored[j] = true;
			}
			List<Integer> order = new ArrayList<>();
			for (int j = 0; j < n; j++) if (scored[j]) order.add(j);
			order.sort(Comparator.comparingDouble((Integer j) -> scores[j]).reversed());
			selected = new HashSet<>();
			int limit = Math.max(0, Math.min(k, order.size()));
			for (int i = 0; i < limit; i++) selected.add(order.get(i));
		}
		selected.add(classIdx);

		List<Variable> newVars = new ArrayList<>();
		int[] indexMap = new int[n];
		int newIdx = 0;
		for (int j = 0; j < n; j++) {
			if (selected.contains(j)) {
				newVars.add(vars.get(j));
				indexMap[j] = newIdx++;
			} else {
				indexMap[j] = -1;
			}
		}
		int[][] newCases = new int[cases.length][newVars.size()];
		for (int i = 0; i < cases.length; i++) {
			for (int j = 0; j < n; j++) {
				if (indexMap[j] >= 0) newCases[i][indexMap[j]] = cases[i][j];
			}
		}
		return new CaseDatabase(newVars, newCases);
	}

	private static int indexOf(List<Variable> vars, Variable v) {
		for (int j = 0; j < vars.size(); j++) {
			if (vars.get(j).getName().equals(v.getName())) return j;
		}
		return -1;
	}

	private static double mutualInformation(int[][] cases, List<Variable> vars, int xIdx, int yIdx) {
		Variable vx = vars.get(xIdx);
		Variable vy = vars.get(yIdx);
		int nx = vx.getNumStates();
		int ny = vy.getNumStates();
		int xMiss = vx.getStateIndex("?");
		int yMiss = vy.getStateIndex("?");

		int[][] joint = new int[nx][ny];
		int[] cx = new int[nx];
		int[] cy = new int[ny];
		int total = 0;
		for (int[] c : cases) {
			int x = c[xIdx];
			int y = c[yIdx];
			if (xMiss >= 0 && x == xMiss) continue;
			if (yMiss >= 0 && y == yMiss) continue;
			joint[x][y]++;
			cx[x]++;
			cy[y]++;
			total++;
		}
		if (total == 0) return 0;
		double mi = 0;
		for (int i = 0; i < nx; i++) {
			if (cx[i] == 0) continue;
			for (int j = 0; j < ny; j++) {
				if (joint[i][j] == 0 || cy[j] == 0) continue;
				double pxy = (double) joint[i][j] / total;
				double px = (double) cx[i] / total;
				double py = (double) cy[j] / total;
				mi += pxy * Math.log(pxy / (px * py)) / LOG2;
			}
		}
		return mi;
	}

	private static double chiSquared(int[][] cases, List<Variable> vars, int xIdx, int yIdx) {
		Variable vx = vars.get(xIdx);
		Variable vy = vars.get(yIdx);
		int nx = vx.getNumStates();
		int ny = vy.getNumStates();
		int xMiss = vx.getStateIndex("?");
		int yMiss = vy.getStateIndex("?");

		int[][] obs = new int[nx][ny];
		int[] cx = new int[nx];
		int[] cy = new int[ny];
		int total = 0;
		for (int[] c : cases) {
			int x = c[xIdx];
			int y = c[yIdx];
			if (xMiss >= 0 && x == xMiss) continue;
			if (yMiss >= 0 && y == yMiss) continue;
			obs[x][y]++;
			cx[x]++;
			cy[y]++;
			total++;
		}
		if (total == 0) return 0;
		double chi = 0;
		for (int i = 0; i < nx; i++) {
			if (cx[i] == 0) continue;
			for (int j = 0; j < ny; j++) {
				if (cy[j] == 0) continue;
				double exp = (double) cx[i] * cy[j] / total;
				if (exp == 0) continue;
				double diff = obs[i][j] - exp;
				chi += diff * diff / exp;
			}
		}
		return chi;
	}

	/**
	 * Incremental Association Markov Blanket (Tsamardinos &amp; Aliferis, 2003).
	 * Forward phase: greedily add the feature with the highest conditional
	 * mutual information against the class given the current blanket, while it
	 * exceeds {@code IAMB_THRESHOLD}. Backward phase: remove any feature in the
	 * blanket whose CMI against the class given the rest of the blanket falls
	 * below the threshold.
	 */
	private static Set<Integer> iamb(int[][] cases, List<Variable> vars, int classIdx) {
		int n = vars.size();
		Set<Integer> mb = new HashSet<>();
		boolean added = true;
		while (added && mb.size() < n - 1) {
			added = false;
			double bestCmi = IAMB_THRESHOLD;
			int bestVar = -1;
			for (int j = 0; j < n; j++) {
				if (j == classIdx || mb.contains(j)) continue;
				double cmi = conditionalMutualInformation(cases, vars, j, classIdx, mb);
				if (cmi > bestCmi) {
					bestCmi = cmi;
					bestVar = j;
				}
			}
			if (bestVar >= 0) {
				mb.add(bestVar);
				added = true;
			}
		}
		boolean changed = true;
		while (changed) {
			changed = false;
			Integer toRemove = null;
			for (Integer x : new ArrayList<>(mb)) {
				Set<Integer> rest = new HashSet<>(mb);
				rest.remove(x);
				double cmi = conditionalMutualInformation(cases, vars, x, classIdx, rest);
				if (cmi <= IAMB_THRESHOLD) {
					toRemove = x;
					break;
				}
			}
			if (toRemove != null) {
				mb.remove(toRemove);
				changed = true;
			}
		}
		return mb;
	}

	private static double conditionalMutualInformation(int[][] cases, List<Variable> vars,
			int xIdx, int yIdx, Set<Integer> condSet) {
		if (condSet.isEmpty()) return mutualInformation(cases, vars, xIdx, yIdx);

		int[] condIdx = new int[condSet.size()];
		int p = 0;
		for (int c : condSet) condIdx[p++] = c;

		Variable vx = vars.get(xIdx);
		Variable vy = vars.get(yIdx);
		int nx = vx.getNumStates();
		int ny = vy.getNumStates();
		int xMiss = vx.getStateIndex("?");
		int yMiss = vy.getStateIndex("?");
		int[] condMiss = new int[condIdx.length];
		for (int i = 0; i < condIdx.length; i++) condMiss[i] = vars.get(condIdx[i]).getStateIndex("?");

		Map<String, int[][]> groupJoint = new HashMap<>();
		Map<String, Integer> groupTotal = new HashMap<>();
		int total = 0;
		StringBuilder sb = new StringBuilder();
		for (int[] c : cases) {
			int x = c[xIdx];
			int y = c[yIdx];
			if (xMiss >= 0 && x == xMiss) continue;
			if (yMiss >= 0 && y == yMiss) continue;
			sb.setLength(0);
			boolean skip = false;
			for (int i = 0; i < condIdx.length; i++) {
				int v = c[condIdx[i]];
				if (condMiss[i] >= 0 && v == condMiss[i]) { skip = true; break; }
				sb.append(v).append(',');
			}
			if (skip) continue;
			String key = sb.toString();
			int[][] joint = groupJoint.computeIfAbsent(key, k -> new int[nx][ny]);
			joint[x][y]++;
			groupTotal.merge(key, 1, Integer::sum);
			total++;
		}
		if (total == 0) return 0;

		double cmi = 0;
		for (Map.Entry<String, int[][]> e : groupJoint.entrySet()) {
			int[][] joint = e.getValue();
			int gt = groupTotal.get(e.getKey());
			int[] cx = new int[nx];
			int[] cy = new int[ny];
			for (int i = 0; i < nx; i++) {
				for (int j = 0; j < ny; j++) {
					cx[i] += joint[i][j];
					cy[j] += joint[i][j];
				}
			}
			double mi = 0;
			for (int i = 0; i < nx; i++) {
				if (cx[i] == 0) continue;
				for (int j = 0; j < ny; j++) {
					if (joint[i][j] == 0 || cy[j] == 0) continue;
					double pxy = (double) joint[i][j] / gt;
					double px = (double) cx[i] / gt;
					double py = (double) cy[j] / gt;
					mi += pxy * Math.log(pxy / (px * py)) / LOG2;
				}
			}
			cmi += ((double) gt / total) * mi;
		}
		return cmi;
	}

	public static Method[] getMethods() {
		return Method.values();
	}

	public enum Method implements Localizable {
		NONE, MUTUAL_INFORMATION, CHI_SQUARED, MARKOV_BLANKET_IAMB;

		@Override public @NotNull String path() {
			return "";
		}

		@Override public @NotNull String localize(LocalizationFormatter formatter) {
			return switch (this) {
				case NONE -> "Do not select features";
				case MUTUAL_INFORMATION -> "Mutual information (top-k)";
				case CHI_SQUARED -> "Chi-squared (top-k)";
				case MARKOV_BLANKET_IAMB -> "Markov blanket (IAMB)";
			};
		}
	}
}
