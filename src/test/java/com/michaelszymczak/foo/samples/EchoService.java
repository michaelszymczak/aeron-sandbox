package com.michaelszymczak.foo.samples;

import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;

class EchoService implements ClusteredService {

  private Cluster cluster;

  @Override
  public void onStart(Cluster cluster) {
    this.cluster = cluster;
  }

  @Override
  public void onSessionOpen(ClientSession session, long timestampMs) {

  }

  @Override
  public void onSessionClose(ClientSession session, long timestampMs, CloseReason closeReason) {

  }

  public void onSessionMessage(
          final ClientSession session,
          final long timestampMs,
          final DirectBuffer buffer,
          final int offset,
          final int length,
          final Header header) {
    while (session.offer(buffer, offset, length) < 0) {
      cluster.idle();
    }
  }

  @Override
  public void onTimerEvent(long correlationId, long timestampMs) {

  }

  @Override
  public void onTakeSnapshot(Publication snapshotPublication) {

  }

  @Override
  public void onLoadSnapshot(Image snapshotImage) {

  }

  @Override
  public void onRoleChange(Cluster.Role newRole) {

  }
}
