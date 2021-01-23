package markov.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import finitestatemachine.Action;
import finitestatemachine.State;
import markov.MDP;

/**
 * 
 * @author loisv
 *
 */
public class MDPs {

	public static<S extends State, A extends Action> boolean isEquivalent(MDP<S, A> m1, MDP<S,A> m2) {
		if(m1.getAllStates().equals(m2.getAllStates()))return false;
		if(! m1.getAllStates().stream().allMatch(x->m1.getPossibleActionsIn(x).equals(m2.getPossibleActionsIn(x))))
			return false;
		
		Set<PairImpl<S, A>> allStateActionPairs = getAllStateActionPairsOf(m1);
		
		return allStateActionPairs.stream().allMatch(x-> 
		m1.getConsequencesOf(x.getLeft(), x.getRight()).equals(m2.getConsequencesOf(x.getLeft(), x.getRight())));
	}

	private static<S extends State, A extends Action> Set<PairImpl<S, A>> getAllStateActionPairsOf(MDP<S, A> m) {
		return m.getAllStates()
				.stream()
				.map(x->
				m.getPossibleActionsIn(x).stream()
				.map(y->PairImpl.newInstance((S)x, (A)y)).collect(Collectors.toSet())
						)
				.reduce(new HashSet<>(),
						(x,y)->{x.addAll(y); return x;});
	}

}
