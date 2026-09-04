package net.wowdev.ecommerce.invoices.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConfig {
  @Value("${spring.kafka.consumer.group-id}")
  private String consumerGroup;

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.producer.acks:all}")
  private String acks;

  @Value("${spring.kafka.producer.properties.delivery.timeout.ms:30000}")
  private String deliveryTimeout;

  @Value("${spring.kafka.producer.properties.request.timeout.ms:10000}")
  private String requestTimeout;

  @Value("${spring.kafka.producer.properties.max.in.flight.requests.per.connection:5}")
  private Integer maxRequestsInFlight;

  @Value("${spring.kafka.producer.properties.linger.ms:0}")
  private String linger;

  @Value("${spring.kafka.producer.properties.enable.idempotence:true}")
  private boolean idempotence;

  @Value("${spring.kafka.producer.retries:3}")
  private Integer retries;

  @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
  private String trustedPackages;

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate(
      final ProducerFactory<String, Object> factory) {
    return new KafkaTemplate<>(factory);
  }

  @Bean
  public ProducerFactory<String, Object> producerFactory() {
    final Map<String, Object> properties = new HashMap<>();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
    properties.put(ProducerConfig.ACKS_CONFIG, acks);
    properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, idempotence);
    properties.put(ProducerConfig.RETRIES_CONFIG, retries);
    properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxRequestsInFlight);
    properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeout);
    properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeout);
    properties.put(ProducerConfig.LINGER_MS_CONFIG, linger);
    return new DefaultKafkaProducerFactory<>(properties);
  }

  @Bean
  public ConsumerFactory<String, Object> consumerFactory() {
    final Map<String, Object> properties = new HashMap<>();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
    properties.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
    return new DefaultKafkaConsumerFactory<>(properties);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
      final ConsumerFactory<String, Object> consumerFactory,
      final KafkaTemplate<String, Object> kafkaTemplate) {
    final var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
    factory.setConsumerFactory(consumerFactory);
    factory.getContainerProperties().setAckMode(AckMode.RECORD);
    factory.setCommonErrorHandler(
        new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(2000L, retries)));
    return factory;
  }
}
