package net.wowdev.ecommerce.invoices.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

class KafkaConfigTest {

  private KafkaConfig config;

  @BeforeEach
  void setUp() {
    config = new KafkaConfig();
    ReflectionTestUtils.setField(config, "consumerGroup", "invoices-group");
    ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
    ReflectionTestUtils.setField(config, "acks", "all");
    ReflectionTestUtils.setField(config, "deliveryTimeout", "30000");
    ReflectionTestUtils.setField(config, "requestTimeout", "10000");
    ReflectionTestUtils.setField(config, "maxRequestsInFlight", 5);
    ReflectionTestUtils.setField(config, "linger", "0");
    ReflectionTestUtils.setField(config, "idempotence", true);
    ReflectionTestUtils.setField(config, "retries", 3);
    ReflectionTestUtils.setField(config, "trustedPackages", "net.wowdev.ecommerce.domain.events");
  }

  @Test
  void createsKafkaBeans() {
    ProducerFactory<String, Object> producerFactory = config.producerFactory();
    assertNotNull(config.kafkaTemplate(producerFactory));
    assertNotNull(config.consumerFactory());
    assertNotNull(
        config.kafkaListenerContainerFactory(config.consumerFactory(), mock(KafkaTemplate.class)));
  }
}
