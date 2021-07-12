package markov.impl;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import finitestatemachine.Action;
import finitestatemachine.State;
import markov.MDP;
import markov.StateProbabilityDistribution;

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

	public static<S extends State, A extends Action> String toGraphviz(MDP<S, A> mdp) {
		String res = "";
		Map<S, Integer> uniqueGraphvizIdentifier = new HashMap<>();
		final DecimalFormat df = new DecimalFormat("#.##");
		
		int index = 0;
		for(S s:mdp.getAllStates())
		{
			uniqueGraphvizIdentifier.put(s, index);
			index++;
		}
		
		Map<A, Integer> uniqueIdentifiedPerAction = new HashMap<>();
		for(A a:mdp.getAllStates().stream().map(x->mdp.getPossibleActionsIn(x)).reduce(new HashSet<>(), (x,y)->{x.addAll(y); return x;}))
		{
			uniqueIdentifiedPerAction.put(a, index);
			index++;
		}
			
		res = mdp.getAllStates().stream().map(x->uniqueGraphvizIdentifier.get(x)+" [label=\""+x.toString()+"\"]").reduce((x,y)->x+"\n"+y).get()+"\n\n";
		
		for(S s: mdp.getAllStates())
			for(A a:mdp.getPossibleActionsIn(s))
			{
				String actionNode = "\""+uniqueGraphvizIdentifier.get(s)+"_"+uniqueIdentifiedPerAction.get(a)+"\"";
				res+= actionNode+ " [label = \""+a.toString()+"\", shape=box, fontsize=10]\n";
				res+=uniqueGraphvizIdentifier.get(s)+"->"+actionNode+"\n";
				
				StateProbabilityDistribution<S> outcome = mdp.getConsequencesOf(s, a);
				for(S next:outcome.getItems())
				{
					res+=actionNode+"->"+uniqueGraphvizIdentifier.get(next)+" [label="+df.format(outcome.getProbabilityOf(next))+", style=dashed, color=gray50, fontsize=10]\n";
				}
			}
		
				
		res = "digraph G {\n"+ res+"\n}";
		
		return res;
	}

}
