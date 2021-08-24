package markov.impl;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import finitestatemachine.Action;
import finitestatemachine.State;
import markov.GeneralizedValueFunction;
import markov.MDP;
import markov.Policy;
import markov.StateProbabilityDistribution;
import markov.probas.DiscreteProbabilityDistribution;
import markov.probas.DiscreteProbabilityDistributionImpl;
import markov.probas.DiscreteProbabilityDistributionAccuracyParameters;

/**
 * 
 * @author loisv
 * 
 * 20201222
 *
 */
public class Policies {

	public static<S extends State, A extends Action> Policy<S, A> getRandomPolicyFor(MDP<S, A> mdp) {
		Map<S, A> res = new HashMap<>();

		Random r = new Random();

		for(S s:mdp.getAllStates())
		{
			List<A> actions = mdp.getPossibleActionsIn(s).stream().collect(Collectors.toList());
			res.put(s,actions.get(r.nextInt(actions.size())));
		}

		return x->res.get(x);
	}

	public static<S extends State, A extends Action> void exportToText(MDP<S,A> mdp, Policy<S, A>p, BiFunction<S, A, String> toString, String fileName) {
		String res = "";

		for(S s:mdp.getAllStates())
			res+=toString.apply(s, p.apply(s))+"\n";

		FileWriter myWriter;
		try {
			myWriter = new FileWriter(fileName);
			myWriter.write(res);
			myWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
	}

	public static <S extends State, A extends Action>  Policy<S, A> importPolicyOrOptimal(String string, MDP<S,A> mdp,
			Function<String, S> parserState, Function<String, A> parserAction, int horizon) {
		try {
			
			if(!Files.exists(Paths.get(string)))return getOptimalPolicy(mdp, horizon, false);

			Map<S, A> res = new HashMap<>();
			for(String s: Files.readAllLines(Paths.get(string)))
				res.put(parserState.apply(s.split(" ")[0]), parserAction.apply(s.split(" ")[1]));
			return x->res.get(x);
		}
		catch(Exception e) {throw new Error();}			
	}

	public static<S extends State, A extends Action, V extends Object> V evaluateAddedValueOfAction(
			MDP<S, A>mdp,
			S s, A a,
			Function<S, V>currentValuePerState,
			BiFunction<V, Double, V> valueAdder,
			Function<DiscreteProbabilityDistribution<V>,V> valueDistributionMerger, 
			DiscreteProbabilityDistributionAccuracyParameters params
			) {
			
			
		StateProbabilityDistribution<S> sd = mdp.getConsequencesOf(s, a);
		Map<V, Double> probabilityPerValue = new HashMap<>();
		for(S next:sd.getItems())
		{
			V currentVal = currentValuePerState.apply(next);

			if(!probabilityPerValue.containsKey(currentVal))
				probabilityPerValue.put(currentVal, 0d);

			probabilityPerValue.put(currentVal, probabilityPerValue.get(currentVal)+sd.getProbabilityOf(next));
		}
		V postMerge  =valueDistributionMerger.apply(DiscreteProbabilityDistributionImpl.newInstance(probabilityPerValue, params));
		
		Double reward = mdp.getRewardFor(s, a);
		V postReward = valueAdder.apply(postMerge, reward);
		return postReward;
	}

	public static<S extends State, A extends Action> Function<S, Set<A>> toActionRestricter(Policy<S, A> p) {
		return x-> Arrays.asList(p.apply(x)).stream().collect(Collectors.toSet());
	}

	public static<S extends State, A extends Action> Policy<S, A> getOptimalPolicy(MDP<S, A> mdp, int horizon, boolean deterministic) {
		
		//initialization
		GeneralizedValueFunction<S, Double>value = x->0d;
		Set<S> allStates = mdp.getAllStates();
		for(int i = 0 ; i < horizon; i++)
		{
			final GeneralizedValueFunction<S, Double>currentValue = value;
			
			Stream<S> stream = allStates.parallelStream();
			if(deterministic)allStates.stream();
			Map<S,Double> next = stream.collect(
					Collectors.toMap(Function.identity(), 
							s->getValueBestAction(mdp,s,currentValue)));
			value = x->next.get(x);
		}
		
		final GeneralizedValueFunction<S, Double>finalValue = value;
		return s->getBestAction(mdp,s,finalValue);
	}
	
	private static <S extends State, A extends Action> double getValueBestAction(MDP<S,A> mdp, S s, GeneralizedValueFunction<S, Double> currentValue)
	{
		return getValueForAction(mdp, s, getBestAction(mdp, s, currentValue), currentValue);			
	}
	
	private static <S extends State, A extends Action> A getBestAction(MDP<S,A> mdp, S s, GeneralizedValueFunction<S, Double> currentValue)
	{
		if(mdp.getPossibleActionsIn(s).isEmpty())throw new Error("No action defined from \""+s+"\". Define at least one action for this state");
		return mdp.getPossibleActionsIn(s).stream().max((a1,a2)-> Double.compare(getValueForAction(mdp,s,a1,currentValue),getValueForAction(mdp,s,a2,currentValue))).get();			
	}
	

	private static <S extends State, A extends Action> double getValueForAction(MDP<S,A> mdp, S s, A a, GeneralizedValueFunction<S, Double> currentValue) {
		StateProbabilityDistribution<S> sd = mdp.getConsequencesOf(s, a);
		return mdp.getRewardFor(s, a) + sd.getItems().stream().map(x->sd.getProbabilityOf(x)* currentValue.apply(x)).reduce((x,y)->x+y).get();
	}

	public static <S extends State, A extends Action> void printPolicy(MDP<S, A> mdp, Policy<S, A> policy) {
		mdp.getAllStates()
		.stream()
		.sorted((x,y)->x.toString().compareTo(y.toString()))
		.forEach(s-> System.out.println(s+" "+policy.apply(s)));		
	}

	public static <S extends State, A extends Action> Policy<S, A> improvePolicy(
			MDP<S, A>mdp, 
			Comparator<Policy<S,A>> comparator, 
			Policy<S, A> currentOptimal,
			Consumer<S> onCompletedState,
			BiConsumer<Policy<S, A>, S> onImprovement, Collection<S> consideredStates
			) {
	
		AtomicReference<Policy<S,A>> res = new AtomicReference<Policy<S,A>>(currentOptimal);
	
		consideredStates.stream().forEach(
				s-> {
					onCompletedState.accept(s);
					mdp.getPossibleActionsIn(s)
					.stream()
					.filter(x->!x.equals(res.get().apply(s)))
					.forEach(
							a->{
								final Policy<S, A> opt = res.get();
								Map<S, A> m = mdp.getAllStates().stream().collect(Collectors.toMap(Function.identity(), x-> opt.apply(x)));
								m.put(s, a);
								Policy<S, A> testedPolicy = (x->m.get(x)); 
								if(comparator.compare(res.get(), testedPolicy)<0)
								{
									onImprovement.accept(testedPolicy,s);
									res.set(testedPolicy);
								}
							});
				}
				);
		return res.get();
	}

	/**
	 * This function takes a MDP and generates a locally optimal policy by comparing the value of this policy versus
	 * the value of other policies when switching one state
	 * @param <S>
	 * @param <A>
	 * @param mdp
	 * @param comparator
	 * @return
	 */
	public static<S extends State, A extends Action> Policy<S, A> 
	getLocalOptimum(MDP<S, A>mdp, Comparator<Policy<S,A>> comparator)
	{
		Policy<S, A> currentOptimal = getRandomPolicyFor(mdp);
		
		BiConsumer<Policy<S, A>, S> onImprovement = (p,s) -> {};
		boolean updated = true;
		while(updated)
		{
			Policy<S, A> outcome = improvePolicy(
					mdp, 
					comparator, 
					currentOptimal,
					(s)->{},
					onImprovement,
					mdp.getAllStates());
			
			updated = comparator.compare(currentOptimal, outcome)<0;
			currentOptimal = outcome;
		}
		return currentOptimal;
	}

	public static<S extends State, A extends Action> List<PairImpl<S,A>> getMostProbableTrajectoryFollowing(
			MDP<S,A> mdp, 
			S startState,
			int horizon,
			Policy<S, A>p) {
		List<PairImpl<S, A>> res = new ArrayList<>(horizon);
		S currentState = startState;
		for(int i = 0 ; i < horizon ; i++)
		{
			A a= p.apply(currentState);
			res.add(PairImpl.newInstance(currentState,a));
			currentState =  DiscreteProbabilityDistributions.getMostProbableValue(mdp.getConsequencesOf(currentState, a));
		}
		return res;
	}

}
