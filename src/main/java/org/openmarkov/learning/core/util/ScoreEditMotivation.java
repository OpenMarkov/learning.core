/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * This class represents the motivation of an edit based on a score.
 */
public class ScoreEditMotivation extends LearningEditMotivation {

	private final double score;
    
    private static final int DEFAULT_NUM_DECIMALS = 6;
	private final int numDecimals;

	// Constructor
	/**
	 * @param score the score of this motivation.
	 */
	public ScoreEditMotivation(double score) {
		this.score = score;
		this.numDecimals = DEFAULT_NUM_DECIMALS;
	}

	/**
	 * Constructs a ScoreEditMotivation with the given score and number of decimal places for display.
	 *
	 * @param score      the score of this motivation
	 * @param numDecimals the number of decimal places for string representation
	 */
	public ScoreEditMotivation(double score, int numDecimals) {
		this.score = score;
		this.numDecimals = numDecimals;
	}

	/**
	 * Compares this ScoreEditMotivation with another LearningEditMotivation.
	 *
	 * @param edit the LearningEditMotivation to compare with.
	 * @return a negative integer, zero, or a positive integer as this object
	 *         is less than, equal to, or greater than the specified object.
	 */
	@Override public int compareTo(LearningEditMotivation edit) {
		if (edit instanceof ScoreEditMotivation scoreEditMotivation) {
			return Double.compare(this.score, scoreEditMotivation.score);
		}
		return 0;
    }

	/**
	 * Returns a string representation of this score, rounded to numDecimals (default = 2) decimal places.
	 *
	 * @return a string representation of the score.
	 */
	@Override public String toString() {
		return new BigDecimal(score).setScale(numDecimals, RoundingMode.FLOOR).toString();
	}

	/**
	 * @return the score.
	 */
	public double getScore() {
		return score;
	}
}
