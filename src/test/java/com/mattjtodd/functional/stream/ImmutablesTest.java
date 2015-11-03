package com.mattjtodd.functional.stream;

import static com.mattjtodd.functional.stream.Immutables.appendToHead;
import static com.mattjtodd.functional.stream.Immutables.appendToTail;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.List;

/**
 * Tests for {@link Immutables}
 */
public class ImmutablesTest {

  @Test
  public void appendToHeadCheckingCorrectOrder() {
    Object value1 = new Object();
    List<Object> list = asList(value1);

    Object value2 = new Object();
    assertThat(appendToHead(list, value2), is(asList(value2, value1)));
  }

  @Test
  public void appendToTailCheckingCorrectOrder() {
    Object value1 = new Object();
    List<Object> list = asList(value1);

    Object value2 = new Object();
    assertThat(appendToTail(list, value2), is(asList(value1, value2)));
  }
}
