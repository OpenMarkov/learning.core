/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.algorithm;

import org.jetbrains.annotations.NotNull;
import org.openmarkov.core.exception.InvalidArgumentException;
import org.openmarkov.plugin.PluginSearch;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Stream;

/**
 * This class manages the learning algorithms.
 */
public class LearningAlgorithmManager {
    
    public static final LearningAlgorithmManager INSTANCE = new LearningAlgorithmManager();
    
    // Attributes
    private final List<Class<? extends LearningAlgorithm>> learningAlgorithms;
    
    public Stream<Class<? extends LearningAlgorithm>> getLearningAlgorithms() {
        return this.learningAlgorithms.stream();
    }
    
    // Constructor
    
    /**
     * Finds all learning algorithms using the plugin architecture, which means all those with the annotation
     * corresponding to {@code LearningAlgorithmType} and stores them in a map.
     */
    private LearningAlgorithmManager() {
        this.learningAlgorithms = LearningAlgorithmManager.findAllLearningAlgorithms().toList();
    }
    
    public static LearningAlgorithmType info(Class<? extends LearningAlgorithm> plugin) {
        return plugin.getAnnotation(LearningAlgorithmType.class);
    }
    
    public final LearningAlgorithm instanciateByClass(Class<? extends LearningAlgorithm> algorithmClass, List<Object> parameters) throws InvalidArgumentException {
        LearningAlgorithm instance = Arrays
                .stream(algorithmClass.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == parameters.size())
                .map(constructor -> {
                    try {
                        return (LearningAlgorithm) constructor.newInstance(parameters.toArray());
                    } catch (InstantiationException | IllegalAccessException |
                             InvocationTargetException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (instance == null) {
            throw new InvalidArgumentException("there is no Learning Algorithm that can be constructed with these arguments: " + parameters);
        }
        return instance;
    }
    
    
    /**
     * Finds all learning algorithms.
     *
     * @return a list of learning algorithms.
     */
    private static @NotNull Stream<Class<? extends LearningAlgorithm>> findAllLearningAlgorithms() {
        return PluginSearch.init()
                           .annotatedWith(LearningAlgorithmType.class)
                           .childrenOf(LearningAlgorithm.class)
                           .stream();
    }
    
}

