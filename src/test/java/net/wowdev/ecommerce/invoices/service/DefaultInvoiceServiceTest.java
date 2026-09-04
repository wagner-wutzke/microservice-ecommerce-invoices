package net.wowdev.ecommerce.invoices.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.InvoiceDTO;
import net.wowdev.ecommerce.domain.entity.InvoiceEntity;
import net.wowdev.ecommerce.domain.events.InvoiceCompletedEvent;
import net.wowdev.ecommerce.domain.events.InvoiceFailedEvent;
import net.wowdev.ecommerce.invoices.TestFixtures;
import net.wowdev.ecommerce.invoices.messaging.InvoiceProducer;
import net.wowdev.ecommerce.invoices.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class DefaultInvoiceServiceTest {
  @Mock InvoiceRepository repository;
  @Mock InvoiceProducer producer;
  @InjectMocks DefaultInvoiceService service;
  private UUID id;

  @BeforeEach
  void setUp() {
    id = UUID.randomUUID();
  }

  @Test
  void findsInvoice() {
    when(repository.findById(id)).thenReturn(Optional.of(entity(id)));
    assertEquals(id, service.findById(id).getId());
  }

  @Test
  void rejectsMissingInvoice() {
    when(repository.findById(id)).thenReturn(Optional.empty());
    assertThrows(InvoiceNotFoundException.class, () -> service.findById(id));
  }

  @Test
  void listsInvoices() {
    when(repository.findAll(any(PageRequest.class)))
        .thenReturn(new PageImpl<>(java.util.List.of(entity(id))));
    assertEquals(1, service.findAll(PageRequest.of(0, 20)).getTotalElements());
  }

  @Test
  void createsInvoice() {
    lenient().when(repository.save(any(InvoiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    InvoiceDTO result = service.create(TestFixtures.invoice(id));
    assertEquals("INV-1", result.getInvoiceNumber());
  }

  @Test
  void rejectsInvalidInvoice() {
    assertThrows(IllegalArgumentException.class, () -> service.create(new InvoiceDTO()));
  }

  @Test
  void updatesInvoice() {
    InvoiceEntity current = entity(id);
    when(repository.findById(id)).thenReturn(Optional.of(current));
    when(repository.save(current)).thenReturn(current);
    InvoiceDTO request = TestFixtures.invoice(id);
    request.setDelivered(true);
    assertTrue(service.update(id, request).isDelivered());
  }

  @Test
  void deletesExistingInvoice() {
    when(repository.existsById(id)).thenReturn(true);
    service.delete(id);
    verify(repository).deleteById(id);
  }

  @Test
  void rejectsDeletingMissingInvoice() {
    when(repository.existsById(id)).thenReturn(false);
    assertThrows(InvoiceNotFoundException.class, () -> service.delete(id));
  }

  @Test
  void processesOrder() {
    var order = TestFixtures.order();
    var publishedEvents = new java.util.ArrayList<Object>();
    lenient().when(repository.save(any(InvoiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    lenient()
        .doAnswer(
            invocation -> {
              publishedEvents.add(invocation.getArgument(0));
              return null;
            })
        .when(producer)
        .publish(any(InvoiceCompletedEvent.class));
    lenient()
        .doAnswer(
            invocation -> {
              publishedEvents.add(invocation.getArgument(0));
              return null;
            })
        .when(producer)
        .publish(any(InvoiceFailedEvent.class));

    service.process(order);

    assertEquals(1, publishedEvents.size());
    var event = publishedEvents.get(0);
    assertTrue(event instanceof InvoiceCompletedEvent || event instanceof InvoiceFailedEvent);
    if (event instanceof InvoiceCompletedEvent completedEvent) {
      assertEquals(order.getId().toString(), completedEvent.transactionId());
    } else {
      assertEquals(order.getId().toString(), ((InvoiceFailedEvent) event).transactionId());
    }
  }

  @Test
  void rejectsInvalidOrder() {
    assertThrows(IllegalArgumentException.class, () -> service.process(null));
  }

  private InvoiceEntity entity(final UUID value) {
    InvoiceDTO dto = TestFixtures.invoice(value);
    return net.wowdev.ecommerce.domain.mapper.InvoiceMapper.toEntity(dto);
  }
}
