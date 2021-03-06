package com.mattjtodd.functional.stream;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.mattjtodd.functional.stream.Immutables.appendToTail;
import static com.mattjtodd.functional.stream.Result.latest;
import static com.mattjtodd.functional.stream.Streams.none;
import static com.mattjtodd.functional.stream.Streams.some;
import static com.mattjtodd.functional.stream.Suppliers.memoize;

/**
 * An immutable non-strict stream.
 */
class Stream<T> {

  /**
   * A static empty stream instance.
   */
  private static final Stream<?> EMPTY = new Stream<>(none());

  /**
   * The current value for this stream.
   */
  private final Optional<Value<T>> value;

  /**
   * Constructs a new instance with the supplied value.
   *
   * @param value an optional Monad containing the supplier for the head supplier and the next
   *              stream value supplier.
   */
  private Stream(Optional<Value<T>> value) {
    this.value = checkNotNull(value);
  }

  /**
   * Constructs a new instance with a non-strict head and tail.
   *
   * @param head the current head expression
   * @param tail the next head expression
   */
  private Stream(Supplier<? extends T> head, Supplier<Stream<T>> tail) {
    this(some(new Value<>(head, tail)));
  }

  /**
   * Creates a new Stream instance with the supplied head and tail both of which are memoized.
   *
   * @param head the current head expression
   * @param tail the next head expression
   * @return the new stream
   */
  public static <T> Stream<T> stream(Supplier<? extends T> head, Supplier<Stream<T>> tail) {
    return new Stream<>(memoize(head), memoize(tail));
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
   * A fold-right reduce function.
   *
   * @param result the non-strict result function
   * @param func the reduction function
   * @return the reduced value
   */
  public <E> E foldRight(Supplier<E> result, BiFunction<? super T, Supplier<E>, E> func) {
    return value
        .map(value -> func.apply(value.evalHead(), () -> value.evalTail().foldRight(result, func)))
        .orElseGet(result);
  }

  /**
   * A fold-left reduce function using a trampoline to optimise it's tail-call recursion. It's also
   * possible to short-circuit the traversal of the stream early by returning a terminal {@link
   * Result} form the supplied fold function.
   *
   * @param result the non-strict result function
   * @param func the reduction function
   * @return the reduced value
   */
  public <E> E foldLeft(Supplier<E> result, BiFunction<? super T, Supplier<E>, Result<E>> func) {
    return doFoldLeft(result, func, this).invoke();
  }

  /**
   * Implements the foldLeft function using tail recursion and a trampoline to handle the stack.  It
   * also evaluates the lazy function arguments which would otherwise blow the stack.
   *
   * @param seed the current reduction seed
   * @param func the function to apply when reducing
   * @param stream the current stream value
   * @return the reduced value
   */
  private static <E, T> Trampoline<E> doFoldLeft(Supplier<E> seed,
                                                 BiFunction<? super T, Supplier<E>, Result<E>> func,
                                                 Stream<T> stream) {

    if (stream.isEmpty()) {
      return Trampoline.done(seed.get());
    }

    // remove the non-strictness from the trampoline calls by invoking the suppliers
    Value<T> value = stream.value.get();
    Result<E> result = func.apply(value.evalHead(), seed);

    // try to short circuit the left-fold
    if (result.isTerminal()) {
      return Trampoline.done(result.getValue());
    }

    // bouncy bouncy!
    return () -> doFoldLeft(result::getValue, func, value.evalTail());
  }

  /**
   * Repeatedly applies the evaluated head to the consumer and then repeats with the evaluated tail.
   * Virtual tail-call optimised forEach, capable of handling an infinite stream. Uses a while loop
   * rather than a trampoline as this will be more efficient.
   *
   * @param consumer the consumer to be applied to every item
   */
  public void forEach(Consumer<? super T> consumer) {
    Stream<T> current = this;
    while (current.value.isPresent()) {
      Value<T> value = current.value.get();
      consumer.accept(value.evalHead());
      current = value.evalTail();
    }
  }

  /**
   * Terminal operation which consumes this stream.
   *
   * @return the list ofList the stream values
   */
  public List<T> toList() {
    return foldLeft(Collections::emptyList, (head, tail) -> latest(appendToTail(tail.get(), head)));
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
        .map(value -> stream(value.getHead(), () -> value.evalTail().take(number - 1)))
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
    value.ifPresent(value -> consumer.accept(value.evalHead()));
    return value
        .map(tuple -> stream(tuple.getHead(), () -> tuple.evalTail().peek(consumer)))
        .orElse(empty());
  }

  /**
   * Return a stream which takes values whilst some condition provided by a function is true.
   *
   * @param condition the conditional function
   * @return the stream bound by the function
   */
  public Stream<T> takeWhile(Function<? super T, Boolean> condition) {
    return foldRightToStream((head, tail) -> condition.apply(head) ? stream(() -> head, tail) : empty());
  }
  /**
   * Terminal reduce function which checks for a condition being met, then terminates the traversal
   * early.
   *
   * @param condition the condition to satisfy once
   * @return true if the condition is met, false otherwise
   */
  public boolean exists(Function<? super T, Boolean> condition) {
    return foldLeftToBoolean(false, (head, tail) -> Result.of(condition.apply(head)));
  }

  /**
   * Terminal reduction function which checks that every item in the stream satisfies a given
   * function, or terminates the traversal early.
   *
   * @param condition the condition to satisfy for all elements
   * @return true if the condition was met, false otherwise
   */
  public boolean forAll(Function<? super T, Boolean> condition) {
    return foldLeftToBoolean(true, (head, tail) -> Result.of(condition.apply(head) && tail.get()));
  }

  /**
   * Applies the supplied transform function to each element in the stream, returning a new stream
   * to the transformed type.  This will not invoke recursion of the tail as two is never evaluated.
   *
   * @param func the transform function
   * @return the transformed stream
   */
  public <E> Stream<E> map(Function<? super T, ? extends E> func) {
    return foldRightToStream((head, tail) -> stream(() -> func.apply(head), tail));
  }

  /**
   * Filters element from the stream which don't match the supplied predicate.
   *
   * @param predicate the filter predicate
   * @return the filtered stream
   */
  public Stream<T> filter(Predicate<? super T> predicate) {
    return foldRightToStream((head, tail) -> predicate.test(head) ? stream(() -> head, tail) : tail.get());
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
    return foldRight(stream, (head, tail) -> stream(() -> head, tail));
  }

  /**
   * A flat-map function over the stream.
   *
   * @param func the function to use for flat mapping
   * @return a stream which has been flat-mapped
   */
  public <E> Stream<E> flatMap(Function<? super T, Stream<E>> func) {
    return foldRightToStream((head, tail) -> func.apply(head).append(tail));
  }

  /**
   * Partially applied fold right which always reduces to a stream.
   *
   * @param func the reduction function
   * @return the reduction stream
   */
  public Boolean foldLeftToBoolean(boolean start, BiFunction<? super T, Supplier<Boolean>, Result<Boolean>> func) {
    return foldLeft(() -> start, func);
  }

  /**
   * Partially applied fold right which always reduces to a stream  starting at an empty stream.
   *
   * @param func the reduction function
   * @return the reduction stream
   */
  public <E> Stream<E> foldRightToStream(BiFunction<? super T, Supplier<Stream<E>>, Stream<E>> func) {
    return foldRight(Stream::empty, func);
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
   * Get's the head of this stream.
   *
   * @return the streams head value which may or may not be present
   */
  public Optional<T> head() {
    return value.map(Value::evalHead);
  }

  /**
   * Gets the value optional.
   *
   * @return the value
   */
  Optional<Value<T>> getValue() {
    return value;
  }

  @Override
  public String toString() {
    return toList().toString();
  }
}
