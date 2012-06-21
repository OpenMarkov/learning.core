/*
* Copyright 2011 CISIAD, UNED, Spain
*
* Licensed under the European Union Public Licence, version 1.1 (EUPL)
*
* Unless required by applicable law, this code is distributed
* on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
*/

package org.openmarkov.learning.core.io;

import org.openmarkov.core.model.network.ProbNet;

public class CaseDatabase
{
    private ProbNet probNet;
    private int[][] cases;

    /**
     * Constructor for CaseDatabase.
     * @param probNet
     * @param cases
     */
    public CaseDatabase (ProbNet probNet, int[][] cases)
    {
        super ();
        this.probNet = probNet;
        this.cases = cases;
    }
    
    /**
     * Returns the probNet.
     * @return the probNet.
     */
    public ProbNet getProbNet ()
    {
        return probNet;
    }
    /**
     * Returns the cases.
     * @return the cases.
     */
    public int[][] getCases ()
    {
        return cases;
    }

}
