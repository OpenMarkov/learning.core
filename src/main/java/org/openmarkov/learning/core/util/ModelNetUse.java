/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.util;

public final class ModelNetUse {
    
    private final boolean useModelNet;
    private final boolean useNodePositions;
    private final boolean startFromModelNet;
    private final boolean allowLinkAddition;
    private final boolean allowLinkRemoval;
    private final boolean allowLinkInversion;

	public ModelNetUse(boolean useModelNet, boolean useNodePositions, boolean startFromModelNet,
			boolean allowLinkAddition, boolean allowLinkRemoval, boolean allowLinkInversion) {
		this.useNodePositions = useNodePositions;
		this.startFromModelNet = startFromModelNet;
		this.allowLinkAddition = allowLinkAddition;
		this.allowLinkRemoval = allowLinkRemoval;
		this.allowLinkInversion = allowLinkInversion;
		if (!useNodePositions && !startFromModelNet) {
			this.useModelNet = false;
        } else {
            this.useModelNet = useModelNet;
        }
	}

	public ModelNetUse() {
		this(false, false, false, false, false, false);
	}

	/**
	 * @return the useModelNet
	 */
	public boolean isUseModelNet() {
		return useModelNet;
	}
    
    /**
	 * @return the useNodesModelNet
	 */
	public boolean isUseNodePositions() {
		return useNodePositions;
	}

	public boolean isStartFromModelNet() {
		return startFromModelNet;
	}
    
    /**
	 * @return the addLinkModelNet
	 */
	public boolean isLinkAdditionAllowed() {
		return allowLinkAddition;
	}
    
    /**
	 * @return the deleteLinksModelNet
	 */
	public boolean isLinkRemovalAllowed() {
		return allowLinkRemoval;
	}
    
    /**
	 * @return the allowLinkInversion
	 */
	public boolean isLinkInversionAllowed() {
		return allowLinkInversion;
	}
 
}
