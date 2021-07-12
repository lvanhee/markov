package markov.impl;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import finitestatemachine.Action;
import finitestatemachine.State;
import markov.MDP;
import markov.StateProbabilityDistribution;

/**
 * 
 * @author loisv
 *
 * @param <S>
 * @param <A>
 */
public class FunctionBasedMDPImpl<S extends State, A extends Action> implements MDP<S,A> {
	
	private final BiFunction<S, A, StateProbabilityDistribution<S>> transitionFunction;
	private final BiFunction<S, A, Double> rewardFunction;
	private final Function<S, Set<A>> possibleActionsPerStateOf;
	private final Set<S> states;
	
	private FunctionBasedMDPImpl(
			Set<S> states,
			BiFunction<S, A, StateProbabilityDistribution<S>> transitionFunction,
			BiFunction<S, A, Double> rewardFunction,
			Function<S, Set<A>> actionsPerState)
	{
		this.states = states;
		this.transitionFunction = transitionFunction;
		this.rewardFunction = rewardFunction;
		this.possibleActionsPerStateOf = actionsPerState;
	}

	public static <S extends State, A extends Action> FunctionBasedMDPImpl<S, A> newInstance(
			Set<S>states,
			BiFunction<S, A, StateProbabilityDistribution<S>> transition,
			BiFunction<S, A, Double> reward,
			Function<S, Set<A>> actionsPerState) {
		return new FunctionBasedMDPImpl<S, A>(states,transition,reward,actionsPerState);
	}
	
	@Override
	public Set<A> getPossibleActionsIn(S s) {return possibleActionsPerStateOf.apply(s);}

	@Override
	public double getRewardFor(S currentState, A a) {return rewardFunction.apply(currentState, a);}

	@Override
	public StateProbabilityDistribution<S> getConsequencesOf(S currentState, A a) 
	{return transitionFunction.apply(currentState, a);}

	@Override
	public Set<S> getAllStates() {return states;}
	
	public int hashCode() {
		return 
			transitionFunction.hashCode()+
			rewardFunction.hashCode()+
			possibleActionsPerStateOf.hashCode()+
			states.hashCode();
	}
	
	public boolean equals(Object o){return MDPs.isEquivalent(this, (MDP)o);}

}
