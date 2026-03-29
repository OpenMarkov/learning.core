/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.util;

/**
 * A text-based motivation for a learning edit, used when a descriptive reason
 * (rather than a numeric score) is associated with the edit.
 */
public class StringEditMotivation extends LearningEditMotivation {
	private String motivation;

	/**
	 * Constructs a StringEditMotivation with the given textual description.
	 *
	 * @param motivation the text describing the motivation for the edit
	 */
	public StringEditMotivation(String motivation) {
		this.motivation = motivation;
	}

	@Override public int compareTo(LearningEditMotivation edit) {
		return 0;
	}

	@Override public String toString() {
		return motivation;
	}

}
