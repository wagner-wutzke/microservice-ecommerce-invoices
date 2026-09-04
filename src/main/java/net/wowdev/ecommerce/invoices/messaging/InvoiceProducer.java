package net.wowdev.ecommerce.invoices.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.events.InvoiceCompletedEvent;
import net.wowdev.ecommerce.domain.events.InvoiceFailedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.invoices-topic}")
    private String topic;

    public void publishAfterCommit(final InvoiceCompletedEvent event) {
        publishAfterCommit(event, event.ordetDTO().getId().toString());
    }

    public void publishAfterCommit(final InvoiceFailedEvent event) {
        publishAfterCommit(event, event.ordetDTO().getId().toString());
    }

    private void publishAfterCommit(final Object event, final String key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event, key);
                }
            });
            return;
        }
        send(event, key);
    }

    private void send(final Object event, final String key) {
        log.debug("Publishing invoice event with key {}", key);
        kafkaTemplate.send(topic, key, event);
    }
}
