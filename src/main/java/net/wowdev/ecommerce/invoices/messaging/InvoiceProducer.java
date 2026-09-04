package net.wowdev.ecommerce.invoices.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.events.InvoiceCompletedEvent;
import net.wowdev.ecommerce.domain.events.InvoiceFailedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Value("${app.kafka.invoices-topic}")
  private String topic;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(final InvoiceCompletedEvent event) {
    log.debug(">> Publishing InvoiceCompletedEvent: {}", event.eventId());
    kafkaTemplate.send(topic, event.eventId().toString(), event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(final InvoiceFailedEvent event) {
    log.debug(">> Publishing InvoiceFailedEvent: {}", event.eventId());
    kafkaTemplate.send(topic, event.eventId().toString(), event);
  }
}