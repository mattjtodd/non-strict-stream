package com.mattjtodd.functional.stream;

import static com.mattjtodd.functional.stream.Trampoline.done;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for the {@link Trampoline}.
 */
public class TrampolineTest {

  @Test(expected = UnsupportedOperationException.class)
  public void doneCheckingApplyThrowsException() {
    done(new Object()).apply();
  }

  @Test
  public void doneCheckingResult() {
    Object value = new Object();

    assertThat(done(value).result(), is(value));
  }

  @Test
  public void doneCheckingIsComplete() {
    assertTrue(done(new Object()).isComplete());
  }

  @Test
  public void applyCheckingIsCompleteFalse() {
    assertFalse(trampoline().isComplete());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void applyCheckingResultThrowsException() {
    trampoline().result();
  }

  @Test
  public void invokeCheckingResultThrowsException() {
    assertTrue(trampoline().invoke());
  }

  private static Trampoline<Boolean> trampoline() {
    return trampoline(false);
  }

  private static Trampoline<Boolean> trampoline(boolean terminal) {
    return terminal ? done(terminal) : () -> trampoline(!terminal);
  }
}
