package com.mattjtodd.functional.stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A two value tuple.
 * @param <T1> the type of the one value
 * @param <T2> the type of the two value
 */
class Tuple<T1, T2> {
  /**
   * The one.
   */
  private final T1 one;

  /**
   * The two.
   */
  private final T2 two;

  /**
   * Creates a fully populated tuple.
   *
   * @param one the one value
   * @param two the two value
   */
  private Tuple(T1 one, T2 two) {
    this.one = checkNotNull(one);
    this.two = checkNotNull(two);
  }

  /**
   * Creates a tuple of the supplied values.
   *
   * @param one the one value
   * @param two the two value
   * @return the new tuple
   */
  public static <T, E> Tuple<T, E> tupleOf(T one, E two) {
    return new Tuple<>(one, two);
  }

  /**
   * Get the one value.
   *
   * @return the one value
   */
  public T1 one() {
    return one;
  }

  /**
   * Get the two value.
   *
   * @return the two value
   */
  public T2 two() {
    return two;
  }
}
