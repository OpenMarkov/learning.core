/*
 * Copyright (c) CISIAD, UNED, Spain,  2018. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.learning.core.util;

public class StringEditMotivation extends LearningEditMotivation
{
    private String motivation;
    
    public StringEditMotivation(String motivation)
    {
        this.motivation = motivation;
    }
    
    public int compareTo ( LearningEditMotivation edit ){
        return 0;
    }
    
    @Override
    public String toString()
    {
       return motivation;
    }

}
