package com.mattjtodd.functional.stream;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.mattjtodd.functional.stream.Result.latest;
import static com.mattjtodd.functional.stream.Stream.stream;
import static com.mattjtodd.functional.stream.Stream.empty;
import static com.mattjtodd.functional.stream.Tuple.tupleOf;
import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Functions for creating and processing a {@link Stream}.
 */
final class Streams {

  private Streams() {
    throw new AssertionError();
  }

  /**
   * Creates a stream ofList the supplied values list.
   *
   * @param values the values which will be used to populate the stream
   * @return the stream for the supplied values
   */
  public static <T> Stream<T> streamOf(Collection<T> values) {
    return streamOf(values.iterator());
  }

  /**
   * Creates a stream ofList the supplied iterator
   *
   * @param iterator the values which will be used to populate the stream
   * @return the stream for the supplied values
   */
  public static <T> Stream<T> streamOf(Iterator<T> iterator) {
    return unfold(iterator, current -> current.hasNext() ? some(tupleOf(current.next(), current)) : none());
  }

  /**
   * Creates an infinite stream with a constant value.
   *
   * @param value the value to forever provide
   * @return the stream
   */
  public static <T> Stream<T> constant(T value) {
    return unfold(value, current -> some(tupleOf(current, current)));
  }

  /**
   * Create an infinite stream of Integers starting from the supplied base int.
   *
   * @param startInclusive the start value
   * @return the stream
   */
  public static Stream<Integer> from(int startInclusive) {
    return unfold(startInclusive, value -> some(tupleOf(value, value + 1)));
  }

  /**
   * Corecursive lazy stream generation function.
   *
   * @param state the initial state
   * @param func  the function to generate the next item in the stream
   * @return the stream generated from the function
   */
  public static <A, B> Stream<A> unfold(B state, Function<B, Optional<Tuple<A, B>>> func) {
    return func
        .apply(state)
        .map(tuple -> stream(tuple::one, () -> unfold(tuple.two(), func)))
        .orElse(empty());
  }

  /**
   * Terminal function which returns the sum of all elements in an integer stream.
   *
   * @param stream the stream to sum
   * @return the result of the sum
   */
  public static int sum(Stream<Integer> stream) {
    return stream.foldLeft(() -> 0, (one, two) -> latest(one + two.get()));
  }

  /**
   * Creates an optional which contains a value.  For to increased readability.
   *
   * @param value the value
   * @return the populated Optional
   */
  static <T> Optional<T> some(T value) {
    return Optional.of(value);
  }

  /**
   * Creates an empty optional.  For to increased readability.
   *
   * @return the empty optional
   */
  static <T> Optional<T> none() {
    return Optional.empty();
  }

  /**
   * Gets the tail of a list.
   *
   * @param list the list to get the tail of
   * @return the tail of the list or an empty list
   */
  static <T> List<T> tailOf(List<T> list) {
    return list.size() > 1 ? copyOf(list.subList(1, list.size())) : emptyList();
  }
}
