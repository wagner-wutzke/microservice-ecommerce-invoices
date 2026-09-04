package net.wowdev.ecommerce.invoices.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.events.OrderProcessingCompletedEvent;
import net.wowdev.ecommerce.domain.events.OrderProcessingFailedEvent;
import net.wowdev.ecommerce.invoices.service.InvoiceService;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(groupId = "${spring.kafka.consumer.group-id}",
        topics = {"${app.kafka.orders-topic}", "${app.kafka.invoices-topic}"},
        containerFactory = "kafkaListenerContainerFactory")
public class InvoiceConsumer {
    private final InvoiceService invoiceService;

    @KafkaHandler
    public void handleOrderProcessingCompleted(final OrderProcessingCompletedEvent event) {
        log.debug("Processing completed order event {}", event.eventId());
        invoiceService.process(event.orderDTO());
    }

    @KafkaHandler
    public void handleOrderProcessingFailed(final OrderProcessingFailedEvent event) {
        log.debug("Order processing failed for invoice flow: {}", event.reason());
    }

    @KafkaHandler(isDefault = true)
    public void handleUnknown(final Object event) {
        log.debug(">> Received an unmapped event of type {}", event.getClass().getSimpleName());
    }
}
