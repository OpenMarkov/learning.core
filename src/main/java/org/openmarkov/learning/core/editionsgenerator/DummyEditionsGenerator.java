/*
 * Copyright 2012 CISIAD, UNED, Spain Licensed under the European Union Public
 * Licence, version 1.1 (EUPL) Unless required by applicable law, this code is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.editionsgenerator;


/**
 * Dummy Editions Generator, does not return any edit. 
 * Thought for learning algorithms that are not structural, only parametric
 * 
 * @author ibermejo
 */
public class DummyEditionsGenerator extends EditionsGenerator
{

    @Override
    public LearningEditProposal getBest (boolean onlyAllowedEdits, boolean onlyPositiveEdits)
    {
        return null;
    }

    @Override
    public LearningEditProposal getNext (boolean onlyAllowedEdits, boolean onlyPositiveEdits)
    {
        return null;
    }
    
    @Override
    public boolean isLastPhase ()
    {
        return true;
    }


}
