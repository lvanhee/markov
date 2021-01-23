package markov.impl;

import java.util.Map;
import java.util.Set;
import finitestatemachine.State;
import markov.StateProbabilityDistribution;
import markov.probas.DiscreteProbabilityDistribution;
import markov.probas.DiscreteProbabilityDistributionImpl;
import markov.probas.DiscreteProbabilityDistributionParameters;


/**
 * 20210116
 * @author loisv
 *
 * @param <S> state type
 */
public class StateProbabilityDistributionHashImpl<S extends State> implements StateProbabilityDistribution<S> {
	
	private final DiscreteProbabilityDistribution<S> probabilityDistribution;

	private StateProbabilityDistributionHashImpl(DiscreteProbabilityDistribution<S> probabilityDistribution) {
		this.probabilityDistribution = probabilityDistribution;
	}

	@Override
	public Set<S> getItems() {
		return probabilityDistribution.getItems();
	}

	@Override
	public Map<S, Double> getMap() {
		return probabilityDistribution.getMap();
	}

	@Override
	public double getProbabilityOf(S t) {
		return probabilityDistribution.getProbabilityOf(t);
	}
	
	public String toString()
	{
		return probabilityDistribution.toString();
	}
	
	public int hashCode() {return probabilityDistribution.hashCode();}
	public boolean equals(Object o) {return ((StateProbabilityDistributionHashImpl)o).probabilityDistribution.equals(probabilityDistribution);}

	
	public static<S extends State> StateProbabilityDistributionHashImpl<S> newInstance(Map<S, Double> m) {
		return new StateProbabilityDistributionHashImpl<S>(
				DiscreteProbabilityDistributionImpl.newInstance(m, 
						DiscreteProbabilityDistributionParameters.EXACT_MODEL));
	}

	public static<S extends State> StateProbabilityDistributionHashImpl<S> newInstance(S newInstance) {
		DiscreteProbabilityDistribution<S> pd = DiscreteProbabilityDistributionImpl.newInstance(
				newInstance, 
				DiscreteProbabilityDistributionParameters.EXACT_MODEL);
		return new StateProbabilityDistributionHashImpl<>(pd);
	}
	
	public static <S extends State> StateProbabilityDistributionHashImpl<S> mergeWeightedDistributions(
			Map<StateProbabilityDistribution<S>, Double> distribution, DiscreteProbabilityDistributionParameters params) {
		return new StateProbabilityDistributionHashImpl<>(DiscreteProbabilityDistributionImpl.newInstanceMerge((Map)distribution, params));
	}


}
