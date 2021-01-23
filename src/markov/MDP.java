package markov;

import java.util.Set;

import finitestatemachine.Action;
import finitestatemachine.State;
import markov.impl.StateProbabilityDistributionHashImpl;

/**
 * 20210115
 * 
 * @author loisv
 *
 * @param <S> type of state
 * @param <A> type of action
 */
public interface MDP<S extends State, A extends Action> {
	Set<S> getAllStates();
	Set<A> getPossibleActionsIn(S s);
	double getRewardFor(S s, A a);
	StateProbabilityDistribution<S> getConsequencesOf(S s, A a);
}
