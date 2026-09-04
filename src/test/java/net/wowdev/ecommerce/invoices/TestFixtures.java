package net.wowdev.ecommerce.invoices;

import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.InvoiceDTO;
import net.wowdev.ecommerce.domain.dto.OrderDTO;

public final class TestFixtures {
    private TestFixtures() {
    }

    public static InvoiceDTO invoice(final UUID id) {
        return new InvoiceDTO(id, UUID.randomUUID(), UUID.randomUUID(), "INV-1", false,
                "https://example.test/invoice.pdf", "invoice.pdf", null, null);
    }

    public static OrderDTO order() {
        final OrderDTO order = new OrderDTO();
        order.setId(UUID.randomUUID());
        order.setCustomerId(UUID.randomUUID());
        return order;
    }
}
