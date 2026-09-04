package net.wowdev.ecommerce.invoices.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;
import net.wowdev.ecommerce.domain.events.InvoiceCompletedEvent;
import net.wowdev.ecommerce.domain.events.InvoiceFailedEvent;
import net.wowdev.ecommerce.invoices.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class InvoiceProducerTest {
    private KafkaTemplate<String, Object> template;
    private InvoiceProducer producer;
    private InvoiceCompletedEvent completed;

    @BeforeEach void setUp() {
        template = mock(KafkaTemplate.class);
        producer = new InvoiceProducer(template);
        ReflectionTestUtils.setField(producer, "topic", "invoices-topic.v1");
        completed = new InvoiceCompletedEvent(UUID.randomUUID(), "tx", TestFixtures.order(), Instant.now(), "test");
    }

    @Test void publishesCompletedImmediatelyWithoutTransaction() {
        producer.publish(completed);
        verify(template).send("invoices-topic.v1", completed.eventId().toString(), completed);
    }

    @Test void publishesFailedImmediatelyWithoutTransaction() {
        var event = new InvoiceFailedEvent(UUID.randomUUID(), "tx", TestFixtures.order(), Instant.now(), "bad", "test");
        producer.publish(event);
        verify(template).send("invoices-topic.v1", event.eventId().toString(), event);
    }

    @Test void publishesAfterTransactionCommit() throws NoSuchMethodException {
        var completedListener = InvoiceProducer.class
                .getMethod("publish", InvoiceCompletedEvent.class)
                .getAnnotation(TransactionalEventListener.class);
        var failedListener = InvoiceProducer.class
                .getMethod("publish", InvoiceFailedEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertEquals(TransactionPhase.AFTER_COMMIT, completedListener.phase());
        assertEquals(TransactionPhase.AFTER_COMMIT, failedListener.phase());
    }
}
