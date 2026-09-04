package net.wowdev.ecommerce.invoices.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class PersistenceConfigTest {
    @Test void createsConfiguration() {
        assertNotNull(new PersistenceConfig());
    }
}
