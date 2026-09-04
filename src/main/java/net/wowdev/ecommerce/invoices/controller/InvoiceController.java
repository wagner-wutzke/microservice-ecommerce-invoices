package net.wowdev.ecommerce.invoices.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.wowdev.ecommerce.domain.dto.InvoiceDTO;
import net.wowdev.ecommerce.invoices.service.InvoiceNotFoundException;
import net.wowdev.ecommerce.invoices.service.InvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {
  private final InvoiceService service;

  @GetMapping("/{id}")
  public InvoiceDTO get(@PathVariable final UUID id) {
    return service.findById(id);
  }

  @GetMapping
  public Page<InvoiceDTO> list(
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int pageSize) {
    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new IllegalArgumentException(
          "page must be non-negative and pageSize must be between 1 and 100");
    }
    return service.findAll(
        PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
  }

  @PostMapping
  public ResponseEntity<InvoiceDTO> create(@Valid @RequestBody final InvoiceDTO invoice) {
    final InvoiceDTO created = service.create(invoice);
    return ResponseEntity.created(URI.create("/api/v1/invoices/" + created.getId())).body(created);
  }

  @PutMapping("/{id}")
  public InvoiceDTO update(
      @PathVariable final UUID id, @Valid @RequestBody final InvoiceDTO invoice) {
    return service.update(id, invoice);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable final UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @ExceptionHandler(InvoiceNotFoundException.class)
  public ResponseEntity<Void> notFound(final InvoiceNotFoundException exception) {
    return ResponseEntity.notFound().build();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> badRequest(final IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(exception.getMessage());
  }
}
