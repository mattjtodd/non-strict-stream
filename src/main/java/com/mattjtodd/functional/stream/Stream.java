package com.mattjtodd.functional.stream;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.mattjtodd.functional.stream.Suppliers.cache;
import static com.mattjtodd.functional.stream.Tuple.of;
import static java.util.Collections.emptyList;

/**
 * A non-strict stream.
 */
public class Stream<T> {
    /**
     * The current value for this stream.
     */
    private final Optional<Tuple<Supplier<? extends T>, Supplier<Stream<T>>>> value;

    private static final Stream<?> EMPTY = new Stream<>(Optional.empty());

    /**
     * Constructs a new instance with the supplied value.
     *
     * @param value an optional Monad containing the supplier for the head supplier and the next stream value supplier.
     */
    private Stream(Optional<Tuple<Supplier<? extends T>, Supplier<Stream<T>>>> value) {
        this.value = value;
    }

    /**
     * Constructs a new instance with a non-strict function definition for the value which is decorated with caching ofList
     * first access and next value.
     *
     * @param value the current value as a non-strict function
     * @param nextValue the next value as a non-strict function
     */
    private Stream(Supplier<? extends T> value, Supplier<Stream<T>> nextValue) {
        this(Optional.of(of(cache(value), nextValue)));
    }

    /**
     * Simple static constructor to avoid verbosity ofList new.
     *
     * @param value the nonstrict function which supplies the value for this stream
     * @param nextValue the non-strict next stream value
     * @return the new stream
     */
    private static <T> Stream<T> cons(Supplier<? extends T> value, Supplier<Stream<T>> nextValue) {
        return new Stream<>(value, nextValue);
    }

    /**
     * An empty stream constructor.
     *
     * @return the empty stream
     */
    public static <T> Stream<T> empty() {
        return (Stream<T>) EMPTY;
    }

    /**
     * Creates a stream ofList the supplied values list.
     *
     * @param values the values which will be used to populate the stream
     * @return the stream for the supplied values
     */
    public static <T> Stream<T> ofList(List<T> values) {

        if (values.isEmpty()) {
            return empty();
        }

        List<T> tail = values.size() > 1 ? copyOf(values.subList(1, values.size())) : emptyList();

        return cons(() -> values.get(0), () -> ofList(tail));
    }

    /**
     * Creates an infinite stream with a constant value.
     *
     * @param value the value to forever provide
     * @return the stream
     */
    public static <T> Stream<T> constant(T value) {
        return cons(() -> value, () -> constant(value));
    }

    /**
     * Create an infinite stream ofList Integers starting from the supplied base int.
     *
     * @param startInclusive the start value
     * @return the stream
     */
    public static Stream<Integer> from(int startInclusive) {
        return cons(() -> startInclusive, () -> from(startInclusive + 1));
    }

    public static <A, B> Stream<A> unfold(B state, Function<B, Optional<Tuple<A, B>>> func) {
        return func
            .apply(state)
            .map(tuple -> cons(tuple::getOne, () -> unfold(tuple.getTwo(), func)))
            .orElse(empty());
    }

    public void forEach(Consumer<T> consumer) {
        value.ifPresent(tuple -> {
            consumer.accept(tuple.getOne().get());
            tuple.getTwo().get().forEach(consumer);
        });
    }

    /**
     * Terminal operation which consumes this stream.
     *
     * @return the list ofList the stream values
     */
    public List<T> toList() {
        return value
            .map(Stream::toList)
            .orElse(emptyList());
    }

    /**
     * Recursively turns the stream into a list.
     *
     * @param value the value for this stream
     * @return the terminal list
     */
    private static <T> List<T> toList(Tuple<Supplier<? extends T>, Supplier<Stream<T>>> value) {
        return new ImmutableList.Builder<T>()
            .add(value.getOne().get())
            .addAll(value.getTwo().get().toList())
            .build();
    }

    /**
     * Returns a stream which consists ofList the supplied number ofList steps, or the number ofList remaining steps if less
     * than those requested is available.
     *
     * @param number the number ofList values to handle from the stream
     * @return a stream with the requested number ofList steps
     */
    public Stream<T> take(int number) {
        if (number == 0 || !value.isPresent()) {
            return empty();
        }

        return cons(value.get().getOne(), () -> value.get().getTwo().get().take(number - 1));
    }

    /**
     * The current value represented in this stream will be <b>evaluated</b> and passed to the consumer.
     *
     * @param consumer the consumer ofList the peeked value
     * @return the current stream
     */
    public Stream<T> peek(Consumer<? super T> consumer) {
        if (!value.isPresent()) return empty();
        consumer.accept(value.get().getOne().get());
        return cons(value.get().getOne(), () -> value.get().getTwo().get().peek(consumer));
    }

