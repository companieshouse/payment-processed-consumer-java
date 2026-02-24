package uk.gov.companieshouse.paymentprocessed.consumer.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.payment.PaymentPatchRequestApi;
import uk.gov.companieshouse.api.model.payment.PaymentResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.logging.DataMapHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static uk.gov.companieshouse.paymentprocessed.consumer.Application.NAMESPACE;

@Component
public class PaymentsProcessedApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";
    private static final String GET_PAYMENT_CALL = "GET Payment";
    private static final String PATCH_PAYMENT_CALL = "Patch Payment";
    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String paymentsApiUrl;
    private final String skipGoneResourceId;
    private final Boolean skipGoneResource;

    PaymentsProcessedApiClient(Supplier<InternalApiClient> internalApiClientFactory, ResponseHandler responseHandler, ObjectMapper objectMapper, WebClient webClient, @Value("${payments.api.url}")
    String paymentsApiUrl,
                               @Value("${skip.gone.resource.id}")
                               String skipGoneResourceId,
                               @Value("${skip.gone.resource}")
                               Boolean skipGoneResource) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.objectMapper = objectMapper;
        this.responseHandler = responseHandler;
        this.webClient = webClient;
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
        LOGGER.info(String.format("Initiating %s resource ID: %s and resource URI: %s", GET_PAYMENT_CALL, resourceID, resourceUri));
        try {
            response = Optional.ofNullable(apiClient.privatePayment().getPaymentSession(resourceUri)
                    .execute()
                    .getData());
            loggingPaymentResponse(resourceID, response);
        } catch (ApiErrorResponseException ex) {
            LOGGER.error(String.format("Unable to obtain response from %s for resource ID: %s", GET_PAYMENT_CALL, resourceID));
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
                LOGGER.info(String.format("Successfully called %s for resource ID: %s and response: %s", GET_PAYMENT_CALL, resourceID, jsonResponse));
            } catch (JsonProcessingException ex) {
                responseHandler.handle(GET_PAYMENT_CALL, resourceID, ex);
            }
        }
    }

    public void patchPayment(String paymentsPatchUri, PaymentPatchRequestApi paymentPatchRequestApi) {
        try {
            loggingRequestValue(paymentsPatchUri, paymentPatchRequestApi);
            webClient.patch()
                    .uri(paymentsPatchUri)
                    .contentType(MediaType.valueOf(APPLICATION_MERGE_PATCH_JSON))
                    .bodyValue(paymentPatchRequestApi)
                    .retrieve()
                    .toBodilessEntity()
                    .doOnSuccess(response -> LOGGER.info(String.format("Successfully called PATCH payment for resource URI: %s with status code: %s", paymentsPatchUri, response.getStatusCode()), DataMapHolder.getLogMap()))
                    .block();
        } catch (WebClientResponseException ex) {
            responseHandler.handle(PATCH_PAYMENT_CALL, paymentsPatchUri, ex);
        } catch (Exception ex) {
            String defaultErrorMessage = String.format("Unexpected error occurred during PATCH request for resource URI: %s with message %s", paymentsPatchUri, ex.getMessage());
            LOGGER.error(defaultErrorMessage, DataMapHolder.getLogMap());
            throw new RetryableException(defaultErrorMessage, ex);
        }
    }

    private void loggingRequestValue(String paymentsPatchUri, PaymentPatchRequestApi paymentPatchRequestApi) {
        try {
            LOGGER.debug(String.format("Initiating PATCH request for resource URI: %s and request %s", paymentsPatchUri, objectMapper.writeValueAsString(paymentPatchRequestApi)), DataMapHolder.getLogMap());
        } catch (JsonProcessingException ex) {
            responseHandler.handle(PATCH_PAYMENT_CALL, paymentsPatchUri, ex);
        }
    }

    public boolean checkSkipGoneResource(String paymentId, boolean skipGoneResource) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("payment_id", paymentId);

        if (skipGoneResource) {
            LOGGER.info(String.format("SKIP_GONE_RESOURCE is true - checking if message should be skipped for Payment ID [%s]", paymentId), logData);
            if (skipGoneResourceId != null && !skipGoneResourceId.isEmpty() && !skipGoneResourceId.equals(paymentId)) {
                LOGGER.info(String.format("SKIP_GONE_RESOURCE_ID [%s] does not match Payment ID [%s] - not skipping message", skipGoneResourceId, paymentId), logData);
                return false;
            }
            LOGGER.info(String.format("Message for Payment ID [%s] meets criteria and will be skipped", paymentId), logData);
            return true;
        }
        LOGGER.info(String.format("SKIP_GONE_RESOURCE is false - not skipping message for Payment ID [%s]", paymentId), logData);
        return false;
    }

}
