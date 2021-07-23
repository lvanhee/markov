package markov.probas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import markov.impl.PairImpl;


/**
 * 20210116
 * @author loisv
 *
 *Deserves a bit of cleaning up;
 * @param <T>
 */
public class DiscreteProbabilityDistributionImpl<T> implements DiscreteProbabilityDistribution<T> {

	private Map<T, Double> m;
	private final int hashCodeCache;
	
	private DiscreteProbabilityDistributionImpl(Map<T, Double> m2, double mergeFactor, int maxNumberofItems) {
		assert(!m2.isEmpty());
		assert(m2.values().stream().allMatch(x->x>=0d));
		assert(m2.values().stream().reduce(Double::sum).get()>0.99);
		assert(m2.values().stream().reduce(Double::sum).get()<1.01);
		assert(!m2.containsKey(null));
		
		this.m = m2.keySet()
				.stream()
				.collect(Collectors.toMap(Function.identity(), x->m2.get(x)));
				
		
		initmergeForDouble(maxNumberofItems, mergeFactor);
		
		
		this.m = m.keySet()
				.stream()
				.filter(x->m.get(x)>mergeFactor)
				.collect(Collectors.toMap(Function.identity(), x->m.get(x)));
		
		
		double removed = 1d - m.values().stream().reduce(0d, (x,y)->x+y);
		while(m.size()>maxNumberofItems)
		{
			T min = m.keySet().stream().min((x,y)->
			Double.compare(m.get(x), m.get(y))).get();
			removed+=m.get(min);
			m.remove(min);		
			throw new Error("can be optimized for double!");
		}
		
		for(T t: m.keySet())
			m.put(t, m.get(t)/(1-removed));
		
		hashCodeCache = m.hashCode();
	}

	private void initmergeForDouble(int maxNumberofItems, double mergeFactor) {
		if(m.keySet().iterator().next() instanceof Double)
		{
			TreeSet<PairImpl<Double, Double>> tree = new TreeSet<>((x,y)->
			{
				int res = Double.compare(x.getRight()-x.getLeft(), y.getRight()-y.getLeft());
				if(res==0)return Double.compare(x.getLeft(), y.getLeft());
				assert(res!=0);
				return res;
				
			});
			Map<Double, java.util.List<PairImpl<Double,Double>>> pairsPerT = new HashMap<>();
			Double previousVal = null;
			
			
			List<Double> sortedKeys =m.keySet().stream().map(x->(Double)x).sorted().collect(Collectors.toList());
			for(Double currentVal: sortedKeys)
			{
				if(previousVal != null)
				{
					PairImpl<Double, Double> currentPair = PairImpl.newInstance(previousVal, currentVal);
					assert(!tree.contains(currentPair));
					boolean res = tree.add(currentPair);
					if(!pairsPerT.containsKey(currentVal))pairsPerT.put(currentVal, new LinkedList<>());
					if(!pairsPerT.containsKey(previousVal))pairsPerT.put(previousVal, new LinkedList<>());
					pairsPerT.get(currentVal).add(currentPair);
					pairsPerT.get(previousVal).add(currentPair);
				}
				previousVal = currentVal;
			}
			
			while(tree.size()>1 &&(m.size() > maxNumberofItems|| tree.first().getRight()-tree.first().getLeft()<mergeFactor))
			{
				PairImpl<Double, Double> minPair = tree.first();
				double diff =tree.first().getRight()- tree.first().getLeft();
			/*	if(m.size()<NUMBER_BEFORE_MERGE)
					System.out.println(diff+" "+tree.size());*/
				
				Double left = minPair.getLeft();
				Double right = minPair.getRight();
				Double probaLeft = m.get(left);
				Double probaRight = m.get(right);
				
				
				tree.remove(minPair);
				double relativeWeight = probaLeft/(probaLeft+probaRight);
				Double updatedValue = left*relativeWeight + right*(1 - relativeWeight);
				while(m.containsKey(updatedValue)) updatedValue= java.lang.Math.nextAfter(updatedValue, Double.POSITIVE_INFINITY);
				
				List<PairImpl<Double, Double>> newPairsList = new ArrayList<>(2);
				
				List<PairImpl<Double,Double>> pairsRelatedToLeft = pairsPerT.get(left);
				pairsPerT.remove(left);
				
				assert(pairsRelatedToLeft.remove(minPair));
				if(!pairsRelatedToLeft.isEmpty())
					assert(tree.removeAll(pairsRelatedToLeft));
				
				
				

				if(!pairsRelatedToLeft.isEmpty())
				{
					Double formerLeftOfLeft = pairsRelatedToLeft.get(0).getLeft();
					PairImpl<Double, Double> formerPairToReplace = PairImpl.newInstance(formerLeftOfLeft, left);
					PairImpl<Double, Double> replacement = PairImpl.newInstance(formerLeftOfLeft, updatedValue);
					assert(pairsRelatedToLeft.size()==1);
					newPairsList.add(replacement);
					
					pairsPerT.get(formerLeftOfLeft).set(pairsPerT.get(formerLeftOfLeft).indexOf(formerPairToReplace), replacement);
				}
				
				List<PairImpl<Double,Double>> pairsRelatedToRight = pairsPerT.get(right);
				pairsPerT.remove(right);
				
				assert(pairsRelatedToRight.remove(minPair));
				if(!pairsRelatedToRight.isEmpty())
					assert(tree.removeAll(pairsRelatedToRight));
				
				
				if(!pairsRelatedToRight.isEmpty())
				{
					assert(pairsRelatedToRight.size()==1);
					assert(pairsRelatedToRight.get(0).getLeft().equals(right));
					
					//Pair<Double, Double> formerPairForRightOfRightToReplace = Pair.newInstance(right, formerRightOfRight);
					Double formerRightOfRight = pairsRelatedToRight.get(0).getRight();
					PairImpl<Double, Double> replacement = PairImpl.newInstance(updatedValue, formerRightOfRight);

					newPairsList.add(replacement);

					//assert(newPairsList.remove(formerPairForRightOfRightToReplace));
					List<PairImpl<Double, Double>> updatedListRightOfRight = pairsPerT.get(formerRightOfRight);
					updatedListRightOfRight.set(0, replacement);
				}
								
				pairsPerT.put(updatedValue, newPairsList);
								
				sortedKeys.remove(left);
				sortedKeys.remove(right);
				m.remove(left);
				m.remove(right);

				assert(!m.containsKey(updatedValue));
				m.put((T)updatedValue, probaLeft+probaRight);
				sortedKeys.add(updatedValue);
				tree.addAll(newPairsList);
				
				assert(m.size()==tree.size()+1);
	
			}
		}
	}

