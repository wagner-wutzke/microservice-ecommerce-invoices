package net.wowdev.ecommerce.invoices;

import static org.junit.jupiter.api.Assertions.*;

import net.wowdev.ecommerce.domain.dto.InvoiceDTO;
import net.wowdev.ecommerce.domain.entity.InvoiceEntity;
import net.wowdev.ecommerce.domain.mapper.InvoiceMapper;
import org.junit.jupiter.api.Test;

class InvoiceMapperTest {
  @Test
  void mapsBothDirectionsAndNulls() {
    InvoiceDTO source = TestFixtures.invoice(null);
    InvoiceEntity entity = InvoiceMapper.toEntity(source);
    assertEquals(source.getInvoiceNumber(), entity.getInvoiceNumber());
    assertEquals(source.getCustomerId(), InvoiceMapper.toDto(entity).getCustomerId());
    assertNull(InvoiceMapper.toEntity(null));
    assertNull(InvoiceMapper.toDto(null));
  }
}
