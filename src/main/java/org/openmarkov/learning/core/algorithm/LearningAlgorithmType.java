/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.algorithm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) public @interface LearningAlgorithmType {

    String name(); // The name will be stored in the annotation of the algorithm.

    boolean discriminative();

    boolean supportsUnobservedVariables();

    /**
     * Names of the metrics this algorithm uses, matching {@code @MetricType(name = ...)}.
     * Order follows the algorithm constructor signature (first metric = first constructor arg).
     * Empty (default) means the algorithm does not use scoring metrics.
     */
    String[] metrics() default {};

}
