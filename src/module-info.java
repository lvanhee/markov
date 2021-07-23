module markov {
	requires finitestatemachine;
	requires cachingutils;
	exports markov;
	exports markov.probas;
	exports markov.impl;
}