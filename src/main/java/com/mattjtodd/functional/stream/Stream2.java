package com.mattjtodd.functional.stream;

import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by mattjtodd on 18/11/2015.
 */
public class Stream2<T> {
  private final T head;

  private final Supplier<Stream2<T>> tail;

  static <T> Stream2<T> empty() {
    return new Stream2<>(null, null);
  }
  public Stream2(T head, Supplier<Stream2<T>> tail) {
    this.head = head;
    this.tail = tail;
  }

  public static <T> Stream2<T> cons(T head, Supplier<Stream2<T>> tail) {
    return new Stream2<>(head, tail);
  }

  public static <T> Stream2<T> value(T value) {
    return cons(value, () -> value(value));
  }

  public static Stream2<Integer> from(int start) {
    return cons(start, () -> from(start + 1));
  }

  private boolean isEmpty() {
    return head == null && tail == null;
  }

  public Stream2<T> take(int count) {
    if (isEmpty() || count <= 0) {
      return empty();
    }

    return new Stream2<>(head, () -> tail.get().take(count -1));
  }

  public <E> Stream2<E> map(Function<T, E> map) {
    if (isEmpty()) {
      return empty();
    }

    return new Stream2<>(map.apply(head), () -> tail.get().map(map));
  }

  public void forEach(Consumer<T> consumer) {
    if (!isEmpty()) {
      consumer.accept(head);
      tail.get().forEach(consumer);
    }
  }

  public void forEachT(Consumer<T> consumer) {
    doForEach(consumer, this);
  }

  public static <T> void doForEach(Consumer<T> consumer, Stream2<T> stream) {
    if (!stream.isEmpty()) {
      consumer.accept(stream.head);
      doForEach(consumer, stream.tail.get());
    }
  }

  public void forEachI(Consumer<T> consumer) {
    Stream2<T> current = this;
    while(!current.isEmpty()) {
      consumer.accept(current.head);
      current = current.tail.get();
    }
  }

  public static void main(String[] args) {
    from(0)
        .take(10000000)
        .map(value -> value * 10000)
        .map(Date::new)
        .forEachI(System.out::println);
  }
}
