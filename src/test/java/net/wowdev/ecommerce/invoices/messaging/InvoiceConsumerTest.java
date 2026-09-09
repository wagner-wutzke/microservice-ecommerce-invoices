package net.wowdev.ecommerce.invoices.messaging;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;
import net.wowdev.ecommerce.domain.events.PaymentCompletedEvent;
import net.wowdev.ecommerce.domain.events.ShipmentFailedEvent;
import net.wowdev.ecommerce.invoices.TestFixtures;
import net.wowdev.ecommerce.invoices.service.InvoiceService;
import org.junit.jupiter.api.Test;

class InvoiceConsumerTest {
  private final InvoiceService service = mock(InvoiceService.class);
  private final InvoiceConsumer consumer = new InvoiceConsumer(service);

  @Test
  void processesCompletedOrder() {
    var order = TestFixtures.order();
    var payment = TestFixtures.payment();
    consumer.handle(
        new PaymentCompletedEvent(
            UUID.randomUUID(), "tx", order, payment, Instant.now(), "orders"));
    verify(service).process(order);
  }

  @Test
  void compensatesFailedShipmentAndIgnoresUnknownEvents() {
    var order = TestFixtures.order();
    consumer.handle(
        new ShipmentFailedEvent(UUID.randomUUID(), "tx", order, "failed", Instant.now(), "orders"));
    verify(service).compensate(order, "failed");

    clearInvocations(service);
    consumer.handleUnknown("unknown");
    verifyNoInteractions(service);
  }
}
