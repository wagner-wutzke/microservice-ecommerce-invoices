package net.wowdev.ecommerce.invoices.messaging;

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
        producer.publishAfterCommit(completed);
        verify(template).send("invoices-topic.v1", completed.ordetDTO().getId().toString(), completed);
    }

    @Test void publishesFailedImmediatelyWithoutTransaction() {
        var event = new InvoiceFailedEvent(UUID.randomUUID(), "tx", TestFixtures.order(), Instant.now(), "bad", "test");
        producer.publishAfterCommit(event);
        verify(template).send("invoices-topic.v1", event.ordetDTO().getId().toString(), event);
    }

    @Test void publishesAfterTransactionCommit() {
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            producer.publishAfterCommit(completed);
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .getSynchronizations().get(0).afterCommit();
            verify(template).send("invoices-topic.v1", completed.ordetDTO().getId().toString(), completed);
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
