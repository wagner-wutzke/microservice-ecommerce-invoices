package net.wowdev.ecommerce.invoices.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.dto.InvoiceDTO;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.entity.InvoiceEntity;
import net.wowdev.ecommerce.domain.events.InvoiceCompleted;
import net.wowdev.ecommerce.domain.events.InvoiceFailed;
import net.wowdev.ecommerce.domain.mapper.InvoiceMapper;
import net.wowdev.ecommerce.invoices.messaging.InvoiceProducer;
import net.wowdev.ecommerce.invoices.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

  private static final String ORIGIN_SERVICE = "INVOICES-SERVICE";
  private final InvoiceRepository repository;
  private final InvoiceProducer producer;

  @Value(value = "${app.service.invoices.failing}")
  private boolean failsWhenRunning;

  @Override
  @Transactional(readOnly = true)
  public InvoiceDTO findById(final UUID id) {
    return repository
        .findById(id)
        .map(InvoiceMapper::toDto)
        .orElseThrow(() -> new InvoiceNotFoundException(id));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<InvoiceDTO> findAll(final Pageable pageable) {
    return repository.findAll(pageable).map(InvoiceMapper::toDto);
  }

  @Override
  @Transactional
  public InvoiceDTO create(final InvoiceDTO invoice) {
    validate(invoice);
    final InvoiceEntity entity = InvoiceMapper.toEntity(invoice);
    return InvoiceMapper.toDto(repository.save(entity));
  }

  @Override
  @Transactional
  public InvoiceDTO update(final UUID id, final InvoiceDTO invoice) {
    validate(invoice);
    final InvoiceEntity current =
        repository.findById(id).orElseThrow(() -> new InvoiceNotFoundException(id));
    current.setOrderId(invoice.getOrderId());
    current.setCustomerId(invoice.getCustomerId());
    current.setInvoiceNumber(invoice.getInvoiceNumber());
    current.setDelivered(invoice.isDelivered());
    current.setDocumentUrl(invoice.getDocumentUrl());
    current.setDocumentName(invoice.getDocumentName());
    return InvoiceMapper.toDto(repository.save(current));
  }

  @Override
  @Transactional
  public void delete(final UUID id) {
    if (!repository.existsById(id)) {
      throw new InvoiceNotFoundException(id);
    }
    repository.deleteById(id);
  }

  @Override
  @Transactional
  public void process(final OrderDTO orderDTO) {

    InvoiceDTO invoice =
        new InvoiceDTO(
            null,
            orderDTO.getId(),
            orderDTO.getCustomerId(),
            "INV-" + UUID.randomUUID(),
            false,
            null,
            "invoice-" + orderDTO.getId() + ".pdf",
            null,
            null);

    try {
      if (failsWhenRunning()) {
        throw new RuntimeException("Invoicing service partner is not available.");
      }
      invoice = create(invoice);
      publishInvoiceCompletedEvent(orderDTO);
      log.debug(">> Created invoice {} for order {}", invoice.getId(), orderDTO.getId());
    } catch (RuntimeException e) {
      log.debug(
          ">> Invoice generation for order {} failed. Reason: {}", orderDTO.getId(), e.getMessage());
      publishInvoiceFailedEvent(orderDTO, e.getMessage());
    }
  }

  @Override
  @Transactional
  public void compensate(OrderDTO orderDTO, String reason) {
    log.debug(">> Compensating Invoice processing for order: {}", orderDTO.getId());

    repository.deleteByOrderId(orderDTO.getId());

    publishInvoiceFailedEvent(orderDTO, reason);
  }

  private void validate(final InvoiceDTO invoice) {
    if (invoice == null
        || invoice.getOrderId() == null
        || invoice.getCustomerId() == null
        || invoice.getInvoiceNumber() == null
        || invoice.getInvoiceNumber().isBlank()) {
      throw new IllegalArgumentException("orderId, customerId and invoiceNumber are required");
    }
  }

  private void publishInvoiceCompletedEvent(OrderDTO orderDTO) {
    producer.publish(
        new InvoiceCompleted(
            UUID.randomUUID(),
            orderDTO.getId().toString(),
            orderDTO,
            Instant.now(),
            ORIGIN_SERVICE));
  }

  private void publishInvoiceFailedEvent(OrderDTO orderDTO, String reason) {
    producer.publish(
        new InvoiceFailed(
            UUID.randomUUID(),
            orderDTO.getId().toString(),
            orderDTO,
            Instant.now(),
            reason,
            ORIGIN_SERVICE));
  }

  private boolean failsWhenRunning() {
    if (this.failsWhenRunning) {
      log.debug(
          """
          >> Service is configured to be failing when processing events. "
             This option can be configured with the "SERVICE_INVOICES_FAILING" environment variable.
          """);
    }
    return this.failsWhenRunning;
  }
}
