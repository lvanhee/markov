package markov;

import java.util.function.Function;

import finitestatemachine.Action;
import finitestatemachine.State;

public interface Policy<S extends State, A extends Action> extends Function<S, A>{}
