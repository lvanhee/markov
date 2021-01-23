package markov.impl;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import finitestatemachine.Action;
import finitestatemachine.State;
import markov.GeneralizedValueFunction;
import markov.MDP;
import markov.Policy;
import markov.StateProbabilityDistribution;
import markov.probas.DiscreteProbabilityDistribution;
import markov.probas.DiscreteProbabilityDistributionImpl;
import markov.probas.DiscreteProbabilityDistributionParameters;

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
			
			if(!Files.exists(Paths.get(string)))return getOptimalPolicy(mdp, horizon);

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
			DiscreteProbabilityDistributionParameters params
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

	public static<S extends State, A extends Action> Policy<S, A> getOptimalPolicy(MDP<S, A> mdp, int horizon) {
		
		//initialization
		GeneralizedValueFunction<S, Double>value = x->0d;
				
		for(int i = 0 ; i < horizon; i++)
		{
			final GeneralizedValueFunction<S, Double>currentValue = value;
			Map<S,Double> next = mdp.getAllStates().parallelStream().collect(
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

}
