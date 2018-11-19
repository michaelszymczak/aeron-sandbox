package com.michaelszymczak.foo.samples;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Created 18/11/18.
 */
public class BasicPublisherAndSubscriberTest {

  @Test
  public void shouldWorkTogether() throws Exception {
    BasicPublisher publisher1 = new BasicPublisher(true, "aeron:udp?endpoint=localhost:40124");
    BasicSubscriber subscriber1 = new BasicSubscriber(true, "aeron:udp?endpoint=localhost:40124");
    BasicPublisher publisher2 = new BasicPublisher(true, "aeron:udp?endpoint=localhost:40125");
    BasicSubscriber subscriber2 = new BasicSubscriber(true, "aeron:udp?endpoint=localhost:40125");
    AtomicBoolean running = new AtomicBoolean(true);
    newSingleThreadExecutor().submit(() -> subscriber1.start(running));
    newSingleThreadExecutor().submit(new DoneWhenInterrupted(publisher1::start));
    newSingleThreadExecutor().submit(() -> subscriber2.start(running));
    newSingleThreadExecutor().submit(new DoneWhenInterrupted(publisher2::start));
    Thread.sleep(3000);
    running.set(false);
    Thread.sleep(2000);
  }

  private static class DoneWhenInterrupted implements Runnable
  {
    private final PotentiallyInterrupted potentiallyInterrupted;

    public DoneWhenInterrupted(PotentiallyInterrupted potentiallyInterrupted) {
      this.potentiallyInterrupted = requireNonNull(potentiallyInterrupted);
    }

    @Override
    public void run() {
      try {
        potentiallyInterrupted.run();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @FunctionalInterface
  private interface PotentiallyInterrupted
  {
    void run() throws InterruptedException;
  }
}