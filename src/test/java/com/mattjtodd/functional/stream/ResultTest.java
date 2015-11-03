package com.mattjtodd.functional.stream;

import static com.mattjtodd.functional.stream.Result.latest;
import static com.mattjtodd.functional.stream.Result.terminal;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for the {@link Result}.
 */
public class ResultTest {

  @Test
  public void latestCheckingFalseTerminal() {
    assertFalse(latest(new Object()).isTerminal());
  }

  @Test(expected = NullPointerException.class)
  public void latestCheckingNullValueException() {
    latest(null);
  }

  @Test
  public void terminalCheckingTrueTerminal() {
    assertTrue(terminal(new Object()).isTerminal());
  }

  @Test(expected = NullPointerException.class)
  public void terminalCheckingNullValueException() {
    terminal(null);
  }
}
