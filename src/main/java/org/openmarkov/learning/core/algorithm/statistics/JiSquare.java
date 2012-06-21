/*
* Copyright 2011 CISIAD, UNED, Spain
*
* Licensed under the European Union Public Licence, version 1.1 (EUPL)
*
* Unless required by applicable law, this code is distributed
* on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
*/

package org.openmarkov.learning.core.algorithm.statistics;

import java.util.ArrayList;

import org.openmarkov.core.exception.ProbNodeNotFoundException;


/** @author manuel */
public class JiSquare {

	// Attributes
	/** Significance level% = 100 * ( 1 - alpha ).<p>
	 * Example: significance level = 95% <=> alpha = 0.05. */
	private static final double[] alphas = {0.001, 0.0025, 0.005, 0.01, 0.025, 
			0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5};

	/** row = freedom degree, column = alpha (corresponding to alphas 
	 *  attribute). */
	private static final double[][] jiTable = {
			{10.8274, 9.1404, 7.8794, 6.6349, 5.0239, 3.8415, 2.7055, 2.0722,
			 1.6424, 1.3233, 1.0742, 0.8735, 0.7083, 0.5707, 0.4549},// free = 1
			{13.8150, 11.9827, 10.5965, 9.2104, 7.3778, 5.9915, 4.6052, 3.7942,
			 3.2189, 2.7726, 2.4079, 2.0996, 1.8326, 1.5970, 1.3863},// free = 2
			{16.2660, 14.3202, 12.8381, 11.3449, 9.3484, 7.8147, 6.2514, 5.3170,
			 4.6416, 4.1083, 3.6649, 3.2831, 2.9462, 2.6430, 2.3660},// free = 3
			{18.4662, 16.4238, 14.8602, 13.2767, 11.1433, 9.4877, 7.7794,6.7449,
			 5.9886, 5.3853, 4.8784, 4.4377, 4.0446, 3.6871, 3.3567},// free = 4
			{20.5147, 18.3854, 16.7496, 15.0863, 12.8325, 11.0705,9.2363,8.1152,
			 7.2893, 6.6257, 6.0644, 5.5731, 5.1319, 4.7278, 4.3515},// free = 5
			{22.4575, 20.2491, 18.5475, 16.8119, 14.4494,12.5916,10.6446,9.4461,
			 8.5581, 7.8408, 7.2311, 6.6948, 6.2108, 5.7652, 5.3481},// free = 6
			{24.3213, 22.0402, 20.2777, 18.4753,16.0128,14.0671,12.0170,10.7479,
			 9.8032, 9.0371, 8.3834, 7.8061, 7.2832, 6.8000, 6.3458},// free = 7
			{26.1239, 23.7742, 21.9549, 20.0902,17.5345,15.5073,13.3616,12.0271,
			 11.0301, 10.2189, 9.5245, 8.9094, 8.3505,7.8325,7.3441},// free = 8
			{27.8767, 25.4625, 23.5893,21.6660,19.0228,16.9190,14.6837, 13.2880,
			 12.2421, 11.3887, 10.6564,10.0060,9.4136,8.8632,8.3428},// free = 9
			{29.5879, 27.1119, 25.1881, 23.2093,20.4832,18.3070,15.9872,14.5339,
			 13.4420,12.5489,11.7807,11.0971,10.4732,9.8922,9.3418},// free = 10
			{31.2635, 28.7291, 26.7569, 24.7250,21.9200,19.6752,17.2750,15.7671,
			 14.6314,13.7007,12.8987,12.1836,11.5298,10.9199,10.3410},// free=11
			{32.9092, 30.3182, 28.2997, 26.2170,23.3367,21.0261,18.5493,16.9893,
			15.8120,14.8454,14.0111,13.2661,12.5838,11.9463,11.3403},// free =12
			{34.5274, 31.8830, 29.8193, 27.6882,24.7356,22.3620,19.8119,18.2020,
			16.9848,15.9839,15.1187,14.3451,13.6356,12.9717,12.3398},// free =13
			{36.1239, 33.4262, 31.3194, 29.1412,26.1189,23.6848,21.0641,19.4062,
			18.1508,17.1169,16.2221,15.4209,14.6853,13.9961,13.3393},// free =14
			{37.6978, 34.9494, 32.8015, 30.5780,27.4884,24.9958,22.3071,20.6030,
			19.3107,18.2451,17.3217,16.4940,15.7332,15.0197,14.3389},// free =15
			{39.2518, 36.4555, 34.2671, 31.9999,28.8453,26.2962,23.5418,21.7931,
			20.4651,19.3689,18.4179,17.5646,16.7795,16.0425,15.3385},// free =16
			{40.7911, 37.9462, 35.7184, 33.4087,30.1910,27.5871,24.7690,22.9770,
			21.6146,20.4887,19.5110,18.6330,17.8244,17.0646,16.3382},// free =17
			{42.3119, 39.4220, 37.1564, 34.8052,31.5264,28.8693,25.9894,24.1555,
			22.7595,21.6049,20.6014,19.6993,18.8679,18.0860,17.3379},// free =18
			{43.8194, 40.8847, 38.5821, 36.1908,32.8523,30.1435,27.2036,25.3289,
			23.9004,22.7178,21.6891,20.7638,19.9102,19.1069,18.3376},// free =19
			{45.3142, 42.3358, 39.9969, 37.5663,34.1696,31.4104,28.4120,26.4976,
			25.0375,23.8277,22.7745,21.8265,20.9514,20.1272,19.3374},// free =20
			{46.7963, 43.7749, 41.4009, 38.9322,35.4789,32.6706,29.6151,27.6620,
			26.1711,24.9348,23.8578,22.8876,21.9915,21.1470,20.3372},// free =21
			{48.2676, 45.2041, 42.7957, 40.2894,36.7807,33.9245,30.8133,28.8224,
			27.3015,26.0393,24.9390,23.9473,23.0307,22.1663,21.3370},// free =22
			{49.7276, 46.6231, 44.1814, 41.6383,38.0756,35.1725,32.0069,29.9792,
			28.4288,27.1413,26.0184,25.0055,24.0689,23.1852,22.3369},// free =23
			{51.1790, 48.0336, 45.5584, 42.9798,39.3641,36.4150,33.1962,31.1325,
			29.5533,28.2412,27.0960,26.0625,25.1064,24.2037,23.3367},// free =24
			{52.6187, 49.4351, 46.9280, 44.3140,40.6465,37.6525,34.3816,32.2825,
			30.6752,29.3388,28.1719,27.1183,26.1430,25.2218,24.3366},// free =25
			{54.0511, 50.8291, 48.2898, 45.6416,41.9231,38.8851,35.5632,33.4295,
			31.7946,30.4346,29.2463,28.1730,27.1789,26.2395,25.3365},// free =26
			{55.4751, 52.2152, 49.6450, 46.9628,43.1945,40.1133,36.7412,34.5736,
			32.9117,31.5284,30.3193,29.2266,28.2141,27.2569,26.3363},// free =27
			{56.8918, 53.5939, 50.9936, 48.2782,44.4608,41.3372,37.9159,35.7150,
			34.0266,32.6205,31.3909,30.2791,29.2486,28.2740,27.3362},// free =28
			{58.3006, 54.9662, 52.3355, 49.5878,45.7223,42.5569,39.0875,36.8538,
			35.1394,33.7109,32.4612,31.3308,30.2825,29.2908,28.3361}};// free=29
	
