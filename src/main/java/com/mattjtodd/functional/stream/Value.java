package com.mattjtodd.functional.stream;

import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.mattjtodd.functional.stream.Suppliers.memoize;

/**
 * A memoized head tail value container for a stream.
 */
class Value<T> {
  /**
   * The head thunk.
   */
  private final Supplier<? extends T> head;

  /**
   * The tail thunk.
   */
  private final Supplier<Stream<T>> tail;

  /**
   * Creates a new value.  The thunks will be memoized.  Values cannot be null.
   *
   * @param head the head thunk
   * @param tail the tail thunk
   */
  public Value(Supplier<? extends T> head, Supplier<Stream<T>> tail) {
    this.head = memoize(checkNotNull(head));
    this.tail = memoize(checkNotNull(tail));
  }

  /**
   * Evaluate the head and return the value.
   *
   * @return the evaluated thunk
   */
  public T evalHead() {
    return head.get();
  }

  /**
   * Evaluates the tail and return the stream.
   *
   * @return the evaluated tail thunk
   */
  public Stream<T> evalTail() {
    return tail.get();
  }

  public Supplier<? extends T> getHead() {
    return head;
  }

  public Supplier<Stream<T>> getTail() {
    return tail;
  }
}
