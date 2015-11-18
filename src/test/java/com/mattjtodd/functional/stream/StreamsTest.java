package com.mattjtodd.functional.stream;

import org.junit.Test;

import static com.mattjtodd.functional.stream.Streams.*;
import static com.mattjtodd.functional.stream.Tuple.tupleOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link Streams}.
 */
public class StreamsTest {

  @Test
  public void someCheckingIsValueContainer() throws Exception {
    Object value = new Object();

    assertThat(some(value), is(of(value)));
  }

  @Test
  public void noneCheckingIsEmptyOptional() throws Exception {
    assertThat(none(), is(empty()));
  }

  @Test
  public void tailOfEmptyListCheckingEmptyList() throws Exception {
    assertThat(tailOf(emptyList()), is(emptyList()));
  }

  @Test
  public void tailOfSingletonListCheckingEmptyList() throws Exception {
    assertThat(tailOf(singletonList(new Object())), is(emptyList()));
  }

  @Test
  public void tailOfListCheckingList() throws Exception {
    Object tailValue = new Object();

    assertThat(tailOf(asList(new Object(), tailValue)), is(singletonList(tailValue)));
  }

  @Test
  public void sumWhenEmptyStreamIsZero() {
    assertThat(sum(Stream.empty()), is(0));
  }

  @Test
  public void sumWhenOneValueStreamIsOne() {
    assertThat(sum(streamOf(singletonList(1))), is(1));
  }

  @Test
  public void sumWhenFiveValuesStreamIsFive() {
    assertThat(sum(streamOf(asList(1, 1, 1, 1, 1))), is(5));
  }

  @Test
  public void sumWhenLargeValueNoStackOverflow() {
    assertThat(sum(Streams.constant(1).take(100000)), is(100000));
  }

  @Test
  public void unfoldTest() {
    Streams
        .unfold(0, value -> value > 10 ? none() : some(tupleOf(value, value + 1)))
        .toList();
  }
}
