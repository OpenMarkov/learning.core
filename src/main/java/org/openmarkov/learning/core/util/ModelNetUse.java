/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.util;

/**
 * Configuration for how a model network constrains the learning process.
 * Controls whether to use node positions, start from the model network structure,
 * and which link operations are allowed.
 */
public final class ModelNetUse {
    
    private final boolean useModelNet;
    private final boolean useNodePositions;
    private final boolean startFromModelNet;
    private final boolean allowLinkAddition;
    private final boolean allowLinkRemoval;
    private final boolean allowLinkInversion;

	/**
	 * Constructs a ModelNetUse configuration with the specified options.
	 *
	 * @param useModelNet        whether to use the model network at all
	 * @param useNodePositions   whether to copy node positions from the model network
	 * @param startFromModelNet  whether to start learning from the model network structure
	 * @param allowLinkAddition  whether to allow adding links during learning
	 * @param allowLinkRemoval   whether to allow removing links during learning
	 * @param allowLinkInversion whether to allow inverting links during learning
	 */
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

	/**
	 * Constructs a default ModelNetUse with all options disabled.
	 */
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
