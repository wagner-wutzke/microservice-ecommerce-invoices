package net.wowdev.ecommerce.invoices.repository;

import java.util.UUID;
import net.wowdev.ecommerce.domain.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {
}
