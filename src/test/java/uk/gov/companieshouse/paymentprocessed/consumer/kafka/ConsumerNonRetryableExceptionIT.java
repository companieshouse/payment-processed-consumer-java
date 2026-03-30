package uk.gov.companieshouse.paymentprocessed.consumer.kafka;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import payments.payment_processed;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.service.PaymentProcessedServiceRouter;
import uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ConsumerNonRetryableExceptionIT extends AbstractKafkaIT {

    @MockitoBean
    private PaymentProcessedServiceRouter paymentProcessedServiceRouter;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    @BeforeEach
    public void drainKafkaTopics() {
        if (!kafka.isRunning()) {
            throw new IllegalStateException("Kafka container is not running!");
        }
    }

    @Test
    void testRepublishToPaymentProcessedInvalidMessageTopicIfNonRetryableExceptionThrown() throws Exception {
        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<payment_processed> writer = new ReflectDatumWriter<>(payment_processed.class);

        payment_processed paymentProcessed = TestUtils.getPaymentProcessed();

        writer.write(paymentProcessed, encoder);
        doThrow(NonRetryableException.class).when(paymentProcessedServiceRouter).route(any());

        // when
        testProducer.send(new ProducerRecord<>(AbstractKafkaIT.CONSUMER_MAIN_TOPIC, 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        if (!testConsumerAspect.getLatch().await(5L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        // then
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 2);
        assertThat(AbstractKafkaIT.recordsPerTopic(consumerRecords, AbstractKafkaIT.CONSUMER_MAIN_TOPIC)).isOne();
        assertThat(AbstractKafkaIT.recordsPerTopic(consumerRecords, AbstractKafkaIT.CONSUMER_RETRY_TOPIC)).isZero();
        assertThat(AbstractKafkaIT.recordsPerTopic(consumerRecords, AbstractKafkaIT.CONSUMER_ERROR_TOPIC)).isZero();
        assertThat(AbstractKafkaIT.recordsPerTopic(consumerRecords, AbstractKafkaIT.CONSUMER_INVALID_TOPIC)).isOne();
        verify(paymentProcessedServiceRouter).route(any());
    }

}
