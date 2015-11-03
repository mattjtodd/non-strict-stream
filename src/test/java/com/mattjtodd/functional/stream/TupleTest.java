package com.mattjtodd.functional.stream;

import static com.mattjtodd.functional.stream.Tuple.tupleOf;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for the {@code Tuple}.
 */
public class TupleTest {

  @Test(expected = NullPointerException.class)
  public void ofWithNullValues() {
    tupleOf(null, null);
  }

  @Test
  public void getOneCheckingValueCorrect() {
    Object one = new Object();
    Object two = new Object();
    Tuple<Object, Object> tuple = tupleOf(one, two);

    assertEquals(one, tuple.one());
  }

  @Test
  public void getTwoCheckingValueCorrect() {
    Object one = new Object();
    Object two = new Object();
    Tuple<Object, Object> tuple = tupleOf(one, two);

    assertEquals(two, tuple.two());
  }
}
