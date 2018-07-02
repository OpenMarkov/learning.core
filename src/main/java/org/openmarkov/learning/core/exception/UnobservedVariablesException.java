/*
 * Copyright (c) CISIAD, UNED, Spain,  2018. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.learning.core.exception;

import org.openmarkov.core.exception.OpenMarkovException;
import org.openmarkov.core.model.network.Variable;

import java.util.List;

@SuppressWarnings("serial") public class UnobservedVariablesException extends OpenMarkovException {
	List<Variable> unobservedVariables;

	public UnobservedVariablesException(List<Variable> unobservedVariables) {
		this.unobservedVariables = unobservedVariables;
	}

	public List<Variable> getUnobservedVariables() {
		return unobservedVariables;
	}
}
