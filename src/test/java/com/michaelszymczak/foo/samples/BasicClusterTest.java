package com.michaelszymczak.foo.samples;

import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.client.AeronArchive;
import io.aeron.cluster.ClusteredMediaDriver;
import io.aeron.cluster.ConsensusModule;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.MinMulticastFlowControlSupplier;
import io.aeron.driver.ThreadingMode;
import org.agrona.CloseHelper;
import org.agrona.ExpandableArrayBuffer;
import org.junit.Test;

import java.io.File;

import static io.aeron.CommonContext.generateRandomDirName;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class BasicClusterTest {

  @Test(timeout = 10_000)
  public void shouldExchangeMessages() throws Exception {
    final MediaDriver.Context mediaDriverContext = new MediaDriver.Context()
            .aeronDirectoryName(generateRandomDirName())
            .threadingMode(ThreadingMode.SHARED)
            .dirDeleteOnStart(true);
    final AeronArchive.Context aeronArchiveContext = new AeronArchive.Context()
            .controlRequestChannel("aeron:udp?endpoint=localhost:18012")
            .controlRequestStreamId(101)
            .controlResponseChannel("aeron:udp?endpoint=localhost:18013")
            .controlResponseStreamId(102)
            .aeronDirectoryName(mediaDriverContext.aeronDirectoryName());
    final Archive.Context archiveContext = new Archive.Context()
            .aeronDirectoryName(mediaDriverContext.aeronDirectoryName())
            .archiveDir(new File(mediaDriverContext.aeronDirectoryName(), "archive"))
            .controlChannel(aeronArchiveContext.controlRequestChannel())
            .controlStreamId(aeronArchiveContext.controlRequestStreamId())
            .localControlChannel("aeron:ipc")
            .localControlStreamId(aeronArchiveContext.controlRequestStreamId())
            .threadingMode(ArchiveThreadingMode.SHARED)
            .deleteArchiveOnStart(true);
    ConsensusModule.Context consensusModuleContext = new ConsensusModule.Context()
            .clusterMemberId(0)
            .clusterMembers("0,localhost:18110,localhost:18220,localhost:18330,localhost:18440,localhost:18012")
            .appointedLeaderId(0)
            .aeronDirectoryName(mediaDriverContext.aeronDirectoryName())
            .clusterDir(new File(mediaDriverContext.aeronDirectoryName(), "consensus-module"))
            .ingressChannel("aeron:udp")
            .logChannel("aeron:udp?control=localhost:55550")
            .archiveContext(aeronArchiveContext.clone())
            .deleteDirOnStart(true);

    final ClusteredMediaDriver driver = ClusteredMediaDriver.launch(
            mediaDriverContext, archiveContext, consensusModuleContext);

    final ClusteredServiceContainer container = ClusteredServiceContainer.launch(
            new ClusteredServiceContainer.Context()
                    .aeronDirectoryName(mediaDriverContext.aeronDirectoryName())
                    .archiveContext(aeronArchiveContext.clone())
                    .clusterDir(new File(mediaDriverContext.aeronDirectoryName(), "service"))
                    .clusteredService(new EchoService()));

    final MediaDriver clientMediaDriver = MediaDriver.launchEmbedded(
            new MediaDriver.Context().threadingMode(ThreadingMode.SHARED)
    );
    final ResponseCountingEgressListener egressListener = new ResponseCountingEgressListener();
    final AeronCluster client = AeronCluster.connect(
            new AeronCluster.Context()
                    .egressListener(egressListener)
                    .aeronDirectoryName(clientMediaDriver.aeronDirectoryName())
                    .ingressChannel("aeron:udp")
                    .clusterMemberEndpoints("0=localhost:18110"));

    final int numberOfExchangedMessages = 1_000_000;
    final ExpandableArrayBuffer msgBuffer = new ExpandableArrayBuffer();
    final String message = "Foo bar";
    msgBuffer.putStringWithoutLengthAscii(0, message);
    for (int i = 0; i < numberOfExchangedMessages; i++) {
      while (client.offer(msgBuffer, 0, message.length()) < 0) {
        Thread.yield();
      }

      client.pollEgress();
    }

    while (egressListener.receivedResponses() < numberOfExchangedMessages) {
      Thread.yield();
      client.pollEgress();
    }

    assertThat(egressListener.receivedResponses(), is(numberOfExchangedMessages));

    // Teardown
    CloseHelper.close(client);
    CloseHelper.close(clientMediaDriver);
    clientMediaDriver.context().deleteAeronDirectory();
    CloseHelper.close(container);
    CloseHelper.close(driver);
    driver.mediaDriver().context().deleteAeronDirectory();

  }
}
