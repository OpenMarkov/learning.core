/*
* Copyright 2011 CISIAD, UNED, Spain
*
* Licensed under the European Union Public Licence, version 1.1 (EUPL)
*
* Unless required by applicable law, this code is distributed
* on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
*/

package org.openmarkov.learning.core.io;

import java.util.List;

import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

public class CaseDatabase
{
    private ProbNet probNet;
    private List<Variable> variables;
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
        this.variables = probNet.getVariables ();
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

    /**
     * Returns the variables.
     * @return the variables.
     */
    public List<Variable> getVariables ()
    {
        return variables;
    }

}
