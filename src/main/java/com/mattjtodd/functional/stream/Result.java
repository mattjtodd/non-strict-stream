package com.mattjtodd.functional.stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A container used to identify short-circuiting in a foldLeft.
 */
class Result<T> {

  /**
   * The result value.
   */
  private final T value;

  /**
   * Flag indicating if this is a terminal function
   */
  private final boolean terminal;

  /**
   * Constructs a new result.
   *
   * @param value    the value
   * @param terminal terminal flag value
   */
  private Result(T value, boolean terminal) {
    this.value = checkNotNull(value);
    this.terminal = terminal;
  }

  /**
   * Create a value which represents the latest non-terminal result.
   *
   * @param value the value of the result
   * @return the result
   */
  public static <T> Result<T> latest(T value) {
    return new Result<>(value, false);
  }

  /**
   * Create a terminal result.
   *
   * @param value the value of the result
   * @return the result
   */
  public static <T> Result<T> terminal(T value) {
    return new Result<>(value, true);
  }

  public T getValue() {
    return value;
  }

  public boolean isTerminal() {
    return terminal;
  }
}