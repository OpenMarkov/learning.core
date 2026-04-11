/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.algorithm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger logger = LogManager.getLogger(LearningAlgorithmManager.class);

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
    
    /**
     * Returns the {@code LearningAlgorithmType} annotation for the given algorithm class.
     *
     * @param plugin the algorithm class to inspect
     * @return the {@code LearningAlgorithmType} annotation, or null if not present
     */
    public static LearningAlgorithmType info(Class<? extends LearningAlgorithm> plugin) {
        return plugin.getAnnotation(LearningAlgorithmType.class);
    }

    /**
     * Returns all registered generative (non-discriminative) learning algorithms.
     */
    public Stream<Class<? extends LearningAlgorithm>> getGenerativeAlgorithms() {
        return learningAlgorithms.stream()
                .filter(a -> !info(a).discriminative());
    }

    /**
     * Returns all registered discriminative learning algorithms.
     */
    public Stream<Class<? extends LearningAlgorithm>> getDiscriminativeAlgorithms() {
        return learningAlgorithms.stream()
                .filter(a -> info(a).discriminative());
    }
    
    /**
     * Instantiates a learning algorithm by its class, using reflection to find a constructor
     * that matches the given parameters by count.
     *
     * @param algorithmClass the class of the learning algorithm to instantiate
     * @param parameters     the constructor arguments
     * @return the instantiated learning algorithm
     * @throws InvalidArgumentException if no matching constructor is found
     */
    public final LearningAlgorithm instantiateByClass(Class<? extends LearningAlgorithm> algorithmClass, List<Object> parameters) {
        LearningAlgorithm instance = Arrays
                .stream(algorithmClass.getConstructors())
                .filter(constructor -> isTypeCompatible(constructor.getParameterTypes(), parameters))
                .map(constructor -> {
                    try {
                        return (LearningAlgorithm) constructor.newInstance(parameters.toArray());
                    } catch (InstantiationException | IllegalAccessException |
                             InvocationTargetException e) {
                        logger.warn("Failed to instantiate {} via constructor with {} parameters: {}",
                                algorithmClass.getSimpleName(), parameters.size(), e.getMessage(), e);
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

    private static boolean isTypeCompatible(Class<?>[] paramTypes, List<Object> parameters) {
        if (paramTypes.length != parameters.size()) return false;
        for (int i = 0; i < paramTypes.length; i++) {
            Object param = parameters.get(i);
            if (param == null) continue;
            Class<?> expected = wrap(paramTypes[i]);
            if (!expected.isAssignableFrom(param.getClass())) {
                return false;
            }
        }
        return true;
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = Map.of(
            boolean.class, Boolean.class,
            int.class, Integer.class,
            long.class, Long.class,
            double.class, Double.class,
            float.class, Float.class,
            byte.class, Byte.class,
            short.class, Short.class,
            char.class, Character.class
    );

    private static Class<?> wrap(Class<?> type) {
        return type.isPrimitive() ? PRIMITIVE_TO_WRAPPER.get(type) : type;
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