	private double[][] contingencyTable;
	
	private String varName1;
	
	private String varName2;
	
	private int numStates1;
	
	private int numStates2;
	
	private double jiSquare;
	
	private double alpha;
	
	private int alphaIndex = -1;
	
	// Constructor
	/** @param data. <code>int[][]</code>
	 * @param variables. Indicates which variables will be used for test.
	 *  <code>boolean[]</code> 
	 * @argCondition <code>varName1</code> and <code>varName2</code> must 
	 *  exist in <code>variablesNames</code> */
	public JiSquare(int[][] data, String varName1, String varName2, 
			double alpha, ArrayList<String> variablesNames) 
			throws Exception, ProbNodeNotFoundException {
		this.varName1 = varName1;
		this.varName2 = varName2;
		int posVar1 = -1; 
		int posVar2 = -1;
		for (int i = 0; i < variablesNames.size(); i++) {
			if (variablesNames.get(i).contentEquals(varName1)) {
				posVar1 = i;
			} else {
				if (variablesNames.get(i).contentEquals(varName2)) {
					posVar2 = i;
				}
			}
		}
		if ((posVar2 == -1)) {
			throw new Error("Error in class JiSquare: " + varName1 + 
					" not found in the list of variables.");
		}
		if ((posVar1 == -1)) {
			throw new Error("Error in class JiSquare: " + varName2 + 
			" not found in the list of variables.");
		}
		contingencyTable = getContingencyTable(data, posVar1, posVar2);
		jiSquare = -1;
		for (int i = 0; i < alphas.length; i++) {
			if (alpha == alphas[i]) {
				this.alpha = alpha;
				alphaIndex = i;
				break;
			}
		}
		if (alphaIndex == -1) {
			throw new Exception("No data for alpha = " + alpha);
		}
	}

