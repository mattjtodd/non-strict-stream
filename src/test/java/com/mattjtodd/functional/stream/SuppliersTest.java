package com.mattjtodd.functional.stream;

import static com.mattjtodd.functional.stream.Suppliers.memoize;
import static com.mattjtodd.functional.stream.Suppliers.supplier;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Tests for the {@link Suppliers}
 */
public class SuppliersTest {

  @Test
  public void supplierWithDelegateAndConsumerCheckingGet() {
    Object value = new Object();
    Supplier<Object> delegate = () -> value;
    Consumer<Object> consumer = Object::hashCode;
    Supplier<Object> supplier = supplier(delegate, consumer);

    assertThat(supplier.get(), is(value));
  }

  @Test
  public void memoizeCheckingGetInvokesDelegate() {
    Object value = new Object();
    Supplier<Object> delegate = () -> value;

    assertThat(memoize(delegate).get(), is(value));
  }

  @Test
  public void memoizeCheckingDelegateOnlyInvokedOnceForMultipleCalls() {
    Supplier<Object> delegate = mockSupplier();
    when(delegate.get()).thenReturn(new Object());

    Supplier<Object> supplier = memoize(delegate);
    supplier.get();
    supplier.get();

    verify(delegate, only()).get();
  }

  @Test
  public void memoizeCheckingDelegateOnlyInvokedOnceForMultipleCallsWhenSuppliedValueNull() {
    Supplier<Object> delegate = mockSupplier();

    Supplier<Object> supplier = memoize(delegate);
    supplier.get();
    supplier.get();

    verify(delegate, only()).get();
  }

  @Test
  public void memoizeCheckingHandlesConcurrentThreadsCorrectly() throws Exception {
    final int workers = 50;

    Supplier<Object> supplier = memoize(Object::new);

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

  private static <T> Supplier<T> mockSupplier() {
    @SuppressWarnings("unchecked")
    Supplier<T> supplier = mock(Supplier.class);
    return supplier;
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

    return IntStream
        .range(0, count)
        .mapToObj(value -> worker(cyclicBarrier, service, supplier))
        .collect(Collectors.toList());
  }

  private <T> CompletableFuture<T> worker(CyclicBarrier cyclicBarrier, ExecutorService service,
                                      Supplier<? extends T> supplier) {
    return supplyAsync(() -> getAfterAwait(cyclicBarrier, supplier), service);
  }

  private <T> T getAfterAwait(CyclicBarrier cyclicBarrier, Supplier<T> supplier) {
    try {
      cyclicBarrier.await();
      return supplier.get();
    } catch (InterruptedException | BrokenBarrierException e) {
      throw new IllegalStateException((e));
    }
  }
}
