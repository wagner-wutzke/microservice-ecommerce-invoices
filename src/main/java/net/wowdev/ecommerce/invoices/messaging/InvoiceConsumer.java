package net.wowdev.ecommerce.invoices.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.events.PaymentCompletedEvent;
import net.wowdev.ecommerce.domain.events.ShippingFailedEvent;
import net.wowdev.ecommerce.invoices.service.InvoiceService;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
    groupId = "${spring.kafka.consumer.group-id}",
    topics = {
      "${app.kafka.orders-topic}",
      "${app.kafka.payments-topic}",
      "${app.kafka.invoices-topic}"
    },
    containerFactory = "kafkaListenerContainerFactory")
public class InvoiceConsumer {
  private final InvoiceService invoiceService;

  @KafkaHandler
  public void handlePaymentCompleted(final PaymentCompletedEvent event) {
    log.debug(
        ">> Processing PaymentCompletedEvent event sent from {}. Event id {}",
        event.origin(),
        event.eventId());
    invoiceService.process(event.orderDTO());
  }

  @KafkaHandler
  public void handleShippingFailed(final ShippingFailedEvent event) {
    log.debug(
        ">> Processing ShippingFailedEvent event sent from {}. Event id {}",
        event.origin(),
        event.eventId());
    invoiceService.compensate(event.orderDTO(), event.reason());
  }

  @KafkaHandler(isDefault = true)
  public void handleUnknown(final Object event) {
    log.debug(">> Received an unmapped event of type {}", event.getClass().getSimpleName());
  }
}
