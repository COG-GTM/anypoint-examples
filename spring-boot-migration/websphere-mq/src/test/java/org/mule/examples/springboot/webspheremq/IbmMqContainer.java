package org.mule.examples.springboot.webspheremq;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

/**
 * IBM MQ Advanced for Developers (free developer image) with queue manager QM1, channel DEV.APP.SVRCONN and the
 * queues "in" and "out" the Mule example expects. The app user's password is generated per run.
 */
final class IbmMqContainer extends GenericContainer<IbmMqContainer> {

    static final String IMAGE = "icr.io/ibm-messaging/mq:9.4.1.0-r1";
    static final String QUEUE_MANAGER = "QM1";
    static final String CHANNEL = "DEV.APP.SVRCONN";
    static final String USER = "app";
    final String password = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);

    IbmMqContainer() {
        super(DockerImageName.parse(IMAGE));
        withEnv("LICENSE", "accept");
        withEnv("MQ_QMGR_NAME", QUEUE_MANAGER);
        withEnv("MQ_APP_PASSWORD", password);
        withEnv("MQ_ENABLE_EMBEDDED_WEB_SERVER", "false");
        withCopyToContainer(Transferable.of("""
                DEFINE QLOCAL('in') REPLACE
                DEFINE QLOCAL('out') REPLACE
                SET AUTHREC PROFILE('in') OBJTYPE(QUEUE) PRINCIPAL('app') AUTHADD(BROWSE,GET,INQ,PUT,DSP)
                SET AUTHREC PROFILE('out') OBJTYPE(QUEUE) PRINCIPAL('app') AUTHADD(BROWSE,GET,INQ,PUT,DSP)
                """), "/etc/mqm/20-websphere-mq-example.mqsc");
        withExposedPorts(1414);
        waitingFor(Wait.forLogMessage(".*AMQ5026I.*", 1).withStartupTimeout(Duration.ofMinutes(3)));
    }

    String connName() {
        return getHost() + "(" + getMappedPort(1414) + ")";
    }
}
