package com.mattjtodd.functional.stream;

/**
 * A basic trampoline.
 */
@FunctionalInterface
interface TailCall<T> {
    TailCall<T> apply();

    default boolean isComplete() {
        return false;
    }

    default T result() {
        throw new UnsupportedOperationException();
    }

    default T invoke() {
        return java.util.stream.Stream.iterate(this, TailCall::apply)
            .filter(TailCall::isComplete)
            .findFirst()
            .get()
            .result();
    }

    static <T> TailCall<T> done(final T value) {
        return new TailCall<T>() {
            @Override
            public TailCall<T> apply() {
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
}
