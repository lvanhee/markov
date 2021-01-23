package markov.impl;

import finitestatemachine.State;

public class StateHorizonTupleImpl<S extends State> implements StateHorizonPair<S> {
	
	private final int horizon;
	private final S state;
	
	public StateHorizonTupleImpl(S s, int horizon2) {
		this.state = s;
		this.horizon = horizon2;
	}

	public int hashCode()
	{
		return state.hashCode()*(horizon+1);
	}
	
	public boolean equals(Object o) {return ((StateHorizonTupleImpl)o).horizon==horizon&&
			((StateHorizonTupleImpl)o).state.equals(state);
			
	}

	public static<S extends State> StateHorizonTupleImpl<S> newInstance(S s, int horizon) {
		return new StateHorizonTupleImpl<>(s,horizon);
	}

	@Override
	public S getState() {return state;}

	@Override
	public int getHorizon() {return horizon;}

}
