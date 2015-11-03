package com.mattjtodd.functional.stream;

import static com.google.common.collect.Lists.newArrayList;
import static com.mattjtodd.functional.stream.Stream.cons;
import static com.mattjtodd.functional.stream.Stream.empty;
import static com.mattjtodd.functional.stream.Streams.constant;
import static com.mattjtodd.functional.stream.Streams.from;
import static com.mattjtodd.functional.stream.Streams.streamOf;
import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.List;
import java.util.function.Supplier;

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

    assertThat(streamOf(singletonList(value)).getValue().get().one().get(), is(value));
  }

  @Test
  public void applyCheckingEmptyStream() {
    assertThat(streamOf(singletonList(new Object())).getValue().get().two().get(), is(empty()));
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
    Stream<String> stream = oneTwoThree().foldLeftToStream(tuple -> {
      Stream<String> cons = cons(() -> tuple.one(), tuple.two());
      return cons.isEmpty() ? Result.terminal(empty()) : Result.latest(cons);
    });

    assertEquals(ImmutableList.of("Three", "Two", "One"), stream.toList());
  }

  @Test
  public void foldRightToStreamCopyCheckingFoldDirectionCorrect() {
    Stream<String> stream = oneTwoThree().foldRightToStream(tuple -> cons(() -> tuple.one(), tuple.two()));

    assertEquals(ImmutableList.of("One", "Two", "Three"), stream.toList());
  }
}
