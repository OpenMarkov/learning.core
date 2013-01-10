/*
* Copyright 2011 CISIAD, UNED, Spain
*
* Licensed under the European Union Public Licence, version 1.1 (EUPL)
*
* Unless required by applicable law, this code is distributed
* on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
*/

package org.openmarkov.learning.core.editionsgenerator;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.ConstraintViolationException;


/**
 * This interface defines the basic elements of a generator of possible
 * editions for the interactive learning.
 * @author joliva
 * @author manuel
 * @author fjdiez
 * @author ibermejo
 * @version 1.1
 */
public abstract class EditionsGenerator {
	

    /** List of blocked edits */
    protected List<PNEdit> blockedEdits = new ArrayList<PNEdit>();	
    
    protected int phase = 0;

	/**
	 * This method returns the best edition (and its associated score)
	 * that can be done to the network that is being learnt. 
	 * 
	 * @param onlyAllowedEditions If this parameter is true, only those editions
	 * that do not provoke a ConstraintViolationException are returned
	 * @param onlyPositiveEditions If this parameter is true, only those 
	 * editions with a positive associated score are returned.
	 * @return <code>LearningEditProposal</code> with the best edition and its score. 
	 */
    public abstract LearningEditProposal getBest (boolean onlyAllowedEdits,
                                     boolean onlyPositiveEdits);
    
    /**
     * This method returns the next best edition (and its associated score)
     * that can be done to the network that is being learnt. 
     * 
     * @param onlyAllowedEditions If this parameter is true, only those editions
     * that do not provoke a ConstraintViolationException are returned
     * @param onlyPositiveEditions If this parameter is true, only those 
     * editions with a positive associated score are returned.
     * @return <code>LearningEditProposal</code> with the best edition and its score. 
     */
    public abstract LearningEditProposal getNext (boolean onlyAllowedEdits,
                                     boolean onlyPositiveEdits);
    
    public abstract void resetHistory ();
       
    /**
     * Blocks edit
     * @param edit to block
     */
    public void blockEdit(PNEdit edit)
    {
    	blockedEdits.add(edit);
    }
    
    /**
     * Blocks edit
     * @param edit to block
     */
    public void unblockEdit(PNEdit edit)
    {
    	blockedEdits.remove(edit);
    }

	/**
	 * @return the blockedEdits
	 */
	public List<PNEdit> getBlockedEdits() {
		return blockedEdits;
	}    
	
    /**
     * Blocks edit
     * @param edit to block
     */
    public boolean isBlocked(PNEdit edit)
    {
    	return blockedEdits.contains(edit);
    }    
    
    protected boolean isAllowed(PNEdit edit)
    {
        boolean isAllowed = true;
        try
        {
            //Announce edit to check whether it is allowed or not
            try
            {
                edit.getProbNet ().getPNESupport ().announceEdit (edit);
            }
            catch (ConstraintViolationException e)
            {
                isAllowed = false;
            }
        }
        catch (Exception e1)
        {
            e1.printStackTrace ();
        }       
        return isAllowed;
    }    
    
    public boolean isLastPhase ()
    {
    	return true;
    }
    
    public int getPhase ()
    {
    	return phase;
    }
}
