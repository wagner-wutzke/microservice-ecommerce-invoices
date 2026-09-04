package net.wowdev.ecommerce.invoices.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.wowdev.ecommerce.domain.entity.InvoiceEntity;
import net.wowdev.ecommerce.invoices.TestFixtures;
import net.wowdev.ecommerce.invoices.config.PersistenceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(PersistenceConfig.class)
class InvoiceRepositoryTest {
  @Autowired private InvoiceRepository repository;

  @Test
  void savesAndReadsInvoice() {
    InvoiceEntity saved =
        repository.saveAndFlush(
            net.wowdev.ecommerce.domain.mapper.InvoiceMapper.toEntity(TestFixtures.invoice(null)));
    assertTrue(repository.findById(saved.getId()).isPresent());
  }
}
