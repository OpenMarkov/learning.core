/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.learning.core.util;

/**
 * Abstract base class representing the motivation (reason or score) behind a proposed
 * structural edit during network learning. Subclasses provide either a numeric score
 * ({@link ScoreEditMotivation}) or a textual description ({@link StringEditMotivation}).
 */
public abstract class LearningEditMotivation implements Comparable<LearningEditMotivation> {
}
