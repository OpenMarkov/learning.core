/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.util;

import org.openmarkov.core.action.base.PNEdit;

import java.util.Objects;

/**
 * An {@code LearningEditProposal} stores a {@code PNEdit} and the
 * increment of score associated to this edition. Also it stores a pointer
 * to the constraint violated by this edition.
 *
 * @author joliva
 * @author manuel
 * @author fjdiez
 * @version 1.0
 * @since OpenMarkov 1.0
 */
public class LearningEditProposal implements Comparable<LearningEditProposal> {

	protected final PNEdit edit;

	protected final LearningEditMotivation motivation;

	/**
	 * Constructs a learning edit proposal with the given edit and its motivation.
	 *
	 * @param edit       the network edit
	 * @param motivation the motivation (score or description) for this edit
	 */
	public LearningEditProposal(PNEdit edit, LearningEditMotivation motivation) {
		this.edit = edit;
		this.motivation = motivation;
	}

	public PNEdit getEdit() {
		return edit;
	}

	public LearningEditMotivation getMotivation() {
		return motivation;
	}
    
    /**
     * Returns whether this edit proposal is allowed. Always returns true.
     *
     * @return true
     */
    public static boolean isAllowed() {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj == null) || (obj.getClass() != this.getClass()))
			return false;
		LearningEditProposal other = (LearningEditProposal) obj;
		return this.edit.equals(other.edit) && this.motivation.equals(other.motivation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(edit, motivation);
	}

	public String toString() {
        return edit.toString() + " " + motivation;
	}

	@Override public int compareTo(LearningEditProposal editProposal) {
		return motivation.compareTo(editProposal.getMotivation());
	}
}
