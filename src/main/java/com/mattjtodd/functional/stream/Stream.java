package com.mattjtodd.functional.stream;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.mattjtodd.functional.stream.Immutables.appendToTail;
import static com.mattjtodd.functional.stream.Result.latest;
import static com.mattjtodd.functional.stream.Streams.none;
import static com.mattjtodd.functional.stream.Streams.some;
import static com.mattjtodd.functional.stream.Suppliers.memoize;
import static com.mattjtodd.functional.stream.Tuple.tupleOf;

/**
 * A non-strict stream.
 */
class Stream<T> {

  /**
   * A static empty stream instance.
   */
  private static final Stream<?> EMPTY = new Stream<>(none());
  /**
   * The current value for this stream.
   */
  private final Optional<Tuple<Supplier<? extends T>, Supplier<Stream<T>>>> value;

  /**
   * Constructs a new instance with the supplied value.
   *
   * @param value an optional Monad containing the supplier for the head supplier and the next
   *              stream value supplier.
   */
  private Stream(Optional<Tuple<Supplier<? extends T>, Supplier<Stream<T>>>> value) {
    this.value = checkNotNull(value);
  }

  /**
   * Constructs a new instance with a non-strict head and tail, which are both memoized.
   *
   * @param head     the current head expression
   * @param lazyTail the next head expression
   */
  private Stream(Supplier<? extends T> head, Supplier<Stream<T>> lazyTail) {
    this(some(tupleOf(memoize(head), memoize(lazyTail))));
  }

  /**
   * Simple static constructor to avoid verbosity of new.
   *
   * @param value     the current value expression
   * @param nextValue the next value expression
   * @return the new stream
   */
  public static <T> Stream<T> cons(Supplier<? extends T> value, Supplier<Stream<T>> nextValue) {
    return new Stream<>(value, nextValue);
  }

  /**
   * Provides an empty stream.
   *
   * @return the empty stream
   */
  public static <T> Stream<T> empty() {
    @SuppressWarnings("unchecked")
    Stream<T> empty = (Stream<T>) EMPTY;
    return empty;
  }

  /**
   * Implements the foldLeft function using tail recursion and a trampoline to handle the stack.  It
   * also evaluates the lazy function arguments which would otherwise blow the stack.
   *
   * @param result the current reduction result
   * @param func   the function to apply when reducing
   * @param stream the current stream value
   * @return the reduced value
   */
  private static <E, T> Trampoline<E> doFoldLeft(Supplier<E> result,
                                                 BiFunction<T, Supplier<E>, Result<E>> func,
                                                 Stream<T> stream) {

    if (stream.isEmpty()) {
      return Trampoline.done(result.get());
    }

    // remove the non-strictness from the trampoline calls by invoking the suppliers
    Tuple<Supplier<? extends T>, Supplier<Stream<T>>> tuple = stream.value.get();
    Result<E> apply = func.apply(tuple.one().get(), result);

    // try to short circuit the left-fold
    if (apply.isTerminal()) {
      return Trampoline.done(apply.getValue());
    }

    // bouncy bouncy!
    return () -> doFoldLeft(() -> apply.getValue(), func, tuple.two().get());
  }

  /**
   * Virtual tail-call optimised forEach, capable of handling an infinite stream. Uses a while loop
   * rather than a trampoline as this will be more efficient.
   *
   * @param consumer the consumer to be applied to every item
   */
  public void forEach(Consumer<? super T> consumer) {
    Stream<T> current = this;
    while (current.value.isPresent()) {
      Tuple<Supplier<? extends T>, Supplier<Stream<T>>> tuple = current.value.get();
      consumer.accept(tuple.one().get());
      current = tuple.two().get();
    }
  }

  /**
   * Terminal operation which consumes this stream.
   *
   * @return the list ofList the stream values
   */
  public List<T> toList() {
    return foldLeft(Collections::emptyList, (one, two) -> latest(appendToTail(two.get(), one)));
  }

  /**
   * Returns a stream which consists ofList the supplied number ofList steps, or the number ofList
   * remaining steps if less than those requested is available.
   *
   * @param number the number ofList values to handle from the stream
   * @return a stream with the requested number ofList steps
   */
  public Stream<T> take(int number) {
    return value
        .filter(value -> number > 0)
        .map(tuple -> cons(tuple.one(), () -> tuple.two().get().take(number - 1)))
        .orElse(empty());
  }

  /**
   * The current value represented in this stream will be <b>evaluated</b> and passed to the
   * consumer.
   *
   * @param consumer the consumer ofList the peeked value
   * @return the current stream
   */
  public Stream<T> peek(Consumer<? super T> consumer) {
    value.ifPresent(value -> consumer.accept(value.one().get()));
    return value
        .map(tuple -> cons(tuple.one(), () -> tuple.two().get().peek(consumer)))
        .orElse(empty());
  }

