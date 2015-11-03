package com.mattjtodd.functional.stream;

import java.util.stream.Stream;

/**
 * A basic trampoline which is used to remove the burden on the stack of tail-recursive method
 * calls.
 */
@FunctionalInterface
interface Trampoline<T> {

  /**
   * Creates a done trampoline containing the value.
   *
   * @param value the value
   * @return the completed trampoline call
   */
  static <T> Trampoline<T> done(T value) {
    return new Trampoline<T>() {
      @Override
      public Trampoline<T> apply() {
        throw new UnsupportedOperationException();
      }

      @Override
      public T result() {
        return value;
      }

      @Override
      public boolean isComplete() {
        return true;
      }
    };
  }

  /**
   * Create a new trampoline expression.
   *
   * @return the new trampoline for the next step
   */
  Trampoline<T> apply();

  /**
   * Checks if this trampoline value is complete.
   *
   * @return true if the terminal case has been reached.
   */
  default boolean isComplete() {
    return false;
  }

  /**
   * Gets the result
   *
   * @return the result
   * @throws UnsupportedOperationException if not overridden
   */
  default T result() {
    throw new UnsupportedOperationException();
  }

  /**
   * Processes the trampoline to possible termination.  Note that it's possible to never terminate.
   *
   * @return the result
   */
  default T invoke() {
    return Stream
        .iterate(this, Trampoline::apply)
        .filter(Trampoline::isComplete)
        .findFirst()
        .get()
        .result();
  }
}
