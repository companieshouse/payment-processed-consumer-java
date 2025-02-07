package uk.gov.companieshouse.payment_processed_consumer.kafka;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.messaging.Message;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import payments.payment_processed;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.payment_processed_consumer.exception.RetryableException;
import uk.gov.companieshouse.payment_processed_consumer.service.Service;
import uk.gov.companieshouse.payment_processed_consumer.service.ServiceParameters;


@Component
public class Consumer {

    private final Service service;
    private final MessageFlags messageFlags;

    public Consumer(Service service, MessageFlags messageFlags) {
        this.service = service;
        this.messageFlags = messageFlags;
    }

    /**
     * Consume a message from the main Kafka topic.
     *
     * @param message A message containing a payload.
     */
    @KafkaListener(
            containerFactory = "kafkaListenerContainerFactory",
            topics = "${payment.processed.topic}",
            autoStartup = "true"
    )
    @RetryableTopic(
            attempts = "${maximum.retry.attempts}",
            autoCreateTopics = "false",
            backoff = @Backoff(delayExpression = "${consumer.backoff_delay}"),
            dltTopicSuffix = "-error",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = RetryableException.class
    )

    public void consume(Message<payment_processed> message){
        try{
            service.processMessage(new ServiceParameters(message.getPayload()));
        } catch (RetryableException | ApiErrorResponseException e){
            messageFlags.setRetryable(true);
        }
    }

}
