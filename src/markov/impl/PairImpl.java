package markov.impl;

import java.io.Serializable;

/**
 * 20210116
 * @author loisv
 *
 * @param <L>
 * @param <R>
 */
public class PairImpl<L, R> {

	private final L l;
	private final R r;
	private PairImpl(L o1, R o2) {
		this.l = o1;
		this.r = o2;
	}
	
	public L getLeft() {return l;}

	public R getRight() {return r;}
	
	public boolean equals(Object o){return ((PairImpl)o).getLeft().equals(getLeft()) &&((PairImpl)o).getRight().equals(getRight());}
	
	public int hashCode() {return (l.hashCode()+1) * r.hashCode();}
	
	public String toString() {return "("+l+","+r+")";}

	public static<T1, T2> PairImpl<T1, T2> newInstance(T1 o1, T2 o2) {
		return new PairImpl<T1, T2>(o1,o2);
	}
}
