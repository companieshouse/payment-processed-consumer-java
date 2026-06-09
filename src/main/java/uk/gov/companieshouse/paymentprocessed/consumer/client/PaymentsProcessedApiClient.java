package uk.gov.companieshouse.paymentprocessed.consumer.client;

import static uk.gov.companieshouse.paymentprocessed.consumer.Application.NAMESPACE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.payment.PaymentPatchRequestApi;
import uk.gov.companieshouse.api.model.payment.PaymentResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.logging.DataMapHolder;

@Component
public class PaymentsProcessedApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";
    private static final String GET_PAYMENT_CALL = "GET Payment";
    private static final String PATCH_PAYMENT_CALL = "Patch Payment";
    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String paymentsApiUrl;
    private final String skipGoneResourceId;
    private final Boolean skipGoneResource;

    PaymentsProcessedApiClient(Supplier<InternalApiClient> internalApiClientFactory, ResponseHandler responseHandler,
            ObjectMapper objectMapper, RestClient restClient, @Value("${payments.api.url}")
            String paymentsApiUrl,
            @Value("${skip.gone.resource.id}")
            String skipGoneResourceId,
            @Value("${skip.gone.resource}")
            Boolean skipGoneResource) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.objectMapper = objectMapper;
        this.responseHandler = responseHandler;
        this.restClient = restClient;
        this.paymentsApiUrl = paymentsApiUrl;
        this.skipGoneResourceId = skipGoneResourceId;
        this.skipGoneResource = skipGoneResource;
    }

    public Optional<PaymentResponse> getPayment(String resourceID) {
        InternalApiClient apiClient = internalApiClientFactory.get();
        apiClient.setBasePaymentsPath(paymentsApiUrl);
        apiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());
        String resourceUri = String.format("/payments/%s", resourceID);
        Optional<PaymentResponse> response = Optional.empty();
        LOGGER.info(String.format("Initiating %s resource ID: %s and resource URI: %s", GET_PAYMENT_CALL, resourceID,
                resourceUri));
        try {
            response = Optional.ofNullable(apiClient.privatePayment().getPaymentSession(resourceUri)
                    .execute()
                    .getData());
            loggingPaymentResponse(resourceID, response);
        } catch (ApiErrorResponseException ex) {
            LOGGER.error("Error response calling %s".formatted(GET_PAYMENT_CALL), ex, DataMapHolder.getLogMap());
            if (ex.getStatusCode() == HttpStatus.GONE.value() && checkSkipGoneResource(resourceID, skipGoneResource)) {
                return Optional.empty();
            }
            responseHandler.handle(GET_PAYMENT_CALL, resourceID, ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(GET_PAYMENT_CALL, ex);
        }
        return response;
    }

    private void loggingPaymentResponse(String resourceID, Optional<PaymentResponse> response) {
        if (response.isPresent()) {
            String jsonResponse;
            try {
                jsonResponse = objectMapper.writeValueAsString(response.get());
                LOGGER.info(
                        String.format("Successfully called %s for resource ID: %s and response: %s", GET_PAYMENT_CALL,
                                resourceID, jsonResponse));
            } catch (JsonProcessingException ex) {
                responseHandler.handle(GET_PAYMENT_CALL, resourceID, ex);
            }
        }
    }

    public void patchPayment(String paymentsPatchUri, PaymentPatchRequestApi paymentPatchRequestApi) {
        try {
            loggingRequestValue(paymentsPatchUri, paymentPatchRequestApi);
            String requestId = DataMapHolder.getRequestId();
            ResponseEntity<Void> response = restClient.patch()
                    .uri(paymentsPatchUri)
                    .contentType(MediaType.valueOf(APPLICATION_MERGE_PATCH_JSON))
                    .body(paymentPatchRequestApi)
                    .headers(headers -> {
                        if (requestId != null && !requestId.trim().isEmpty()) {
                            headers.add("x-request-id", requestId);
                        }
                    })
                    .retrieve()
                    .toBodilessEntity();
            if (response.getStatusCode().value() == HttpStatus.OK.value()) {
                LOGGER.info(String.format("Successfully called %s for resource URI: %s and status code: %s",
                                PATCH_PAYMENT_CALL, paymentsPatchUri, response.getStatusCode().value()),
                        DataMapHolder.getLogMap());
            }
        } catch (RestClientResponseException ex) {
            responseHandler.handle(PATCH_PAYMENT_CALL, paymentsPatchUri, ex);
        } catch (Exception ex) {
            String defaultErrorMessage = "Error response calling %s".formatted(PATCH_PAYMENT_CALL);
            LOGGER.error(defaultErrorMessage, ex, DataMapHolder.getLogMap());
            throw new RetryableException(defaultErrorMessage, ex);
        }
    }

    private void loggingRequestValue(String paymentsPatchUri, PaymentPatchRequestApi paymentPatchRequestApi) {
        try {
            LOGGER.debug(String.format("Initiating PATCH request for resource URI: %s and request %s", paymentsPatchUri,
                    objectMapper.writeValueAsString(paymentPatchRequestApi)), DataMapHolder.getLogMap());
        } catch (JsonProcessingException ex) {
            responseHandler.handle(PATCH_PAYMENT_CALL, paymentsPatchUri, ex);
        }
    }

    public boolean checkSkipGoneResource(String paymentId, boolean skipGoneResource) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("payment_id", paymentId);

        if (skipGoneResource) {
            LOGGER.info(String.format(
                    "SKIP_GONE_RESOURCE is true - checking if message should be skipped for Payment ID [%s]",
                    paymentId), logData);
            if (skipGoneResourceId != null && !skipGoneResourceId.isEmpty() && !skipGoneResourceId.equals(paymentId)) {
                LOGGER.info(String.format(
                        "SKIP_GONE_RESOURCE_ID [%s] does not match Payment ID [%s] - not skipping message",
                        skipGoneResourceId, paymentId), logData);
                return false;
            }
            LOGGER.info(String.format("Message for Payment ID [%s] meets criteria and will be skipped", paymentId),
                    logData);
            return true;
        }
        LOGGER.info(String.format("SKIP_GONE_RESOURCE is false - not skipping message for Payment ID [%s]", paymentId),
                logData);
        return false;
    }

}
