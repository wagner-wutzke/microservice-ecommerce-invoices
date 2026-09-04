package net.wowdev.ecommerce.invoices.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.InvoiceDTO;
import net.wowdev.ecommerce.invoices.TestFixtures;
import net.wowdev.ecommerce.invoices.service.InvoiceNotFoundException;
import net.wowdev.ecommerce.invoices.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

class InvoiceControllerTest {
    private InvoiceService service;
    private InvoiceController controller;
    private UUID id;

    @BeforeEach void setUp() { service = mock(InvoiceService.class); controller = new InvoiceController(service); id = UUID.randomUUID(); }

    @Test void gets() { when(service.findById(id)).thenReturn(TestFixtures.invoice(id)); assertEquals(id, controller.get(id).getId()); }
    @Test void lists() { when(service.findAll(any())).thenReturn(new PageImpl<>(java.util.List.of())); assertEquals(0, controller.list(0, 20).getTotalElements()); }
    @Test void rejectsBadPage() { assertThrows(IllegalArgumentException.class, () -> controller.list(-1, 20)); assertThrows(IllegalArgumentException.class, () -> controller.list(0, 101)); }
    @Test void creates() { InvoiceDTO dto = TestFixtures.invoice(id); when(service.create(dto)).thenReturn(dto); assertEquals(HttpStatus.CREATED, controller.create(dto).getStatusCode()); }
    @Test void updates() { InvoiceDTO dto = TestFixtures.invoice(id); when(service.update(id, dto)).thenReturn(dto); assertSame(dto, controller.update(id, dto)); }
    @Test void deletes() { assertEquals(HttpStatus.NO_CONTENT, controller.delete(id).getStatusCode()); verify(service).delete(id); }
    @Test void mapsErrors() { assertEquals(HttpStatus.NOT_FOUND, controller.notFound(new InvoiceNotFoundException(id)).getStatusCode()); assertEquals(HttpStatus.BAD_REQUEST, controller.badRequest(new IllegalArgumentException("bad")).getStatusCode()); }
}