	// Methods
	/** @return Ji-Square value for two variables. <code>double</double> */
	public double getJiSquare() {
		double[][] expectedFrecuencies = getExpectedFrecuencies();
		jiSquare = 0;
		int numRows = contingencyTable.length - 1;
		int numColumns = contingencyTable[0].length - 1;
		double total = contingencyTable[numStates1][numStates2];
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numColumns; j++) {
				double aux = contingencyTable[i][j] - expectedFrecuencies[i][j];
				jiSquare += aux * aux / total;
			}
		}
		return jiSquare;
	}
	
	/** @return <code>true</double> if the two variables have a correlation.
	 * <code>boolean</double> */
	public boolean relatedVariables() throws Exception {
		if (jiSquare == -1) {
			getJiSquare();
		}
		int freedomDegrees = (contingencyTable.length - 2) * 
			(contingencyTable[0].length - 2);
		if ((freedomDegrees - 1) > jiTable.length) {
			throw new Exception("Too many freedom degrees in ji-square test");
		}
		return jiSquare < jiTable[freedomDegrees - 1][alphaIndex];
	}
	
	/** @return Free degrees. <code>int</double> */
	public int getNumFreeDegrees() {
		return (contingencyTable.length - 2) * (contingencyTable[0].length - 1);
	}
	
	/** @return A table <code>double[][]</double> */
	private double[][] getExpectedFrecuencies() {
		int numRows = contingencyTable.length - 1;
		int numColumns = contingencyTable[0].length - 1;
		double[][] expectedFrecuencies = new double[numRows][numColumns];
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numColumns; j++) {
				expectedFrecuencies[i][j] = (contingencyTable[i][numStates2] * 
					contingencyTable[numStates1][j]) 
					/ contingencyTable[numStates1][numStates2]; 
			}
		}
		return expectedFrecuencies;
	}

	/** @param data. <code>int[][]</code>
	 * @param numVariable1. <code>int</code>
	 * @param numVariable2. <code>int</code>
	 * @return Contingency table.
	 *  <code>int[numStates variable1 + 1][numStates variable2 + 1]</code> */
	private double[][] getContingencyTable(
			int[][] data, int numVariable1, int numVariable2) {
		// Create table.
		numStates1 = getNumStates(data, numVariable1);
		numStates2 = getNumStates(data, numVariable2);
		double[][] contingencyTable = new double[numStates1 + 1][numStates2 +1];
		// Go round the table to create the table
		for (int i = 0; i < data.length; i++) {
			int state1 = data[i][numVariable1];
			int state2 = data[i][numVariable2];
			contingencyTable[state1][state2] = 
				contingencyTable[state1][state2] + 1;
			contingencyTable[numStates1][state2] = 
				contingencyTable[numStates1][state2] + 1;
			contingencyTable[state1][numStates2] = 
				contingencyTable[state1][numStates2] + 1;
			contingencyTable[numStates1][numStates2] = 
				contingencyTable[numStates1][numStates2] + 1;
		}
		return contingencyTable;
	}

	/** @param data. <code>int[][]</code>
	 * @param numVariable. <code>int</code>
	 * @return Number of different values in variable.
	 * @argCondition Each variable value is an integer (0..numStates-1). */
	private int getNumStates(int[][] data, int numVariable) {
		int max = 0;
		for (int i = 0; i < data.length; i++) {
			if (data[i][numVariable] > max) {
				max = data[i][numVariable];
			}
		}
		return max + 1; // because the first state is numbered as 0.
	}
    
	public String toString() {
		String out = new String("Variables: " + varName1 + ", " + 
				varName2 + ". ");
		double jiSquare = getJiSquare();
		double significance = (1 - alpha) * 100;
		String sSignificance = Double.toString(significance);
		int pointPosition = sSignificance.indexOf('.');
		int iSignificance = Integer.parseInt(
				sSignificance.substring(0, pointPosition));
		out = out + " Ji-Square = " + jiSquare + ". Significance = " 
				+ iSignificance + "%\n";
		int freedomDegrees = 0;
		try {
			freedomDegrees = (contingencyTable.length - 2) * 
				(contingencyTable[0].length - 2);
			boolean accepted = relatedVariables();
			out = out + "Null hyphotesis ";
			if (accepted) {
				out = out + "accepted because " + jiSquare + " < " 
					+ jiTable[freedomDegrees - 1][alphaIndex];
			} else {
				out = out + "rejected because " + jiSquare + " > " 
					+ jiTable[freedomDegrees - 1][alphaIndex];
			}
			out = out + ". Freedom degrees = " + freedomDegrees + ".\n";
		} catch (Exception e) {
			if (freedomDegrees > (jiTable.length + 1)) {
				out = out + "\nThe table does not contain " + freedomDegrees 
					+ " freedomDegrees.";				
			} else {
				out = out + "\nThe table does not contain values for alpha = "
					+ alpha;
			}
		}
		return out;
	}
	
}