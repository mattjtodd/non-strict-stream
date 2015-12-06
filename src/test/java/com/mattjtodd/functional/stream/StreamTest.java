package com.mattjtodd.functional.stream;

import com.google.common.collect.ImmutableList;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static com.mattjtodd.functional.stream.Stream.*;
import static com.mattjtodd.functional.stream.Streams.*;
import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link Stream}.
 */
public class StreamTest {

  private static Stream<String> oneTwoThree() {
    return streamOf(ImmutableList.of("One", "Two", "Three"));
  }

  @Test
  public void applyCheckingOneValue() {
    Object value = new Object();

    assertThat(streamOf(singletonList(value)).getValue().get().evalHead(), is(value));
  }

  @Test
  public void applyCheckingEmptyStream() {
    assertThat(streamOf(singletonList(new Object())).getValue().get().evalTail(), is(empty()));
  }

  @Test
  public void toList() {
    Object value1 = new Object();
    Object value2 = new Object();
    Object value3 = new Object();
    Stream<Object> stream = streamOf(asList(value1, value2, value3));

    assertThat(stream.toList(), is(newArrayList(value1, value2, value3)));
  }

  @Test
  public void peekCheckingNonStrictStream() {
    Supplier<Object> one = mock(Supplier.class);
    Supplier<Object> two = mock(Supplier.class);

    streamOf(ImmutableList.of(one, two))
        .peek(Supplier::get)
        .peek(Supplier::get);

    verify(one, times(2)).get();
    verify(two, never()).get();
  }

  @Test
  public void mapCheckingNonStrictStreamWillNotInvoke() {
    Supplier<Object> one = mock(Supplier.class);
    Supplier<Object> two = mock(Supplier.class);

    streamOf(ImmutableList.of(one, two))
        .map(value -> value)
        .map(value -> value);

    verify(one, never()).get();
    verify(two, never()).get();
  }

  @Test
  public void mapCheckingNonStrictStreamWithTerminal() {
    Supplier<Object> one = () -> "One";
    Supplier<Object> two = () -> "Two";

    List<Object> objects = streamOf(ImmutableList.of(one, two))
        .map(Supplier::get)
        .toList();

    assertEquals(asList("One", "Two"), objects);
  }

  @Test
  public void filter() {
    List<String> objects = oneTwoThree()
        .filter("Two"::equals)
        .toList();

    assertEquals(singletonList("Two"), objects);
  }

  @Test
  public void forAllOneFalse() {
    boolean result = oneTwoThree()
        .forAll("Two"::equals);

    assertFalse(result);
  }

  @Test
  public void forAllAllTrue() {
    boolean result = oneTwoThree()
        .forAll(value -> true);

    assertTrue(result);
  }

  @Test
  public void take() {
    List<String> actual = oneTwoThree()
        .take(2)
        .toList();

    assertThat(actual, is(asList("One", "Two")));
  }

  @Test
  public void takeWhile() {
    List<String> actual = oneTwoThree()
        .takeWhile(value -> !"Three".equals(value))
        .toList();

    assertThat(actual, is(asList("One", "Two")));
  }

  @Test
  public void append() {
    List<String> actual = oneTwoThree()
        .append(oneTwoThree())
        .toList();

    assertThat(actual, is(asList("One", "Two", "Three", "One", "Two", "Three")));
  }

  @Test
  public void flatMap() {
    List<String> actual = oneTwoThree()
        .flatMap(value -> streamOf(asList(value, value)))
        .toList();

    assertThat(actual, is(asList("One", "One", "Two", "Two", "Three", "Three")));
  }

  @Test
  public void forEachCheckingValuesInOrder() {
    Consumer<Object> consumer = mock(Consumer.class);

    streamOf(asList(1, 2, 3, 4)).forEach(consumer);

    InOrder inOrder = inOrder(consumer);
    inOrder.verify(consumer).accept(1);
    inOrder.verify(consumer).accept(2);
    inOrder.verify(consumer).accept(3);
    inOrder.verify(consumer).accept(4);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void existsTrue() {
    assertTrue(oneTwoThree().exists("Two"::equals));
  }

  @Test
  public void existsFalse() {
    assertFalse(oneTwoThree().exists("Zero"::equals));
  }

  @Test
  public void isEmptyCheckingEmpty() {
    assertTrue(empty().isEmpty());
  }

  @Test
  public void isEmptyCheckingNotEmpty() {
    assertFalse(oneTwoThree().isEmpty());
  }

  @Test
  public void constantCheckingTakeFaveAllTheSame() {
    int copies = 100;
    Object value = new Object();

    assertThat(constant(value).take(copies).toList(), Is.is(nCopies(copies, value)));
  }

  @Test
  public void fromCheckingFiveToTen() {
    assertThat(from(5).take(5).toList(), Is.is(newArrayList(5, 6, 7, 8, 9)));
  }

  @Test
  public void foldLeftCheckingToListReversed() {
    Stream<String> stream = oneTwoThree().foldLeft(Stream::empty, (one, two) -> {
      Stream<String> cons = stream(() -> one, two);
      return cons.isEmpty() ? Result.terminal(empty()) : Result.latest(cons);
    });

    assertEquals(ImmutableList.of("Three", "Two", "One"), stream.toList());
  }

  @Test
  public void headWhenEmpty() {
    assertThat(oneTwoThree().filter(value -> value.length() > 5).head(), is(none()));
  }

  @Test
  public void headCheckingOne() {
    assertThat(oneTwoThree().head(), is(some("One")));
  }

  @Test
  public void foldRightToStreamCopyCheckingFoldDirectionCorrect() {
    Stream<String> stream = oneTwoThree().foldRightToStream((one, two) -> stream(() -> one, two));

    assertEquals(ImmutableList.of("One", "Two", "Three"), stream.toList());
  }

  @Test
  public void headCheckingEmptyWhenEmpty() {
    assertThat(empty().head(), is(Optional.empty()));
  }

  @Test
  public void headCheckingOneWhenConstOne() {
    assertThat(constant(1).head(), is(Optional.of(1)));
  }
}
