package org.openmarkov.learning.core.preprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.ProbNodeNotFoundException;
import org.openmarkov.core.io.database.CaseDatabase;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.learning.core.preprocess.exception.WrongDiscretizationLimitException;

public class DiscretizationTests
{
    CaseDatabase database = null;
    Variable varA, varB;
    
    @Before
    public void setUp ()
    {
        List<Variable> variables = new ArrayList<>();
        varA = new Variable ("A", "3", "?", "2", "4", "7", "5");
        varB = new Variable ("B", "2.6", "1.0", "-3.2", "0.01", "0.6", "1.2");
        variables.add (varA);
        variables.add (varB);
        
        int[][] cases = {{ 0, 0}, { 0, 1}, { 1, 1}, { 2, 3}, { 3, 0}, { 3, 4}, {4, 1}, {5, 5}}; 
        
        database = new CaseDatabase (variables, cases);
    }
    
    @Test
    public void testNoDiscretize ()
        throws InvalidStateException,
        ProbNodeNotFoundException,
        WrongDiscretizationLimitException
    {
        Map<String, Discretization.Option> discretizeOptions = new HashMap<> ();
        discretizeOptions.put ("A", Discretization.Option.NONE);
        discretizeOptions.put ("B", Discretization.Option.NONE);
        
        Map<String, Integer> numIntervalsPerVariable = new HashMap<> ();
        numIntervalsPerVariable.put ("A", 5000);
        numIntervalsPerVariable.put ("B", 1);
        
        CaseDatabase newDatabase = Discretization.process (database, discretizeOptions, numIntervalsPerVariable);
        
        Assert.assertEquals (database.getVariables (), newDatabase.getVariables ());
        Assert.assertEquals (database.getCases ().length, newDatabase.getCases ().length);
    }

    @Test
    public void testDiscretizeEqualWidth ()
        throws InvalidStateException,
        ProbNodeNotFoundException,
        WrongDiscretizationLimitException
    {
        Map<String, Discretization.Option> discretizeOptions = new HashMap<> ();
        discretizeOptions.put ("A", Discretization.Option.EQUAL_WIDTH);
        discretizeOptions.put ("B", Discretization.Option.EQUAL_WIDTH);
        
        Map<String, Integer> numIntervalsPerVariable = new HashMap<> ();
        numIntervalsPerVariable.put ("A", 3);
        numIntervalsPerVariable.put ("B", 4);
        
        
        CaseDatabase newDatabase = Discretization.process (database, discretizeOptions, numIntervalsPerVariable);
        
        Variable newVarA = newDatabase.getVariable ("A");
        Variable newVarB = newDatabase.getVariable ("B");
        Assert.assertEquals (4, newVarA.getStates ().length);
        Assert.assertEquals (4, newVarB.getStates ().length);
        Assert.assertEquals ("[2.0 , 3.666666666666667)", newVarA.getStates ()[0].getName ());
        Assert.assertEquals ("[-3.2 , -1.75)", newVarB.getStates ()[0].getName ());
        Assert.assertEquals ("?", newVarA.getStates ()[3].getName ());
        Assert.assertEquals (0, newDatabase.getCases ()[0][0]);
        Assert.assertEquals (3, newDatabase.getCases ()[0][1]);
        Assert.assertEquals (1, newDatabase.getCases ()[7][0]);
        Assert.assertEquals (3, newDatabase.getCases ()[7][1]);
        
    }
    
