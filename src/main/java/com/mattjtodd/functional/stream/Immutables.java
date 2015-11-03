package com.mattjtodd.functional.stream;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Immutable type utils.
 */
final class Immutables {

  private Immutables() {
    throw new AssertionError();
  }

  /**
   * Creates an immutable copy of the supplied list with the supplied value added to the head.
   *
   * @param list  the list to copy
   * @param value the new value
   * @return the appened to immutable list
   */
  public static <T> List<T> appendToHead(List<T> list, T value) {
    return ImmutableList.<T>builder()
        .add(checkNotNull(value))
        .addAll(checkNotNull(list))
        .build();
  }

  /**
   * Creates an immutable copy of the supplied list with the supplied value added to the tail.
   *
   * @param list  the list to copy
   * @param value the new value
   * @return the appened to immutable list
   */
  public static <T> List<T> appendToTail(List<T> list, T value) {
    return ImmutableList.<T>builder()
        .addAll(checkNotNull(list))
        .add(checkNotNull(value))
        .build();
  }
}
