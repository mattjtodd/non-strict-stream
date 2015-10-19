package com.mattjtodd.functional.stream;

import java.util.stream.*;

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
    public static <T, E> Tuple<T, E> of(T one, E two) {
        return new Tuple<>(one, two);
    }

    public T getOne() {
        return one;
    }

    public E getTwo() {
        return two;
    }

    public static void main(String[] args) {
        java.util.stream.Stream
            .generate(() -> 1)
            .forEach(System.out::println);
    }
}
