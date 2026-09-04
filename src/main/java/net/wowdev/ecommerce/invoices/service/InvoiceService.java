package net.wowdev.ecommerce.invoices.service;

import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.InvoiceDTO;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvoiceService {
  InvoiceDTO findById(UUID id);

  Page<InvoiceDTO> findAll(Pageable pageable);

  InvoiceDTO create(InvoiceDTO invoice);

  InvoiceDTO update(UUID id, InvoiceDTO invoice);

  void delete(UUID id);

  void process(OrderDTO order);

  void compensate(OrderDTO order, String reason);

}
