/*
* Copyright 2011 CISIAD, UNED, Spain
*
* Licensed under the European Union Public Licence, version 1.1 (EUPL)
*
* Unless required by applicable law, this code is distributed
* on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
*/

package org.openmarkov.learning.core.editionsgenerator;

import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.ConstraintViolationException;

/** An <code>EditAndScorePair</code> stores a <code>PNEdit</code> and the
 * increment of score associated to this edition. Also it stores a pointer
 * to the constraint violated by this edition.
 * @author joliva
 * @author manuel
 * @author fjdiez
 * @version 1.0
 * @since Carmen 1.0 */
public class EditAndScorePair {

    protected PNEdit edition;
    
    protected double score;
    
    protected ConstraintViolationException violatedConstraint;
    
    public EditAndScorePair(PNEdit edition, double score){
        this.edition = edition; 
        this.score = score;
        this.violatedConstraint = null;
    }
    
    public EditAndScorePair(PNEdit edition, double score, ConstraintViolationException  e){
        this(edition, score);
        this.violatedConstraint = e;
    }    
    
    public PNEdit getEdition(){
        return edition;
    }
    
    public double getScore(){
        return score;
    }
    
    public Exception getViolatedConstraint(){
    	return violatedConstraint;
    }
    
    public void setViolatedConstraint(ConstraintViolationException violatedConstraint){
    	this.violatedConstraint = violatedConstraint;
    }
    
    public boolean isAllowed()
    {
    	return violatedConstraint == null;
    }
    
    public boolean equals(Object obj){
        if(this == obj)
            return true;
        if((obj == null) || (obj.getClass() != this.getClass()))
            return false;
        return (this.edition.equals(((EditAndScorePair)obj).edition));
    }
    
    public String toString()
    {
        return new StringBuilder().append (edition.toString () + " " + score).toString (); 
    }
}
