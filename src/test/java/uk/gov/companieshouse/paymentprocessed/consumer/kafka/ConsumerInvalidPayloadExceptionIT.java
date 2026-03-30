package uk.gov.companieshouse.paymentprocessed.consumer.kafka;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.companieshouse.paymentprocessed.consumer.service.PaymentProcessedServiceRouter;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConsumerInvalidPayloadExceptionIT extends AbstractKafkaIT {

    @MockitoBean
    private PaymentProcessedServiceRouter paymentProcessedServiceRouter;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    @BeforeEach
    public void setupTopicsAndDrain() {
        // Explicitly create topics before each test
        Properties props = new Properties();
        props.put("bootstrap.servers", kafka.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(props)) {
            adminClient.createTopics(Collections.singletonList(new NewTopic(AbstractKafkaIT.CONSUMER_MAIN_TOPIC, 1, (short) 1))).all().get();
            adminClient.createTopics(Collections.singletonList(new NewTopic(AbstractKafkaIT.CONSUMER_RETRY_TOPIC, 1, (short) 1))).all().get();
            adminClient.createTopics(Collections.singletonList(new NewTopic(AbstractKafkaIT.CONSUMER_ERROR_TOPIC, 1, (short) 1))).all().get();
            adminClient.createTopics(Collections.singletonList(new NewTopic(AbstractKafkaIT.CONSUMER_INVALID_TOPIC, 1, (short) 1))).all().get();
        } catch (Exception e) {
            // Topics probably already exist, ignore
        }
    }

    @Test
    void testPublishToPaymentProcessedInvalidMessageTopicIfInvalidDataDeserialised() throws Exception {
        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new ReflectDatumWriter<>(String.class);
        writer.write("bad data", encoder);

        // when
        testProducer.send(new ProducerRecord<>(AbstractKafkaIT.CONSUMER_MAIN_TOPIC, 0, System.currentTimeMillis(),
                "key", outputStream.toByteArray()));

        // then
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 2);
        assertThat(AbstractKafkaIT.recordsPerTopic(consumerRecords, AbstractKafkaIT.CONSUMER_MAIN_TOPIC)).isOne();
        assertThat(AbstractKafkaIT.recordsPerTopic(consumerRecords, AbstractKafkaIT.CONSUMER_RETRY_TOPIC)).isZero();
        assertThat(AbstractKafkaIT.recordsPerTopic(consumerRecords, AbstractKafkaIT.CONSUMER_ERROR_TOPIC)).isZero();
        assertThat(AbstractKafkaIT.recordsPerTopic(consumerRecords, AbstractKafkaIT.CONSUMER_INVALID_TOPIC)).isOne();
    }
}
