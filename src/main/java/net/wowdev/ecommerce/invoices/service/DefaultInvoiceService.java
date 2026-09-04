package net.wowdev.ecommerce.invoices.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.dto.InvoiceDTO;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.entity.InvoiceEntity;
import net.wowdev.ecommerce.domain.events.InvoiceCompletedEvent;
import net.wowdev.ecommerce.domain.mapper.InvoiceMapper;
import net.wowdev.ecommerce.invoices.messaging.InvoiceProducer;
import net.wowdev.ecommerce.invoices.repository.InvoiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultInvoiceService implements InvoiceService {
    private static final String ORIGIN = "invoices-service";
    private final InvoiceRepository repository;
    private final InvoiceProducer producer;

    @Override
    @Transactional(readOnly = true)
    public InvoiceDTO findById(final UUID id) {
        return repository.findById(id).map(InvoiceMapper::toDto)
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
        final InvoiceDTO saved = InvoiceMapper.toDto(repository.save(entity));
        return saved;
    }

    @Override
    @Transactional
    public InvoiceDTO update(final UUID id, final InvoiceDTO invoice) {
        validate(invoice);
        final InvoiceEntity current = repository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));
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
    public InvoiceDTO process(final OrderDTO order) {
        if (order == null || order.getId() == null || order.getCustomerId() == null) {
            throw new IllegalArgumentException("Order id and customer id are required");
        }
        final InvoiceDTO invoice = new InvoiceDTO(
                null, order.getId(), order.getCustomerId(), "INV-" + UUID.randomUUID(),
                false, null, "invoice-" + order.getId() + ".pdf", null, null);
        final InvoiceDTO saved = create(invoice);
        producer.publishAfterCommit(new InvoiceCompletedEvent(
                UUID.randomUUID(), order.getId().toString(), order, Instant.now(), ORIGIN));
        log.debug("Created invoice {} for order {}", saved.getId(), order.getId());
        return saved;
    }

    private void validate(final InvoiceDTO invoice) {
        if (invoice == null || invoice.getOrderId() == null || invoice.getCustomerId() == null
                || invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "orderId, customerId and invoiceNumber are required");
        }
    }
}
