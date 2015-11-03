package com.mattjtodd.functional.stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A two value tuple.
 */
class Tuple<T, E> {
  /**
   * The one.
   */
  private final T one;

  /**
   * The two.
   */
  private final E two;

  /**
   * Creates a fully populated tuple.
   *
   * @param one the one value
   * @param two the two value
   */
  private Tuple(T one, E two) {
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
  public T one() {
    return one;
  }

  /**
   * Get the two value.
   *
   * @return the two value
   */
  public E two() {
    return two;
  }
}