    @Test
    public void testDiscretizeEqualFreq ()
        throws InvalidStateException,
        ProbNodeNotFoundException,
        WrongDiscretizationLimitException
    {
        Map<String, Discretization.Option> discretizeOptions = new HashMap<> ();
        discretizeOptions.put ("A", Discretization.Option.EQUAL_FREQ);
        discretizeOptions.put ("B", Discretization.Option.EQUAL_FREQ);
        
        Map<String, Integer> numIntervalsPerVariable = new HashMap<> ();
        numIntervalsPerVariable.put ("A", 4);
        numIntervalsPerVariable.put ("B", 2);
        
        
        CaseDatabase newDatabase = Discretization.process (database, discretizeOptions, numIntervalsPerVariable);
        
        Variable newVarA = newDatabase.getVariable ("A");
        Variable newVarB = newDatabase.getVariable ("B");
        Assert.assertEquals (5, newVarA.getStates ().length);
        Assert.assertEquals (2, newVarB.getStates ().length);
        Assert.assertEquals ("(-Infinity , 3.0]", newVarA.getStates ()[0].getName ());
        Assert.assertEquals ("(7.0 , Infinity)", newVarA.getStates ()[3].getName ());
        Assert.assertEquals ("(-Infinity , 1.0]", newVarB.getStates ()[0].getName ());
        Assert.assertEquals ("?", newVarA.getStates ()[4].getName ());
        int[][] newCases = newDatabase.getCases ();
        Assert.assertEquals (0, newCases[0][0]);
        Assert.assertEquals (1, newCases[0][1]);
        Assert.assertEquals (2, newCases[7][0]);
        Assert.assertEquals (1, newCases[7][1]);        
    }
    
    @Test
    public void testDiscretizeModelNet ()
        throws InvalidStateException,
        ProbNodeNotFoundException,
        WrongDiscretizationLimitException
    {
        
        State[] statesA = {new State("(-Infinity, -2]"), new State("(-2, 2]"),  new State("(2, +Infinity]"), new State("?")};
        State[] statesB = {new State("(-Infinity, 0]"), new State("(0, +Infinity]"), new State("?")};
        
        PartitionedInterval partitionedIntervalA = new PartitionedInterval (new double[]{Double.NEGATIVE_INFINITY, -2.0, 2.0, Double.POSITIVE_INFINITY}, new boolean[]{true, true, true, true});
        PartitionedInterval partitionedIntervalB = new PartitionedInterval (new double[]{Double.NEGATIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, new boolean[]{true, true, true});

        Variable modelVarA = new Variable ("A", statesA, partitionedIntervalA, 0.001);
        Variable modelVarB = new Variable ("B", statesB, partitionedIntervalB, 0.001);
        
        ProbNet modelNet = new ProbNet();
        modelNet.addProbNode (modelVarA, NodeType.CHANCE);
        modelNet.addProbNode (modelVarB, NodeType.CHANCE);
        
        CaseDatabase newDatabase = Discretization.process (database, modelNet);
        
        int[][] newCases = newDatabase.getCases ();
        Assert.assertEquals (2, newCases[0][0]);
        Assert.assertEquals (1, newCases[0][1]);
        Assert.assertEquals (3, newCases[2][0]);
        Assert.assertEquals (2, newCases[7][0]);
        Assert.assertEquals (1, newCases[7][1]);                
    }
    
    @Test
    public void testDiscretizeModelNetFS ()
        throws InvalidStateException,
        ProbNodeNotFoundException,
        WrongDiscretizationLimitException
    {
        
        List<Variable> variables = new ArrayList<>();
        varA = new Variable ("A", "st.quo", "higher", "lower");
        varB = new Variable ("B", "-", "+", "0");
        variables.add (varA);
        variables.add (varB);
        
        int[][] cases = {{ 0, 0}, { 0, 1}, { 1, 1}, { 2, 1}, { 0, 2}, { 2, 0}, {1, 0}, {2, 2}}; 
        
        database = new CaseDatabase (variables, cases);        
        
        Variable modelVarA = new Variable ("A", "lower", "st.quo", "higher");
        Variable modelVarB = new Variable ("B", "-", "0", "+");
        
        ProbNet modelNet = new ProbNet();
        modelNet.addProbNode (modelVarA, NodeType.CHANCE);
        modelNet.addProbNode (modelVarB, NodeType.CHANCE);
        
        CaseDatabase newDatabase = Discretization.process (database, modelNet);
        
        int[][] newCases = newDatabase.getCases ();
        Assert.assertEquals (1, newCases[0][0]);
        Assert.assertEquals (0, newCases[0][1]);
        Assert.assertEquals (2, newCases[2][0]);
        Assert.assertEquals (0, newCases[7][0]);
        Assert.assertEquals (1, newCases[7][1]);                
    }    
    
    
}
