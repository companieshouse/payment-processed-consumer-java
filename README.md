# payment-processed-consumer-java

The Payment-Processed-Consumer-Java is designed to process payment-related messages from a Kafka topic. It ensures that
payment events are extracted and patched to several types of payments while adhering to security and business rules.
This document provides a detailed design of the service, focusing on its architecture, components, and business logic.

## Architecture

### Key Components

1. **Kafka Consumer**:
    - Listens to a configured topic for `payment-processed` messages using Spring Kafka.
    - Supports retry and error topics for resilience.

2. **Kafka Producer**:
    - Used for publishing messages to retry or error topics in case of failures.

3. **Payments API Integration**:
    - Fetches payment session details using `private-api-sk' library.
    - Uses SPRING 'Webclient' to Patch request.

4. **Resilience/Retry Handler**:
    - Manages transient errors and retries using Retry Topic and if still fails maintain a Error Dead Letter Topic.

---

## Sequence Diagram

The following sequence diagram illustrates the message processing flow:
[!Sequence Diagram](docs/design/sequence.png)

## Message Processing Flow

### 1. Message Consumption

- The service consumes messages from the Kafka topic using Spring Kafka's `@KafkaListener` annotation.

### 3. Deserialize Message

- The message is deserialized from Avro format into a `paymentProcessed` Java object using a schema registry.
- **Error Handling**: If deserialization fails, the error is logged, and the message is sent to the error topic.

### 4. Fetch Payment Session

- The service calls the Payments API to retrieve payment session details using the `ResourceURI` from the message.
- **Error Handling**: If the API call fails:
    - if Response return Bad Request and Conflict then its Non-retryable.
    - The service checks if the resource is "gone" and skips the message if configured.
    - Otherwise, the error is logged, and the message is sent to the retry topic.

### 5. Patching Request

- The service check if the payment details from payment session response is of refund type or other type of Payments
- refund then create a refund request atch it as refundRequest
- Otherwise Proceed to Patch it as patchRequest.
- **Error Handling**: If the API call fails:
    - if Response return Bad Request and Conflict then its Non-retryable.
    - The service checks if the resource is "gone" and skips the message if configured.
    - Otherwise, the error is logged, and the message is sent to the retry topic.

---

### Error Handling

- **Deserialization Errors**:
    - Logged and sent to the error topic.
- **API Call Failures**:
    - If the resource is "gone", the message is skipped based on configuration.
    - Other errors are logged and sent to the retry topic.
- **RetryableException**:
    - Includes HTTP responses (excluding 400 & 409) and deserialization errors like `InvalidPayloadException`.
- **NonRetryableException**:
    - Includes 400 and 409 HTTP responses, and URI validation exceptions.

## Configuration

### Kafka Topics

- **Standard Topic**: For normal message processing.
- **Retry Topic**: For transient errors.
- **Error Topic**: For unrecoverable errors.

## Handling 410 (Gone) Resources

The `payment-processed-consumer-java` contains handling for situations where a 410 (Gone) status code is returned by the
Payments API - with three scenarios available:

* Skip all messages where a 410 (Gone) status code is received
* Do not skip any messages where a 410 (Gone) status code is received
* Only skip messages which relate to a given payment ID, where a 410 (Gone) status code is received

These scenarios can be configured via the `SKIP_GONE_RESOURCE` and `SKIP_GONE_RESOURCE_ID` environment variables with
the following configurations.

* `SKIP_GONE_RESOURCE=true` and `SKIP_GONE_RESOURCE_ID` is unset - skip all messages.
* `SKIP_GONE_RESOURCE=false` - do not skip any messages - the value of `SKIP_GONE_RESOURCE_ID` is ignored if one is set.
* `SKIP_GONE_RESOURCE=true` and `SKIP_GONE_RESOURCE_ID=<payment_id>` - only skip messages which receive a 410 gone and
  match the given payment id.

## Docker support

Pull image from private CH registry by
running `docker pull 416670754337.dkr.ecr.eu-west-2.amazonaws.com/payment-processed-consumer-java:latest` command
or run the following steps to build image locally:

1. `export SSH_PRIVATE_KEY_PASSPHRASE='[your SSH key passhprase goes here]'` (optional, set only if SSH key is
   passphrase protected)
2. `DOCKER_BUILDKIT=0 docker build --build-arg SSH_PRIVATE_KEY="$(cat ~/.ssh/id_rsa)" --build-arg SSH_PRIVATE_KEY_PASSPHRASE -t 416670754337.dkr.ecr.eu-west-2.amazonaws.com//payment-processed-consumer-java:latest .`

## Configuration

### Kafka Topics

- **Standard Topic**: For normal message processing.
- **Retry Topic**: For transient errors.
- **Error Topic**: For unrecoverable errors.

### Environment Variables

| Variable                      | Description                                                                          | Example                           |
|-------------------------------|--------------------------------------------------------------------------------------|-----------------------------------|
| CHS_API_KEY                   | The client ID of an API key, with internal app privileges, to call payments-api with | abc123def456ghi789                |
| KAFKA_BROKER_ADDR             | The URL to the kafka broker                                                          | kafka:9092                        |
| CONCURRENT_LISTENER_INSTANCES | The number of listeners run in parallel for the consumer                             | 1                                 |
| PAYMENT_PROCESSED_TOPIC       | The topic ID for refund request topic                                                | payment-processed.                |
| PAYMENT_PROCESSED_GROUP_NAME  | The group ID for the service's Kafka topics                                          | payment-processed-consumer-group. |
| MAXIMUM_RETRY_ATTEMPTS        | The number of times a message will be retried before being moved to the error topic  | 2                                 |
| BACKOFF_DELAY                 | The incremental time delay between message retries                                   | 60 (seconds)                      |
| LOGLEVEL                      | The level of log messages output to the logs                                         | debug                             |
| PORT                          | The port at which the service is hosted in ECS                                       | 8080                              |
| SKIP_GONE_RESOURCE            | To Skip a gone resource or not                                                       | true                              |
| SKIP_GONE_RESOURCE_ID         | Id of payment resource to be skipped                                                 | 1234                              
| TIMEOUT_MILLISECONDS          | Amount of time maximum to wait for request to be completed                           | 6000                              
| PAYMENTS_API_URL              | payments url to be use as base path                                                  | 1234                              