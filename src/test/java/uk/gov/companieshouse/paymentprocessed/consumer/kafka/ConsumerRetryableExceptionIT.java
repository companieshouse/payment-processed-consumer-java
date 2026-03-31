package uk.gov.companieshouse.paymentprocessed.consumer.kafka;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import payments.payment_processed;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.service.PaymentProcessedServiceRouter;
import uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class ConsumerRetryableExceptionIT extends AbstractKafkaIT {

    @MockitoBean
    private PaymentProcessedServiceRouter paymentProcessedServiceRouter;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 5);
    }


    @Test
    void testRepublishToPaymentProcessedErrorTopicThroughRetryTopics() throws Exception {
        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<payment_processed> writer = new ReflectDatumWriter<>(payment_processed.class);
        writer.write(TestUtils.getPaymentProcessed(), encoder);

        doThrow(new RetryableException("Retryable exception", new Throwable())).when(paymentProcessedServiceRouter).route(any());

        // when
        testProducer.send(new ProducerRecord<>(AbstractKafkaIT.CONSUMER_MAIN_TOPIC, 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        if (!testConsumerAspect.getLatch().await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        // then
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 6);
        assertThat(AbstractKafkaIT.recordsPerTopic(consumerRecords, AbstractKafkaIT.CONSUMER_MAIN_TOPIC)).isOne();
        assertThat(AbstractKafkaIT.recordsPerTopic(consumerRecords, AbstractKafkaIT.CONSUMER_RETRY_TOPIC)).isEqualTo(4);
        assertThat(AbstractKafkaIT.recordsPerTopic(consumerRecords, AbstractKafkaIT.CONSUMER_ERROR_TOPIC)).isOne();
        assertThat(AbstractKafkaIT.recordsPerTopic(consumerRecords, AbstractKafkaIT.CONSUMER_INVALID_TOPIC)).isZero();
        WireMock.verify(0, anyRequestedFor(anyUrl()));
    }
}
