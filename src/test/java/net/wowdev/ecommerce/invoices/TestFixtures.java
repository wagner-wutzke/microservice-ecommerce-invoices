package net.wowdev.ecommerce.invoices;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.InvoiceDTO;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.dto.PaymentDTO;
import net.wowdev.ecommerce.domain.enums.PaymentMethod;
import net.wowdev.ecommerce.domain.enums.PaymentStatus;

public final class TestFixtures {
  private TestFixtures() {}

  public static InvoiceDTO invoice(final UUID id) {
    return new InvoiceDTO(
        id,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "INV-1",
        false,
        "https://example.test/invoice.pdf",
        "invoice.pdf",
        null,
        null);
  }

  public static PaymentDTO payment() {
    return new PaymentDTO(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "token",
        "tx",
        PaymentStatus.PENDING,
        BigDecimal.valueOf(135.00),
        PaymentMethod.CREDIT_CARD,
        Instant.now(),
        Instant.now());
  }

  public static OrderDTO order() {
    final OrderDTO order = new OrderDTO();
    order.setId(UUID.randomUUID());
    order.setCustomerId(UUID.randomUUID());
    return order;
  }
}
