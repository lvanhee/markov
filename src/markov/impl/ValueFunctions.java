package markov.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import finitestatemachine.Action;
import finitestatemachine.State;
import markov.GeneralizedValueFunction;
import markov.MDP;
import markov.Policy;
import markov.StateProbabilityDistribution;
import markov.caches.Cache;
import markov.caches.HashMapBasedCache;
import markov.probas.DiscreteProbabilityDistribution;
import markov.probas.DiscreteProbabilityDistributionImpl;
import markov.probas.DiscreteProbabilityDistributionParameters;

/**
 * 
 * @author lvanhee
 * 20210109
 */
public class ValueFunctions {

	public static<S extends State, A extends Action> GeneralizedValueFunction<S, Double> 
	getAverageValueOf(
			MDP<S,A> mdp,
			Policy<S, A> policy, 
			int horizon,
			Cache<StateHorizonPair<S>, Double> cache
			)
	{
		return 
				s->
		{
			return ValueFunctions.getValue(mdp, s, horizon, policy,
					(s2,a,h)->mdp.getRewardFor(s2,a), cache);
		};
	}

	public static<S extends State, A extends Action> double 
	getValue(MDP<S, A> mdp,S currentState,int horizon,Policy<S, A> policy) {
		return ValueFunctions.getValue(mdp, 
				currentState,
				horizon, 
				policy,
				(s,a,h)->mdp.getRewardFor(s,a),
				HashMapBasedCache.newInstance());
	}

	public static<S extends State, A extends Action> double getValue(
			MDP<S, A> mdp,
			S currentState, 
			int horizon, 
			Policy<S, A> policy,
			TriFunction<S, A, Integer, Double> evaluator,
			Cache<StateHorizonPair<S>, Double> cache)
	{
		if(cache.has(StateHorizonTupleImpl.newInstance(currentState, horizon)))
		{
			Double res = cache.get(StateHorizonTupleImpl.newInstance(currentState, horizon)); 
			return res;
		}

		if(horizon==0)return 0;

		double immediateValue= evaluator.apply(currentState, policy.apply(currentState), horizon);
		double futureValue = 0;
		StateProbabilityDistribution<S> consequencesOfPlayingA = 
				mdp.getConsequencesOf(currentState,
						policy.apply(currentState));
		for(S next: consequencesOfPlayingA.getItems())
		{
			futureValue+=
					consequencesOfPlayingA.getProbabilityOf(next)
					*getValue(mdp,next,horizon-1,policy, evaluator, cache);			
		}

		double res = immediateValue + futureValue;
		cache.add(StateHorizonTupleImpl.newInstance(currentState, horizon), res);
		return res;
	}

	public static<S extends State, A extends Action> double getValueOf
	(
			MDP<S,A> mdp,
			S initialState, 
			int horizon,
			Policy<S, A> policy,
			TriFunction<S, A,Integer, Double> evaluator) 
	{return getValueOf(mdp, initialState, horizon, policy, evaluator, HashMapBasedCache.newInstance());}

	public static<S extends State, A extends Action> double getValueOf
	(
			MDP<S,A> mdp,
			S initialState, 
			int horizon,
			Policy<S, A> pol,
			TriFunction<S, A,Integer, Double> evaluator,
			Cache<StateHorizonPair<S>, Double> cache) {
		return getValue(mdp, initialState, horizon, pol, evaluator, (Cache)cache);
	}

	public static<S extends State, A extends Action> double getValue(
			MDP<S, A> mdp,
			S currentState, 
			int horizon, 
			Policy<S, A> policy,
			Cache<StateHorizonPair<S>, Double> cache) {
		return getValue(mdp,
				currentState,
				horizon, 
				policy,
				(s,a,h)->mdp.getRewardFor(s,a),
				cache);
	}

	public static <S extends State, A extends Action, V> 
	GeneralizedValueFunction<S,V> 
	getValueUsingValueIterationOnTable(
			MDP<S, A> mdp, 
			int horizon, 
			Comparator<V> comparatorBetweenValueType, 
			BiFunction<S, A, V> rewardToValueType,
			Function<S, V> initValue,
			Function<S, Set<A>>allowedActions,
			BiFunction<V, Double, V> valueAdder,
			Function<DiscreteProbabilityDistribution<V>, V> merger,
			DiscreteProbabilityDistributionParameters parameters,
			List<Object[]> cache
			)
	{
		if(!cache.isEmpty()) return x->(V)cache.get(0)[((TablableState)x).getIndex()];

		int max = mdp.getAllStates().stream().map(x->((TablableState)x).getIndex()).reduce(0, Integer::max); 
		Object[] best = new Object[max+1];

		final Object[] bestLocal = best;
		mdp.getAllStates().stream().forEach(x->
		{

			assert(bestLocal[((TablableState)x).getIndex()]==null);
			bestLocal[((TablableState)x).getIndex()] = initValue.apply(x);
		}); 



		for(int i = 0 ; i < horizon; i++)
		{
			final Object[] current = best;
			Object[] next = new Object[max+1];

			Function<S, V> currentValuePerState = x->{
				int index = ((TablableState)x).getIndex();
				assert(current[index]!=null); return (V)current[index];};

				mdp.getAllStates()
				.parallelStream()
				.forEach(
						s-> {
							V v = ValueFunctions.evaluate(s,mdp,currentValuePerState, valueAdder, allowedActions, merger,comparatorBetweenValueType,parameters);

							assert(v!= null);
							next[((TablableState)s).getIndex()]=v;
						});
				best = next;
		}

		final Object[] bestFinal = best;
		cache.add(bestFinal);
		return x->(V)bestFinal[((TablableState)x).getIndex()];
	}

	public static<S extends State, A extends Action, V extends Object> V evaluate(
			S s, 
			MDP<S, A>mdp, 
			Function<S, V>currentValuePerState,
			BiFunction<V, Double, V> valueAdder,
			Function<S, Set<A>> filtring,
			Function<DiscreteProbabilityDistribution<V>, V> merger,
			Comparator<V> comparator,
			DiscreteProbabilityDistributionParameters probabilityDistributionParameters
			)
	{
		V best = null;
		Set<A> allowedAction = filtring.apply(s);
		Set<A> possibleAction =  mdp.getPossibleActionsIn(s);

		for(A a : possibleAction.stream().filter(x->allowedAction.contains(x)).collect(Collectors.toSet()))
		{
			V val = Policies.evaluateAddedValueOfAction(mdp, s,a, currentValuePerState, valueAdder, merger, probabilityDistributionParameters);

			if(best==null|| comparator.compare(best, val)< 0) best = val;
		}
		assert(best!=null);
		return best;
	}

	public static<S extends State, A extends Action> GeneralizedValueFunction<S, Double> 
	getAverageValueOf(
			MDP<S,A> mdp,
			Policy<S, A> policy, 
			int horizon
			)
	{
		return getAverageValueOf(mdp, policy, horizon, HashMapBasedCache.newInstance());
	}

	public static <S extends State, A extends Action, V>  void	printValuePerState(MDP<S, A> mdp, GeneralizedValueFunction<S, V> val) {
		mdp.getAllStates()
		.stream()
		.sorted((x,y)->x.toString().compareTo(y.toString()))
		.forEach(x->System.out.println(x+" "+val.apply(x)));
	}



}