	@Override
	public Set<T> getItems() {
		return m.keySet();
	}

	@Override
	public double getProbabilityOf(T t) {
		return m.get(t);
	}

	@Override
	public Map<T, Double> getMap() {
		return m;
	}
	
	public boolean equals(Object o)
	{
		return ((DiscreteProbabilityDistribution)o).getMap().equals(m);
	}
	
	
	public int hashCode()
	{
		return hashCodeCache;
	}

	public static<T> DiscreteProbabilityDistribution<T> newInstanceMerge(
			Map<DiscreteProbabilityDistribution<T>,
			Double> m2, DiscreteProbabilityDistributionAccuracyParameters params) {
		Map<T, Double> res = new HashMap<T, Double>();
		for(DiscreteProbabilityDistribution<T> m:m2.keySet())
			for(T t: m.getItems())
			{
				double likelihood = m2.get(m)*m.getProbabilityOf(t);
				
				if(!res.containsKey(t))	res.put(t, 0d);
				
				res.put(t,res.get(t)+likelihood);
			}
		
		return DiscreteProbabilityDistributionImpl.newInstance(res, params);
	}

	public static<T> DiscreteProbabilityDistributionImpl<T> newInstance(Map<T, Double> m, double mergeFactor, int maxNumberofItems) {
		return new DiscreteProbabilityDistributionImpl<T>(m, mergeFactor, maxNumberofItems);
	}

	public static<V> DiscreteProbabilityDistribution<V> newInstance(Map<V, Double> probabilityPerValue,
			DiscreteProbabilityDistributionAccuracyParameters params) {
		return newInstance(probabilityPerValue, params.getMergeFactor(), params.getNumberOfItems());
	}

	public static<V> DiscreteProbabilityDistribution<V> newInstance(V v,
			DiscreteProbabilityDistributionAccuracyParameters precision) {
		Map<V,Double> m = new HashMap<V, Double>();
		m.put(v,1d);
		return newInstance(m,precision);
	}
	
	public static<V> DiscreteProbabilityDistribution<V> newInstance(DiscreteProbabilityDistribution<V> v,
			DiscreteProbabilityDistributionAccuracyParameters precision) {
		return newInstance(v.getMap(),precision);
	}

}
