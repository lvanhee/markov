package markov.impl;

import finitestatemachine.State;

public interface StateHorizonPair<S extends State> {

	S getState();

	int getHorizon();
}
