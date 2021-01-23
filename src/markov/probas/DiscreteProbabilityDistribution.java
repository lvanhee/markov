package markov.probas;

import java.util.Map;
import java.util.Set;

/**
 * This model represents probability distributions placed on a discrete continuum 
 * @author loisv
 * 
 * 20210115
 *
 * @param <V>
 */
public interface DiscreteProbabilityDistribution<V> {
	double getProbabilityOf(V t);
	Set<V> getItems();
	Map<V, Double> getMap();
}
