package com.michaelszymczak.foo.samples;

import io.aeron.cluster.client.EgressListener;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.collections.MutableInteger;

/**
 * Created 19/11/18.
 */
class ResponseCountingEgressListener implements EgressListener {
  private final MutableInteger responseCount = new MutableInteger();

  @Override
  public void onMessage(long clusterSessionId, long timestampMs, DirectBuffer buffer, int offset, int length, Header header) {
    responseCount.value++;
  }

  public int receivedResponses() {
    return responseCount.get();
  }
}
