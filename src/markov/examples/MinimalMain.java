package markov.examples;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import finitestatemachine.Action;
import finitestatemachine.State;
import finitestatemachine.impl.StringActionImpl;
import finitestatemachine.impl.StringStateImpl;
import markov.GeneralizedValueFunction;
import markov.MDP;
import markov.Policy;
import markov.StateProbabilityDistribution;
import markov.impl.FunctionBasedMDPImpl;
import markov.impl.MDPs;
import markov.impl.Policies;
import markov.impl.StateProbabilityDistributionHashImpl;
import markov.impl.ValueFunctions;

public class MinimalMain {
	
	public static void main(String args[])
	{
		
		//------------------------------------------------------------THIS BLOC DEFINES THE ELEMENTS OF THE MDP
		Set<State> states = new HashSet<>();
		State s1 = StringStateImpl.newInstance("s1");
		State s2 = StringStateImpl.newInstance("s2");
		State s3 = StringStateImpl.newInstance("s3");
		states.add(s1);
		states.add(s2);
		states.add(s3);
		
		Action likelyStay = StringActionImpl.newInstance("likelyStay");
		Action likelyNext = StringActionImpl.newInstance("likelyNext");
		Action stayForSure = StringActionImpl.newInstance("stayForSure");
		
		Set<Action> allActions = Arrays.asList(likelyStay, likelyNext, stayForSure).stream().collect(Collectors.toSet());
		
		
		//This code allows defining the transition function within the "transitionFunction" function defined below
		BiFunction<State, Action, StateProbabilityDistribution<State>> transition = MinimalMain::transitionFunction;
		
		
		//This is another way of defining BiFunctions, this time using lambda expressions
		//This form of definition is best used for simple functions 
		BiFunction<State, Action, Double> onState3RewardFunction =  (s,a)->{
			if(s.equals(s3))
				return 1d;
			else return -0.5;
		};
		
		Function<State, Set<Action>> actionsPerState = s-> allActions;
		
		MDP<State, Action> mdp = FunctionBasedMDPImpl.newInstance(states, transition, onState3RewardFunction, actionsPerState);
		
		
		//------------------------------------------------------------THIS BLOC SHOWS HOW TO RELATE MDPs TO POLICIES AND VALUES
		
		//Example of a very simple policy, which always prefers to stand still
		Policy<State, Action> myHandCraftedPolicy = s -> likelyStay;
		
		System.out.println("Handcrafted policy:");
		Policies.printPolicy(mdp, myHandCraftedPolicy);
		
		//the cache is not mandatory but allows re-using computations for a later time, when computing the value becomes expensive
		GeneralizedValueFunction<State,Double> valHandcrafted = ValueFunctions.getAverageValueOf(mdp, myHandCraftedPolicy, 10);
		ValueFunctions.printValuePerState(mdp, valHandcrafted);
		
		
		//------------------------------------------------------------AUTOMATIC GENERATION OF OPTIMAL POLICY
		Policy<State, Action> valueIterationPolicy = Policies.getOptimalPolicy(mdp,10);
		GeneralizedValueFunction<State, Double> valueVIPolicy = ValueFunctions.getAverageValueOf(mdp, valueIterationPolicy, 10);
		System.out.println("\nOptimal policy: ");
		Policies.printPolicy(mdp,valueIterationPolicy);
		ValueFunctions.printValuePerState(mdp, valueVIPolicy);
		
		System.out.println(MDPs.toGraphviz(mdp));
	}
	
	
	/**
	 * This function describes the transition function for this MDP.
	 * Note that this function is defined by hand so it is easy to follow for a first play with MDPs.
	 * However, in practice, transition functions are likely to be defined programmatically, depending on the problem you face
	 * (e.g. implementing "MoveWest" for any state in a map)
	 * @param s
	 * @param a
	 * @return
	 */
	public static  StateProbabilityDistribution<State> transitionFunction(State s, Action a)
	{
		//here is an example of a deterministic transition: the next state is reached with a probability 1
		if(a.equals(StringActionImpl.newInstance("stayForSure")))
			return StateProbabilityDistributionHashImpl.newInstance(s);

		
		if(s.equals(StringStateImpl.newInstance("s1")) && a.equals(StringActionImpl.newInstance("likelyStay"))) 
		{
			Map<State, Double> valuePerState = new HashMap<>();
			valuePerState.put(StringStateImpl.newInstance("s1"), 0.8);
			valuePerState.put(StringStateImpl.newInstance("s2"), 0.15);
			valuePerState.put(StringStateImpl.newInstance("s3"), 0.05);
			return StateProbabilityDistributionHashImpl.newInstance(valuePerState);
		}
		
		if(s.equals(StringStateImpl.newInstance("s2")) && a.equals(StringActionImpl.newInstance("likelyStay"))) 
		{
			Map<State, Double> valuePerState = new HashMap<>();
			valuePerState.put(StringStateImpl.newInstance("s1"), 0.05);
			valuePerState.put(StringStateImpl.newInstance("s2"), 0.8);
			valuePerState.put(StringStateImpl.newInstance("s3"), 0.15);
			return StateProbabilityDistributionHashImpl.newInstance(valuePerState);
		}
		
		if(s.equals(StringStateImpl.newInstance("s3")) && a.equals(StringActionImpl.newInstance("likelyStay"))) 
		{
			Map<State, Double> valuePerState = new HashMap<>();
			valuePerState.put(StringStateImpl.newInstance("s1"), 0.15);
			valuePerState.put(StringStateImpl.newInstance("s2"), 0.05);
			valuePerState.put(StringStateImpl.newInstance("s3"), 0.8);
			return StateProbabilityDistributionHashImpl.newInstance(valuePerState);
		}
		
		
		if(s.equals(StringStateImpl.newInstance("s1")) && a.equals(StringActionImpl.newInstance("likelyNext"))) 
		{
			Map<State, Double> valuePerState = new HashMap<>();
			valuePerState.put(StringStateImpl.newInstance("s1"), 0.3);
			valuePerState.put(StringStateImpl.newInstance("s2"), 0.6);
			valuePerState.put(StringStateImpl.newInstance("s3"), 0.1);
			return StateProbabilityDistributionHashImpl.newInstance(valuePerState);
		}
		
		if(s.equals(StringStateImpl.newInstance("s2")) && a.equals(StringActionImpl.newInstance("likelyNext"))) 
		{
			Map<State, Double> valuePerState = new HashMap<>();
			valuePerState.put(StringStateImpl.newInstance("s1"), 0.1);
			valuePerState.put(StringStateImpl.newInstance("s2"), 0.3);
			valuePerState.put(StringStateImpl.newInstance("s3"), 0.6);
			return StateProbabilityDistributionHashImpl.newInstance(valuePerState);
		}
		
		if(s.equals(StringStateImpl.newInstance("s3")) && a.equals(StringActionImpl.newInstance("likelyNext"))) 
		{
			Map<State, Double> valuePerState = new HashMap<>();
			valuePerState.put(StringStateImpl.newInstance("s1"), 0.6);
			valuePerState.put(StringStateImpl.newInstance("s2"), 0.1);
			valuePerState.put(StringStateImpl.newInstance("s3"), 0.3);
			return StateProbabilityDistributionHashImpl.newInstance(valuePerState);
		}
		
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
