package org.mule.examples.springboot.usingtransactionalscopeinjmstodatabase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The {@code ee:multi-transactional action="ALWAYS_BEGIN"} scope: insert, TestComponent, then the JMS send to "out"
 * ({@code JOIN_IF_POSSIBLE}). The JDBC work runs in a {@code DataSourceTransactionManager} transaction; the JMS send
 * joins the listener's transacted session. Any exception rolls back both (best-effort 1PC, like Mule's
 * multi-transaction, which is not XA either).
 */
@Service
public class OrderTransactionalScope {

    private final JdbcClient jdbc;
    private final JmsTemplate jms;
    private final String outQueue;
    private final boolean throwAfterInsert;

    OrderTransactionalScope(JdbcClient jdbc, JmsTemplate jms, @Value("${jms.out-queue}") String outQueue,
                            @Value("${orders.throw-after-insert}") boolean throwAfterInsert) {
        this.jdbc = jdbc;
        this.jms = jms;
        this.outQueue = outQueue;
        this.throwAfterInsert = throwAfterInsert;
    }

    @Transactional(rollbackFor = Exception.class)
    public void process(Order order) throws MyException {
        int inserted = jdbc.sql("insert into orders(item_id,item_units,customer_id) values (?, ?, ?)")
                .params(order.itemId(), order.itemUnits(), order.customerId())
                .update();
        if (throwAfterInsert) {
            throw new MyException("exception customized");
        }
        // db:insert leaves the update count as payload; the JMS endpoint sends it as an ObjectMessage
        jms.convertAndSend(outQueue, Integer.valueOf(inserted));
    }
}
