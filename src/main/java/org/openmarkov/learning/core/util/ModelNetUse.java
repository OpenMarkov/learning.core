/*
* Copyright 2011 CISIAD, UNED, Spain
*
* Licensed under the European Union Public Licence, version 1.1 (EUPL)
*
* Unless required by applicable law, this code is distributed
* on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
*/

package org.openmarkov.learning.core.util;

public class ModelNetUse {
	
	private boolean useModelNet;
	private boolean useNodesModelNet;
	private boolean addLinkModelNet;
	private boolean deleteLinksModelNet;
	private boolean invertLinksModelNet;
	
	public ModelNetUse(boolean useModelNet, boolean useNodesModelNet, boolean addLinkModelNet, 
						boolean deleteLinksModelNet, boolean invertLinksModelNet)
	{
		this.useModelNet = useModelNet;
		this.useNodesModelNet = useNodesModelNet;
		this.addLinkModelNet = addLinkModelNet;
		this.deleteLinksModelNet = deleteLinksModelNet;
		this.invertLinksModelNet = invertLinksModelNet;
	}
	
    public ModelNetUse()
   {
       this(false, false, false, false, false);
   }	

	/**
	 * @return the useModelNet
	 */
	public boolean isUseModelNet() {
		return useModelNet;
	}

	/**
	 * @param useModelNet the useModelNet to set
	 */
	public void setUseModelNet(boolean useModelNet) {
		this.useModelNet = useModelNet;
	}

	/**
	 * @return the useNodesModelNet
	 */
	public boolean isOnlyUseNodes() {
		return useNodesModelNet;
	}

	/**
	 * @param useOnlyNodes the useNodesModelNet to set
	 */
	public void setOnlyUseNodes(boolean useOnlyNodes) {
		this.useNodesModelNet = useOnlyNodes;
	}

	/**
	 * @return the addLinkModelNet
	 */
	public boolean isAddLinksAllowed() {
		return addLinkModelNet;
	}

	/**
	 * @param addLinkModelNet the addLinkModelNet to set
	 */
	public void setAddLinksAllowed(boolean addLinkModelNet) {
		this.addLinkModelNet = addLinkModelNet;
	}

	/**
	 * @return the deleteLinksModelNet
	 */
	public boolean isDeleteLinksAllowed() {
		return deleteLinksModelNet;
	}

	/**
	 * @param deleteLinksModelNet the deleteLinksModelNet to set
	 */
	public void setDeleteLinksAllowed(boolean deleteLinksModelNet) {
		this.deleteLinksModelNet = deleteLinksModelNet;
	}

	/**
	 * @return the invertLinksModelNet
	 */
	public boolean isInvertLinksAllowed() {
		return invertLinksModelNet;
	}

	/**
	 * @param invertLinksModelNet the invertLinksModelNet to set
	 */
	public void setInvertLinksAllowed(boolean invertLinksModelNet) {
		this.invertLinksModelNet = invertLinksModelNet;
	}
	
}
