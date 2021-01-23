package markov;

import java.util.function.Function;

import finitestatemachine.State;

/**
 * 
 * @author loisv
 * 
 * 20201223
 *
 * @param <S> type of the states
 * @param <V> type of the value associated to the state
 */
public interface GeneralizedValueFunction<S extends State, V extends Object> extends Function<S, V>{}
