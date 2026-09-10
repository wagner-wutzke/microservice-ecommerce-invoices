package net.wowdev.ecommerce.invoices.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.events.PaymentCompleted;
import net.wowdev.ecommerce.domain.events.ShipmentFailed;
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
      "${app.kafka.payments-topic}",
      "${app.kafka.invoices-topic}",
      "${app.kafka.shipments-topic}"
    },
    containerFactory = "kafkaListenerContainerFactory")
public class InvoiceConsumer {
  private final InvoiceService invoiceService;

  @KafkaHandler
  public void handle(final PaymentCompleted event) {
    log.debug(
        ">> Processing PaymentCompleted event sent from {}. Event id {}",
        event.origin(),
        event.eventId());
    invoiceService.process(event.orderDTO());
  }

  @KafkaHandler
  public void handle(final ShipmentFailed event) {
    log.debug(
        ">> Processing ShipmentFailed event sent from {}. Event id {}",
        event.origin(),
        event.eventId());
    invoiceService.compensate(event.orderDTO(), event.reason());
  }

  @KafkaHandler(isDefault = true)
  public void handleUnknown(final Object event) {
    //log.debug(">> Received an unmapped event of type {}", event.getClass().getSimpleName());
  }
}
