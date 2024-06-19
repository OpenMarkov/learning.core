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

 // The <code>Retention</code> annotation specifies how the marked annotation is stored:
 // <code>RetentionPolicy.RUNTIME</code> means that the annotation will be available at runtime.
 @Retention(RetentionPolicy.RUNTIME)

 // The @Target annotation indicates the contexts in which an annotation is applicable.
 // ElementType.TYPE means that the annotation can be applied to any element of a type (class, interface, enum)
 @Target(ElementType.TYPE)

 // The interface annotated <code>LearningAlgorithmType</code>
 // is a special interface that allows metadata to be added to classes, methods, variables, etc.
public @interface LearningAlgorithmType {

	String name(); // The name will be stored in the annotation of the algorithm.

	boolean supportsUnobservedVariables() default false;

}
