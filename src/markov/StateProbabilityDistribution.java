package markov;

import finitestatemachine.State;
import markov.probas.DiscreteProbabilityDistribution;

public interface StateProbabilityDistribution<S extends State> extends DiscreteProbabilityDistribution<S> {}
