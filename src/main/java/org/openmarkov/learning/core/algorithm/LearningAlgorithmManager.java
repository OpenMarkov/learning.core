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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class manages the learning algorithms.
 */
public class LearningAlgorithmManager {
    
    // Attributes
    private final HashMap<String, Class<LearningAlgorithm>> learningAlgorithms;
    
    // Constructor
    
    /**
     * Finds all learning algorithms using the plugin architecture, which means all those with the annotation
     * corresponding to {@code LearningAlgorithmType} and stores them in a map.
     */
    @SuppressWarnings("unchecked") public LearningAlgorithmManager() {
        learningAlgorithms = new HashMap<>();
        findAllLearningAlgorithms().forEach(plugin->{
            LearningAlgorithmType lAnnotation = plugin.getAnnotation(LearningAlgorithmType.class);
            learningAlgorithms.put(lAnnotation.name(), plugin);
        });
    }
    
    /**
     * Returns the class of the learning algorithm whose name is passed
     *
     * @param name the algorithm name.
     * @return a learning algorithm class
     */
    public final Class<? extends LearningAlgorithm> getClassByName(String name) {
        return learningAlgorithms.get(name);
    }
    
    /**
     * Returns a learning algorithm by name.
     *
     * @param name       the algorithm name.
     * @param parameters the parameters of the algorithm constructor.
     * @return a learning algorithm.
     */
    public final LearningAlgorithm getByName(String name, List<Object> parameters) throws InvalidArgumentException {
        LearningAlgorithm instance = Arrays
                .stream(this.learningAlgorithms.get(name).getConstructors())
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
        if (instance == null)
            throw new InvalidArgumentException("there is no Learning Algorithm that can be constructed with said arguments");
        return instance;
    }
    
    /**
     * Returns all learning algorithm names.
     *
     * @return a list of learning algorithms.
     */
    public final Set<String> getLearningAlgorithmNames() {
        return learningAlgorithms.keySet();
    }
    
    public final Set<String> getDiscriminativeLearningAlgorithmNames() {
        return learningAlgorithms.values().stream()
                                 .filter(s -> s.getAnnotation(LearningAlgorithmType.class).discriminative())
                                 .map(aClass -> aClass.getAnnotation(LearningAlgorithmType.class).name())
                                 .collect(Collectors.toSet());
    }
    
    /**
     * Finds all learning algorithms.
     *
     * @return a list of learning algorithms.
     */
    private static @NotNull Stream<Class<LearningAlgorithm>> findAllLearningAlgorithms() {
        return PluginSearch.init()
                           .annotatedWith(LearningAlgorithmType.class)
                           .childrenOf(LearningAlgorithm.class)
                           .stream();
    }
    
}

