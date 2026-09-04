package net.wowdev.ecommerce.invoices.messaging;

import static org.mockito.Mockito.*;

import net.wowdev.ecommerce.domain.events.OrderProcessingCompletedEvent;
import net.wowdev.ecommerce.domain.events.OrderProcessingFailedEvent;
import net.wowdev.ecommerce.invoices.TestFixtures;
import net.wowdev.ecommerce.invoices.service.InvoiceService;
import org.junit.jupiter.api.Test;

class InvoiceConsumerTest {
    private final InvoiceService service = mock(InvoiceService.class);
    private final InvoiceConsumer consumer = new InvoiceConsumer(service);

    @Test void processesCompletedOrder() {
        var order = TestFixtures.order();
        consumer.handleOrderProcessingCompleted(new OrderProcessingCompletedEvent(
                java.util.UUID.randomUUID(), "tx", order, java.time.Instant.now(), "orders"));
        verify(service).process(order);
    }

    @Test void acceptsFailedAndUnknownEvents() {
        var order = TestFixtures.order();
        consumer.handleOrderProcessingFailed(new OrderProcessingFailedEvent(
                java.util.UUID.randomUUID(), "tx", order, "failed", java.time.Instant.now(), "orders"));
        consumer.handleUnknown("unknown");
        verifyNoInteractions(service);
    }
}
