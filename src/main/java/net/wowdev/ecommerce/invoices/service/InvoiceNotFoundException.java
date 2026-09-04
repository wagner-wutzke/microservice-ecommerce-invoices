package net.wowdev.ecommerce.invoices.service;

import java.util.UUID;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(final UUID id) {
        super("Invoice record not found: " + id);
    }
}
