/*
* Copyright 2013 CISIAD, UNED, Spain
*
* Licensed under the European Union Public Licence, version 1.1 (EUPL)
*
* Unless required by applicable law, this code is distributed
* on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
*/
package org.openmarkov.learning.core.exception;

import java.util.List;

import org.openmarkov.core.model.network.Variable;

@SuppressWarnings("serial")
public class UnobservedVariablesException extends Exception
{
    List<Variable> unobservedVariables;
    
    public UnobservedVariablesException(List<Variable> unobservedVariables)
    {
        this.unobservedVariables = unobservedVariables;
    }

    public List<Variable> getUnobservedVariables ()
    {
        return unobservedVariables;
    }
}
