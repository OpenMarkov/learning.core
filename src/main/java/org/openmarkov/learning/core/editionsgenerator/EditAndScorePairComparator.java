/*
* Copyright 2011 CISIAD, UNED, Spain
*
* Licensed under the European Union Public Licence, version 1.1 (EUPL)
*
* Unless required by applicable law, this code is distributed
* on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
*/

package org.openmarkov.learning.core.editionsgenerator;

import java.util.Comparator;

import org.openmarkov.core.action.AddLinkEdit;
import org.openmarkov.core.action.InvertLinkEdit;
import org.openmarkov.core.action.RemoveLinkEdit;


/** Compares two <code>EditAndScorePair</code> attending to their scores.
 * If the scores are the same, the order is: AddLinkEdit, RemoveLinkEdit and
 * InvertLinkEdit. If the editions are of the same class, we compare the 
 * source and destination variables alphabetically
 * @author joliva
 * @author manuel
 * @author fjdiez
 * @version 1.0
 * @since Carmen 1.0 */
public class EditAndScorePairComparator implements Comparator{

	/**
	 * Compares two <code>EditAndScorePair</code> attending to their scores.
	 * If the two scores are the same, we establish the order attending
	 * to the edition class: addLink < removeLink < InvertLink. If the two
	 * editions are of the same class, we use an alphabetical order.
	 */
    public int compare(Object o1, Object o2) {
        if(((EditAndScorePair) o1).getScore() < 
                ((EditAndScorePair) o2).getScore())
            return -1;
        if(((EditAndScorePair) o1).getScore() > 
                ((EditAndScorePair) o2).getScore())
            return 1;
        /* When reaching this point, we know the two pairs have the same score,
        * so we have to establish an order to put and search in the tree*/
        else{ 
            return compareEqualScore((EditAndScorePair)o1, (EditAndScorePair)o2);
        }
    }
    
    /**
     * Compares two <code>EditAndScorePair</code> that have the same scores.
	 * we establish the order attending to the edition class: addLink < 
	 * removeLink < InvertLink. If the two editions are of the same class, 
	 * we use an alphabetical order.
     * @param o1
     * @param o2
     * @return
     */
    private int compareEqualScore(EditAndScorePair o1, EditAndScorePair o2){
        
        /* If the editions are not of the same class, we give preference to
         * addition and then remove and inversion*/
        if (o1.getEdition().getClass() != o2.getEdition().getClass()){
            if (o1.getEdition().getClass() == AddLinkEdit.class)
                return 1;
            else if ((o1.getEdition().getClass() == RemoveLinkEdit.class))
                return 1;
            else
            	return -1;
        }
        /* If the editions are of the same class, we compare alphabetically 
         * its variables. */
        else{
            if(o1.getEdition().getClass() == AddLinkEdit.class)
                return ((AddLinkEdit)o1.getEdition()).compareTo(
                        ((AddLinkEdit)o2.getEdition()));
            else if(o1.getEdition().getClass() == RemoveLinkEdit.class)
                return ((RemoveLinkEdit)o1.getEdition()).compareTo(
                        ((RemoveLinkEdit)o2.getEdition()));
            else
            	return ((InvertLinkEdit)o1.getEdition()).compareTo(
                        ((InvertLinkEdit)o2.getEdition()));
        }
    }

}