  /**
   * Return a stream which takes values whilst some condition provided by a function is true.
   *
   * @param condition the conditional function
   * @return the stream bound by the function
   */
  public Stream<T> takeWhile(Function<? super T, Boolean> condition) {
    return foldRightToStream((one, two) -> condition.apply(one) ? cons(() -> one, two) : empty());
  }

  /**
   * A fold-right reduce function.
   *
   * @param result the non-strict result function
   * @param func   the reduction function
   * @return the reduced value
   */
  public <E> E foldRight(Supplier<E> result, BiFunction<T, Supplier<E>, E> func) {
    return value
        .map(tuple -> func.apply(tuple.one().get(), () -> tuple.two().get().foldRight(result, func)))
        .orElseGet(result);
  }

  /**
   * A fold-left reduce function using trampolines to optimise it's tail-call recursion. It's also
   * possible to short-circuit the traversal of the stream early by returning a terminal {@link
   * Result} form the supplied fold function.
   *
   * @param result the non-strict result function
   * @param func   the reduction function
   * @return the reduced value
   */
  public <E> E foldLeft(Supplier<E> result, BiFunction<T, Supplier<E>, Result<E>> func) {
    return doFoldLeft(result, func, this).invoke();
  }

  /**
   * Terminal reduce function which checks for a condition being met, then terminates the traversal
   * early. Implemented using foldRight.
   *
   * @param condition the condition to satisfy once
   * @return true if the condition is met, false otherwise
   */
  public boolean exists(Function<? super T, Boolean> condition) {
    return foldLeftToBoolean(false, (one, two) -> Result.of(condition.apply(one)));
  }

  /**
   * Terminal reduction function which checks that every item in the stream satisfies a given
   * function, or terminates the traversal early.
   *
   * @param condition the condition to satisfy for all elements
   * @return true if the condition was met, false otherwise
   */
  public boolean forAll(Function<? super T, Boolean> condition) {
    return foldLeftToBoolean(true, (one, two) -> Result.of(condition.apply(one) && two.get()));
  }

  /**
   * Applies the supplied transform function to each element in the stream, returning a new stream
   * to the transformed type.
   *
   * @param func the transform function
   * @return the transformed stream
   */
  public <E> Stream<E> map(Function<? super T, ? extends E> func) {
    return foldRightToStream((one, two) -> cons(() -> func.apply(one), two));
  }

  /**
   * Filters element from the stream which don't match the supplied predicate.
   *
   * @param predicate the filter predicate
   * @return the filtered stream
   */
  public Stream<T> filter(Predicate<? super T> predicate) {
    return foldRightToStream((one, two) -> predicate.test(one) ? cons(() -> one, two) : two.get());
  }

  /**
   * Partially applied fold right which always reduces to a stream  starting at an empty stream.
   *
   * @param func the reduction function
   * @return the reduction stream
   */
  public <E> Stream<E> foldRightToStream(BiFunction<T, Supplier<Stream<E>>, Stream<E>> func) {
    return foldRight(Stream::empty, func);
  }

  /**
   * Partially applied fold right which always reduces to a stream starting at an empty stream.
   *
   * @param func the reduction function
   * @return the reduction stream
   */
  public <E> Stream<E> foldLeftToStream(BiFunction<T, Supplier<Stream<E>>, Result<Stream<E>>> func) {
    return foldLeft(Stream::empty, func);
  }

  /**
   * Partially applied fold right which always reduces to a stream.
   *
   * @param func the reduction function
   * @return the reduction stream
   */
  public Boolean foldLeftToBoolean(boolean start, BiFunction<T, Supplier<Boolean>, Result<Boolean>> func) {
    return foldLeft(() -> start, func);
  }

  /**
   * Lazily appends the supplied stream to the end of this one.
   *
   * @param stream the stream to append
   * @return a stream with the supplier stream appended onto the end
   */
  public Stream<T> append(Stream<T> stream) {
    return append(() -> stream);
  }

  /**
   * Lazily appends the supplied stream to the end of this one.
   *
   * @param stream the stream to append
   * @return a stream with the supplier stream appended onto the end
   */
  public Stream<T> append(Supplier<Stream<T>> stream) {
    return foldRight(stream, (one, two) -> cons(() -> one, two));
  }

  /**
   * A flat-map function over the stream.
   *
   * @param func the function to use for flat mapping
   * @return a stream which has been flat-mapped
   */
  public <E> Stream<E> flatMap(Function<T, Stream<E>> func) {
    return foldRight(Stream::empty, (one, two) -> func.apply(one).append(two));
  }

  /**
   * Checks if this instance is the empty one.
   *
   * @return true if this is the empty stream, false otherwise
   */
  public boolean isEmpty() {
    return !value.isPresent();
  }

  /**
   * Gets the value optional.
   *
   * @return the value
   */
  Optional<Tuple<Supplier<? extends T>, Supplier<Stream<T>>>> getValue() {
      return value;
  }

  @Override
  public String toString() {
    return toList().toString();
  }
}
