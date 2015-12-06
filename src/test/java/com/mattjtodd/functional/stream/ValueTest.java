package com.mattjtodd.functional.stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.function.Supplier;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link Value} type.
 */
@RunWith(MockitoJUnitRunner.class)
public class ValueTest {
  private Object headValue;

  private Stream<Object> tailValue;

  @Mock
  private Supplier<Object> head;

  @Mock
  private Supplier<Stream<Object>> tail;

  @InjectMocks
  private Value<Object> value;

  @Before
  public void setUp() {
    headValue = new Object();
    tailValue = Streams.constant(new Object());
    when(head.get()).thenReturn(headValue);
    when(tail.get()).thenReturn(tailValue);
    value = new Value<>(head, tail);
  }

  @Test
  public void getHeadCheckingExpected() {
    assertThat(value.getHead().get(), is(headValue));
  }

  @Test
  public void getHeadCheckingMemoized() {
    Supplier<?> actual = value.getHead();
    actual.get();
    actual.get();

    verify(head, times(1)).get();
  }

  @Test
  public void evalHeadCheckingExpected() {
    assertThat(value.evalHead(), is(headValue));
  }

  @Test
  public void evalHeadCheckingMemoized() {
    value.evalHead();
    value.evalHead();

    verify(head, times(1)).get();
  }

  @Test
  public void getTailCheckingExpected() {
    assertThat(value.getTail().get(), is(tailValue));
  }

  @Test
  public void getTailCheckingMemozied() {
    Supplier<Stream<Object>> actual = value.getTail();
    actual.get();
    actual.get();

    verify(tail, times(1)).get();
  }

  @Test
  public void evalTailCheckingMemoized() {
    value.evalTail();
    value.evalTail();

    verify(tail, times(1)).get();
  }
}
