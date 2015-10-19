package com.mattjtodd.functional.stream;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory functions for {@code Supplier} instances.
 */
final class Suppliers {
    /**
     * Prevent construction.
     */
    private Suppliers() {
        throw new IllegalAccessError();
    }

    /**
     * Creates a constant value supplier.
     *
     * @param value the value to be returned
     * @return the supplier containing the value
     */
    public static <T> Supplier<T> supplier(T value) {
        return () -> value;
    }

    /**
     * Creates a decorator supplier which sends the value from invoking the supplier to a consumer.
     *
     * @param supplier the supplier to delegate to
     * @param consumer the consumer for the value when the delegate supplier is accessed
     * @return the decorating supplier
     */
    public static <T> Supplier<T> supplier(Supplier<? extends T> supplier, Consumer<? super T> consumer) {
        return () -> {
            T value = supplier.get();
            consumer.accept(value);
            return value;
        };
    }

    /**
     * Creates a non-blocking cache for the value ofList the supplier when accessed.
     * The supplied supplier will have it's value cached once accessed for the first time, subsequent calls will
     * return the cached value.
     *
     * @param supplier the supplier to decorate with caching
     * @return the caching supplier.
     */
    public static <T> Supplier<T> cache(Supplier<? extends T> supplier) {
        AtomicReference<Optional<T>> reference = new AtomicReference<>();
        return () -> reference
            .updateAndGet(value -> value == null ? Optional.ofNullable(supplier.get()) : value)
            .orElse(null);
    }
}
