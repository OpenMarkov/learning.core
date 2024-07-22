/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.algorithm;

import org.openmarkov.plugin.PluginLoader;
import org.openmarkov.plugin.service.FilterIF;
import org.openmarkov.plugin.service.PluginLoaderIF;

import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Constructor;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This class manages the learning algorithms.
 */
public class LearningAlgorithmManager {

	// Attributes
	private final PluginLoaderIF pluginsLoader;
	private final HashMap<String, Class<? extends LearningAlgorithm>> learningAlgorithms;

	// Constructor
	/**
	 * Finds all learning algorithms using the plugin architecture, which means all those with the annotation
	 * corresponding to <code>LearningAlgorithmType</code> and stores them in a map.
	 */
	@SuppressWarnings("unchecked") public LearningAlgorithmManager() {
		super();
		this.pluginsLoader = new PluginLoader();
		learningAlgorithms = new HashMap<>();

		for (Class<?> plugin : findAllLearningAlgorithms()) {
			// Uses the plugin architecture to find all learning algorithms annotated with LearningAlgorithmType
			LearningAlgorithmType lAnnotation = plugin.getAnnotation(LearningAlgorithmType.class);
			if (LearningAlgorithm.class.isAssignableFrom(plugin)) {
				learningAlgorithms.put(lAnnotation.name(), (Class<? extends LearningAlgorithm>) plugin);
			} else {
				throw new AnnotationFormatError(
						"LearningAlgorithmType annotation must be in a class that extends LearningAlgorithm");
			}
		}
	}

	/**
	 * Returns the class of the learning algorithm whose name is passed
	 *
	 * @param name the algorithm name.
	 * @return a learning algorithm class
	 */
	public final Class<? extends LearningAlgorithm> getByName(String name) {

		return learningAlgorithms.get(name);
	}

	/**
	 * Returns a learning algorithm by name.
	 *
	 * @param name the algorithm name.
	 * @param parameters the parameters of the algorithm constructor.
	 * @return a learning algorithm.
	 */
	public final LearningAlgorithm getByName(String name, List<Object> parameters) {
		LearningAlgorithm instance = null;
		try {
			Constructor<?>[] constructors = learningAlgorithms.get(name).getConstructors();
			for (Constructor<?> constructor : constructors) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				if (parameterTypes.length == parameters.size()) {
					int i = 0;
					while (i < parameterTypes.length && parameterTypes[i]
							.isAssignableFrom(parameters.get(i).getClass()))
						++i;
					if (i == parameterTypes.length) {
						instance = (LearningAlgorithm) constructor.newInstance(parameters.toArray());
					} else {
						throw new InvalidParameterException(
								i + " th parameter of the constructor of " + name + " should be a " + parameterTypes[i]
										+ " and is a " + parameters.get(i).getClass());
					}
				}
			}
		} catch (InvalidParameterException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (instance == null)
			throw new InvalidParameterException();
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

	/**
	 * Finds all learning algorithms.
	 *
	 * @return a list of learning algorithms.
	 */
	private List<Class<?>> findAllLearningAlgorithms() {
		try {
			FilterIF filter = org.openmarkov.plugin.Filter.filter().toBeAnnotatedBy(LearningAlgorithmType.class);
			return pluginsLoader.loadAllPlugins(filter);
		} catch (Exception e) {
		}
		return null;
	}

}

