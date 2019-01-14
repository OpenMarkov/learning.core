/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.preprocess.exception;

import org.openmarkov.core.exception.OpenMarkovException;

/**
 * Thrown when the minimum is under the left limit of the first interval,
 * or the maximum is greater than the right limit of the last interval,
 *
 * @author Iñigo
 */
@SuppressWarnings("serial") public class WrongDiscretizationLimitException extends OpenMarkovException {

}
