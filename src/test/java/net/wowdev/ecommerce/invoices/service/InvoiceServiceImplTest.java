package net.wowdev.ecommerce.invoices.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.InvoiceDTO;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.entity.InvoiceEntity;
import net.wowdev.ecommerce.domain.events.InvoiceCompleted;
import net.wowdev.ecommerce.domain.events.InvoiceFailed;
import net.wowdev.ecommerce.invoices.TestFixtures;
import net.wowdev.ecommerce.invoices.messaging.InvoiceProducer;
import net.wowdev.ecommerce.invoices.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {
  @Mock InvoiceRepository repository;
  @Mock InvoiceProducer producer;
  @InjectMocks
  InvoiceServiceImpl service;
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
  void rejectsNullInvoice() {
    assertThrows(IllegalArgumentException.class, () -> service.create(null));
  }

  @Test
  void rejectsInvoiceWithoutOrder() {
    InvoiceDTO invoice = TestFixtures.invoice(id);
    invoice.setOrderId(null);
    assertThrows(IllegalArgumentException.class, () -> service.create(invoice));
  }

  @Test
  void rejectsInvoiceWithoutCustomer() {
    InvoiceDTO invoice = TestFixtures.invoice(id);
    invoice.setCustomerId(null);
    assertThrows(IllegalArgumentException.class, () -> service.create(invoice));
  }

  @Test
  void rejectsInvoiceWithoutInvoiceNumber() {
    InvoiceDTO invoice = TestFixtures.invoice(id);
    invoice.setInvoiceNumber(null);
    assertThrows(IllegalArgumentException.class, () -> service.create(invoice));
  }

  @Test
  void rejectsInvoiceWithBlankInvoiceNumber() {
    InvoiceDTO invoice = TestFixtures.invoice(id);
    invoice.setInvoiceNumber("   ");
    assertThrows(IllegalArgumentException.class, () -> service.create(invoice));
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
  void rejectsUpdateOfMissingInvoice() {
    when(repository.findById(id)).thenReturn(Optional.empty());
    assertThrows(
        InvoiceNotFoundException.class, () -> service.update(id, TestFixtures.invoice(id)));
    verify(repository, never()).save(any(InvoiceEntity.class));
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
        .publish(any(InvoiceCompleted.class));
    lenient()
        .doAnswer(
            invocation -> {
              publishedEvents.add(invocation.getArgument(0));
              return null;
            })
        .when(producer)
        .publish(any(InvoiceFailed.class));

    service.process(order);

    assertEquals(1, publishedEvents.size());
    var event = publishedEvents.get(0);
    assertTrue(event instanceof InvoiceCompleted || event instanceof InvoiceFailed);
    if (event instanceof InvoiceCompleted completedEvent) {
      assertEquals(order.getId().toString(), completedEvent.transactionId());
    } else {
      assertEquals(order.getId().toString(), ((InvoiceFailed) event).transactionId());
    }
  }

  @Test
  void publishesFailureWhenProcessingIsConfiguredToFail() {
    ReflectionTestUtils.setField(service, "failsWhenRunning", true);
    OrderDTO order = TestFixtures.order();

    service.process(order);

    ArgumentCaptor<InvoiceFailed> captor = ArgumentCaptor.forClass(InvoiceFailed.class);
    verify(producer).publish(captor.capture());
    assertEquals(order.getId().toString(), captor.getValue().transactionId());
    assertEquals("Invoicing service partner is not available.", captor.getValue().reason());
    verify(repository, never()).save(any(InvoiceEntity.class));
  }

  @Test
  void publishesFailureWhenInvoiceCreationFails() {
    RuntimeException failure = new RuntimeException("database unavailable");
    when(repository.save(any(InvoiceEntity.class))).thenThrow(failure);
    OrderDTO order = TestFixtures.order();

    service.process(order);

    ArgumentCaptor<InvoiceFailed> captor = ArgumentCaptor.forClass(InvoiceFailed.class);
    verify(producer).publish(captor.capture());
    assertEquals("database unavailable", captor.getValue().reason());
  }

  @Test
  void compensatesInvoiceAndPublishesFailure() {
    OrderDTO order = TestFixtures.order();

    service.compensate(order, "payment rejected");

    verify(repository).deleteByOrderId(order.getId());
    ArgumentCaptor<InvoiceFailed> captor = ArgumentCaptor.forClass(InvoiceFailed.class);
    verify(producer).publish(captor.capture());
    assertEquals(order.getId().toString(), captor.getValue().transactionId());
    assertEquals("payment rejected", captor.getValue().reason());
    assertEquals(InvoiceService.ORIGIN_SERVICE, captor.getValue().origin());
  }

  private InvoiceEntity entity(final UUID value) {
    InvoiceDTO dto = TestFixtures.invoice(value);
    return net.wowdev.ecommerce.domain.mapper.InvoiceMapper.toEntity(dto);
  }
}
