package com.mattjtodd.functional.stream;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.mattjtodd.functional.stream.Suppliers.cache;
import static com.mattjtodd.functional.stream.Suppliers.supplier;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Tests for the {@link Suppliers}
 */
public class SuppliersTest {
    @Test
    public void supplierCheckingValueReturned() {
        Object value = new Object();

        assertThat(supplier(value).get(), is(value));
    }

    @Test
    public void supplierWithConsumerCheckingAcceptInvokedWithGet() {
        Object value = new Object();

        assertThat(supplier(value).get(), is(value));
    }

    @Test
    public void supplierWithDelegateAndConsumerCheckingGet() {
        Object value = new Object();
        Supplier<Object> delegate = () -> value;

        Consumer<Object> consumer = Object::hashCode;

        Supplier<Object> supplier = supplier(delegate, consumer);

        assertThat(supplier.get(), is(value));
    }

    @Test
    public void cacheCheckingGetInvokesDelegate() {
        Object value = new Object();
        Supplier<Object> delegate = () -> value;

        assertThat(cache(delegate).get(), is(value));
    }

    @Test
    public void cacheCheckingDelegateOnlyInvokedOnceForMultipleCalls() {
        Supplier<Object> delegate = mock(Supplier.class);
        when(delegate.get()).thenReturn(new Object());

        Supplier<Object> supplier = cache(delegate);
        supplier.get();
        supplier.get();

        verify(delegate, only()).get();
    }

    @Test
    public void cacheCheckingDelegateOnlyInvokedOnceForMultipleCallsWhenSuppliedValueNull() {
        Supplier<Object> delegate = mock(Supplier.class);

        Supplier<Object> supplier = cache(delegate);
        supplier.get();
        supplier.get();

        verify(delegate, only()).get();
    }

    @Test
    public void cacheCheckingHandlesConcurrentThreadsCorrectly() throws Exception{
        final int workers = 50;

        Supplier<Object> supplier = cache(Object::new);

        // create and execute the workers
        Collection<CompletableFuture<?>> futures = workers(workers, supplier);

        // wait until all workers have completed
        allOf(futures.toArray(new CompletableFuture<?>[futures.size()]));

        // Get the now cached uuid for verification
        Object value = supplier.get();

        // check each ofList the
        boolean result = futures
            .stream()
            .map(this::getQuietly)
            .allMatch(value::equals);

        assertTrue(result);
    }

    private <T> T getQuietly(CompletableFuture<T> completedFuture) {
        try {
            return completedFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private Collection<CompletableFuture<?>> workers(int count, Supplier<?> supplier) {
        ExecutorService service = newFixedThreadPool(count);
        CyclicBarrier cyclicBarrier = new CyclicBarrier(count);
        Collection<CompletableFuture<?>> coll = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            coll.add(worker(cyclicBarrier, service, supplier));
        }
        return coll;
    }

    private CompletableFuture<?> worker(CyclicBarrier cyclicBarrier, ExecutorService service, Supplier<?> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                cyclicBarrier.await();
                return supplier.get();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new IllegalStateException((e));
            }
        }, service);
    }
}
