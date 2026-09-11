package net.wowdev.ecommerce.invoices.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.wowdev.ecommerce.domain.entity.ShipmentEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

class PersistenceConfigTest {
  @Test
  void configuresEntityScanningAndAuditing() {
    EntityScan entityScan = PersistenceConfig.class.getAnnotation(EntityScan.class);

    assertNotNull(entityScan);
    assertNotNull(ShipmentEntity.class.getAnnotation(jakarta.persistence.Entity.class));
    assertNotNull(PersistenceConfig.class.getAnnotation(EnableJpaAuditing.class));
  }
}
