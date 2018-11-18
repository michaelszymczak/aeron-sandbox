package com.michaelszymczak.foo.samples;

import org.junit.Test;

import java.util.Objects;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Created 18/11/18.
 */
public class BasicPublisherAndSubscriberTest {

  @Test
  public void shouldWorkTogether() throws Exception {
    BasicPublisher publisher = new BasicPublisher(true);
    BasicSubscriber subscriber = new BasicSubscriber(true);
    newSingleThreadExecutor().submit(new DoneWhenInterrupted(publisher::start));
    newSingleThreadExecutor().submit(subscriber::start);
    Thread.sleep(5000);
  }

  private static class DoneWhenInterrupted implements Runnable
  {
    private final Interruptable interruptable;

    public DoneWhenInterrupted(Interruptable interruptable) {
      this.interruptable = Objects.requireNonNull(interruptable);
    }

    @Override
    public void run() {
      try {
        interruptable.run();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private interface Interruptable
  {
    void run() throws InterruptedException;
  }
}