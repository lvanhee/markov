package markov.probas;

/**
 * 20210115 This model is describes the quality parameters for discrete probability distribution
 * Notably, 1) by restricting how many values are being considered by a probability distribution
 * And 2) by forbidding to store values with too low probability
 * 
 * This allows for controlled estimation of precision --as probability distributions can easily include a very large
 * number of items and easily cover items with very low probability
 * 
 * @author loisv
 *
 */
public class DiscreteProbabilityDistributionParameters {

	public static final DiscreteProbabilityDistributionParameters EXACT_MODEL = 
			new DiscreteProbabilityDistributionParameters(Integer.MAX_VALUE, 0d);
	final int nbOfItems;
	final double minimumPrecision;
	
	public DiscreteProbabilityDistributionParameters(int nbOfItems, double minimumPrecision)
	{
		this.nbOfItems = nbOfItems;
		this.minimumPrecision = minimumPrecision;
	}

	public int getNumberOfItems() {
		return nbOfItems;
	}

	public double getMergeFactor() {
		return minimumPrecision;
	}
}