    /**
     * Return a stream which takes values whilst some condition provided by a function is true.
     *
     * @param condition the conditional function
     * @return the stream bound by the function
     */
    public Stream<T> takeWhile(Function<? super T, Boolean> condition) {
        return value
            .filter(tuple -> condition.apply(tuple.getOne().get()))
            .map(tuple -> cons(tuple.getOne(), () -> tuple.getTwo().get().takeWhile(condition)))
            .orElse(empty());
    }

    /**
     * A fold-right reduce function.
     *
     * @param result the non-strict result function
     * @param func the reduction function
     * @return the reduced value
     */
    public <E> E foldRight(Supplier<E> result, Function<Tuple<T, Supplier<E>>, E> func) {
        return value
            .map(tuple -> func.apply(of(tuple.getOne().get(), () -> tuple.getTwo().get().foldRight(result, func))))
            .orElseGet(result);
    }

    /**
     * Terminal reduce function which checks for a condition being met, then terminates the traversal early.
     * Implemented using foldRight.
     *
     * @param condition the condition to satisfy once
     * @return true if the condition is met, false otherwise
     */
    public boolean exists(Function<? super T, Boolean> condition) {
        return foldRight(
            () -> false,
            entry -> condition.apply(entry.getOne()) ? true : entry.getTwo().get());
    }

    /**
     * Terminal reduction function which checks that every item in the stream satisfies a given function,
     * or terminates the traversal early.
     *
     * @param condition the condition to satisfy for all elements
     * @return true if the condition was met, false otherwise
     */
    public boolean forAll(Function<? super T, Boolean> condition) {
        return foldRight(
            () -> true,
            entry -> condition.apply(entry.getOne()) && entry.getTwo().get());
    }

    /**
     * As per takeWhile but implemented using foldRight.
     *
     * @param condition the condition to satisfy whilst traversing
     * @return the stream with the traversal function applied to it
     */
    public Stream<T> takeWhile2(Function<? super T, Boolean> condition) {
        return foldRightToStream(entry -> condition.apply(entry.getOne())
                                          ? cons(() -> entry.getOne(), entry.getTwo())
                                          : empty());
    }

    /**
     * Applies the supplied transform function to each element in the stream, returning a new stream to the
     * transformed type.
     *
     * @param func the transform function
     * @return the transformed stream
     */
    public <E> Stream<E> map(Function<? super T, ? extends E> func) {
        return foldRightToStream(entry -> cons(() -> func.apply(entry.getOne()), entry.getTwo()));
    }

    /**
     * Filters element from the stream which don't match the supplied predicate.
     *
     * @param predicate the filter predicate
     * @return the filtered stream
     */
    public Stream<T> filter(Predicate<? super T> predicate) {
        return foldRightToStream(entry -> predicate.test(entry.getOne())
                                          ? cons(() -> entry.getOne(), entry.getTwo())
                                          : entry.getTwo().get());
    }

    /**
     * Partially applied ffold right which always reduces to a stream.
     *
     * @param func the reduction function
     * @return the reduction stream
     */
    public <E> Stream<E> foldRightToStream(Function<Tuple<T, Supplier<Stream<E>>>, Stream<E>> func) {
        return foldRight(() -> empty(), func);
    }

    /**
     * Appends one stream to the end ofList this one.
     *
     * @param stream the stream to append
     * @return a stream with the supplier stream appended onto the end
     */
    public Stream<T> append(Stream<T> stream) {
        return foldRightToStream(entry -> cons(
            () -> entry.getOne(),
            (entry.getTwo().get().isEmpty()) ? () -> stream : entry.getTwo()));
    }

    /**
     * A flat-map function over the stream.
     *
     * @param func the function to use for flat mapping
     * @return a stream which has been flat-mapped
     */
    public Stream<T> flatMap(Function<T, Stream<T>> func) {
        return foldRightToStream(entry -> func.apply(entry.getOne()).append(entry.getTwo().get()));
    }

    @Override
    public String toString() {
        return toList().toString();
    }

    /**
     * Checks if this instance is the empty one.
     *
     * @return tru if this is the empty stream, false otherwise
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * Gets the value optional.
     *
     * @return the value
     */
    Optional<Tuple<Supplier<? extends T>, Supplier<Stream<T>>>> getValue() {
        return value;
    }

    public static void main(String[] args) {
        Stream
            .unfold(1, value -> Optional.of(of(value, value + 1)))
            .forEach(System.out::println);
    }
}
