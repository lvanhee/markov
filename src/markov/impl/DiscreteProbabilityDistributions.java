package markov.impl;

import markov.probas.DiscreteProbabilityDistribution;

/**
 * 20210116
 * @author loisv
 *
 */
public class DiscreteProbabilityDistributions {
	
	public static<V> V getMostProbableValue(DiscreteProbabilityDistribution<V> d)
	{
		return d.getItems().stream().max((x,y)->Double.compare(d.getProbabilityOf(x), d.getProbabilityOf(y))).get();
	}
}
