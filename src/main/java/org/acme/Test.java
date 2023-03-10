package org.acme;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.JMSProducer;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/pooled-jms")
@ApplicationScoped
public class Test {
    private String queue = "test-jms";

    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    TransactionManager tm;

    @GET
    public String get() {
        return receive();
    }

    @POST
    @Transactional
    public void post(String message) throws Exception {
        send(message);

        if ("fail".equals(message)) {
            tm.setRollbackOnly();
        }
    }

    private void send(String body) {
        try (JMSContext context = connectionFactory.createContext()) {
            JMSProducer producer = context.createProducer();
            producer.send(context.createQueue(queue), body);
        }
    }

    private String receive() {
        try (JMSContext context = connectionFactory.createContext();
                JMSConsumer consumer = context.createConsumer(context.createQueue(queue))) {
            Message message = consumer.receive(1000L);
            if (message != null) {
                return message.getBody(String.class);
            } else {
                return null;
            }
        } catch (JMSException e) {
            throw new RuntimeException("Could not receive message", e);
        }
    }
}
