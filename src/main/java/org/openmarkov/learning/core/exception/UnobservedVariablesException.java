/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.learning.core.exception;

import org.openmarkov.core.exception.OpenMarkovException;
import org.openmarkov.core.exception.IBundledOpenMarkovException;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.learning.core.algorithm.LearningAlgorithm;
import org.openmarkov.learning.core.algorithm.LearningAlgorithmType;

import java.util.List;

public class UnobservedVariablesException extends OpenMarkovException {
    
    public UnobservedVariablesException(Class<? extends LearningAlgorithm> algorithmClass, List<Variable> latentVariables) {
        this.algorithmType = algorithmClass.getAnnotation(LearningAlgorithmType.class);
        this.algorithmClass = algorithmClass;
        this.unobservedVariables = latentVariables;
    }
    
    public final Class<? extends LearningAlgorithm> algorithmClass;
    public final List<Variable> unobservedVariables;
    public final LearningAlgorithmType algorithmType;
    
    @Override public String toString() {
        return IBundledOpenMarkovException.toString(this);
    }
}

