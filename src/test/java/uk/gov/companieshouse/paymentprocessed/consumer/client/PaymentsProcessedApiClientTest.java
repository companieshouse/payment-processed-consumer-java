package uk.gov.companieshouse.paymentprocessed.consumer.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.payments.PrivatePaymentResourceHandler;
import uk.gov.companieshouse.api.handler.payments.request.PaymentGetPaymentSession;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.payment.PaymentPatchRequestApi;
import uk.gov.companieshouse.api.model.payment.PaymentResponse;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils;

import java.util.Optional;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils.getPaymentPatchRequestApi;
import static uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils.getPaymentResponse;

@ExtendWith(MockitoExtension.class)
public class PaymentsProcessedApiClientTest {
    private static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";
    private static final String URL = "http://example.com/payments";
    @Mock
    private Supplier<InternalApiClient> internalApiClientFactory;
    @Mock
    private InternalApiClient internalApiClient;
    @Mock
    private PrivatePaymentResourceHandler privatePaymentResourceHandler;
    @Mock
    HttpClient httpClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PaymentGetPaymentSession paymentGetPaymentSession;
    @Mock
    private ResponseHandler responseHandler;
    @Mock
    private WebClient webClient;
    @InjectMocks
    private PaymentsProcessedApiClient paymentsProcessedApiClient;
    private static final String RESOURCE_ID = "P9hl8PWQQrKRBk1Zmc";

    @Test
    void shouldSendSuccessfulGetRequest() throws Exception {
        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privatePayment()).thenReturn(privatePaymentResourceHandler);
        when(privatePaymentResourceHandler.getPaymentSession(anyString())).thenReturn(paymentGetPaymentSession);
        when(paymentGetPaymentSession.execute()).thenReturn(getPaymentResponse());
        Optional<PaymentResponse> paymentResponse = paymentsProcessedApiClient.getPayment(RESOURCE_ID);
        Assertions.assertTrue(paymentResponse.isPresent());
        Assertions.assertEquals(TestUtils.RESOURCE_LINK, paymentResponse.get().getLinks().getResource());
        verify(privatePaymentResourceHandler, times(1)).getPaymentSession("/payments/" + RESOURCE_ID);
    }

    @Test
    void shouldSendSuccessfulPatchRequest() {
        String paymentsPatchUri = URL;
        PaymentPatchRequestApi paymentPatchRequestApi = new PaymentPatchRequestApi();
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        Mono<ResponseEntity<Void>> responseEntityMono = mock(Mono.class);
        // Mocking the WebClient chain
        when(webClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(requestBodyUriSpec.uri(paymentsPatchUri)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.valueOf(APPLICATION_MERGE_PATCH_JSON))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(paymentPatchRequestApi)).thenReturn(requestHeadersSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(responseEntityMono);
        when(responseEntityMono.doOnSuccess(any())).thenReturn(responseEntityMono);

        paymentsProcessedApiClient.patchPayment(paymentsPatchUri, paymentPatchRequestApi);

        verify(webClient, times(1)).patch();
        verify(requestBodyUriSpec, times(1)).uri(paymentsPatchUri);
        verify(requestBodySpec, times(1)).contentType(MediaType.valueOf(APPLICATION_MERGE_PATCH_JSON));
        verify(requestBodySpec, times(1)).bodyValue(paymentPatchRequestApi);
    }


    @Test
    void shouldHandleApiErrorExceptionWhenSendingGetRequest() throws Exception {
        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privatePayment()).thenReturn(privatePaymentResourceHandler);
        Class<ApiErrorResponseException> exceptionClass = ApiErrorResponseException.class;
        when(privatePaymentResourceHandler.getPaymentSession(anyString())).thenReturn(paymentGetPaymentSession);
        when(paymentGetPaymentSession.execute()).thenThrow(ApiErrorResponseException.class);
        paymentsProcessedApiClient.getPayment(RESOURCE_ID);
        verify(responseHandler).handle(anyString(), anyString(), any(exceptionClass));
        verify(privatePaymentResourceHandler, times(1)).getPaymentSession("/payments/" + RESOURCE_ID);
    }

    @Test
    void shouldHandleRetryableExceptionWhenSendingPatchRequest() throws Exception {
        // Arrange
        String paymentsPatchUri = URL;
        PaymentPatchRequestApi paymentPatchRequestApi = getPaymentPatchRequestApi();
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);

        // Mocking the WebClient chain to simulate an error response
        when(webClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(paymentsPatchUri)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.valueOf(APPLICATION_MERGE_PATCH_JSON))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(paymentPatchRequestApi)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenThrow(new RetryableException("Payments Consumer API Patch Payment failed"));

        // Act & Assert
        RetryableException exception = Assertions.assertThrows(RetryableException.class, () ->
                paymentsProcessedApiClient.patchPayment(paymentsPatchUri, paymentPatchRequestApi)
        );

        // Verify
        Assertions.assertTrue(exception.getMessage().contains("Payments Consumer API Patch Payment failed"));
        verify(webClient, times(1)).patch();
        verify(requestBodyUriSpec, times(1)).uri(paymentsPatchUri);
        verify(requestBodySpec, times(1)).contentType(MediaType.valueOf(APPLICATION_MERGE_PATCH_JSON));
        verify(requestBodySpec, times(1)).bodyValue(paymentPatchRequestApi);
    }

    @Test
    void shouldHandleNonRetryableExceptionWhenSendingPatchRequest() throws Exception {
        // Arrange
        String paymentsPatchUri = URL;
        PaymentPatchRequestApi paymentPatchRequestApi = getPaymentPatchRequestApi();
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);

        // Mocking the WebClient chain to simulate an error response
        when(webClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(paymentsPatchUri)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.valueOf(APPLICATION_MERGE_PATCH_JSON))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(paymentPatchRequestApi)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenThrow(new WebClientResponseException(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null,
                null,
                null
        ));
        doThrow(new NonRetryableException("Mocked NonRetryableException"))
                .when(responseHandler)
                .handle(anyString(), anyString(), any(WebClientResponseException.class));


        // Act & Assert
        NonRetryableException exception = Assertions.assertThrows(NonRetryableException.class, () ->
                paymentsProcessedApiClient.patchPayment(paymentsPatchUri, paymentPatchRequestApi)
        );

        // Verify
        Assertions.assertTrue(exception.getMessage().contains("Mocked NonRetryableException"));
        verify(webClient, times(1)).patch();
        verify(requestBodyUriSpec, times(1)).uri(paymentsPatchUri);
        verify(requestBodySpec, times(1)).contentType(MediaType.valueOf(APPLICATION_MERGE_PATCH_JSON));
        verify(requestBodySpec, times(1)).bodyValue(paymentPatchRequestApi);
    }

    @Test
    void shouldHandleURIValidationExceptionWhenSendingGetRequest() throws Exception {
        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privatePayment()).thenReturn(privatePaymentResourceHandler);
        Class<URIValidationException> uriValidationException = URIValidationException.class;
        when(privatePaymentResourceHandler.getPaymentSession(anyString())).thenReturn(paymentGetPaymentSession);
        when(paymentGetPaymentSession.execute()).thenThrow(uriValidationException);
        paymentsProcessedApiClient.getPayment(RESOURCE_ID);
        verify(responseHandler).handle(anyString(), any(uriValidationException));
        verify(privatePaymentResourceHandler, times(1)).getPaymentSession("/payments/" + RESOURCE_ID);
    }

    @Test
    void shouldHandleJsonProcessingExceptionWhenSendingGetRequest() throws Exception {
        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privatePayment()).thenReturn(privatePaymentResourceHandler);
        when(privatePaymentResourceHandler.getPaymentSession(anyString())).thenReturn(paymentGetPaymentSession);
        when(paymentGetPaymentSession.execute()).thenReturn(getPaymentResponse());
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        Optional<PaymentResponse> response = paymentsProcessedApiClient.getPayment(RESOURCE_ID);
        // Assert
        Assertions.assertTrue(response.isPresent());
    }

    @Test
    void shouldHandleGoneResourceWhenSendingGetRequest() throws Exception {
        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privatePayment()).thenReturn(privatePaymentResourceHandler);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(
                HttpStatus.GONE.value(),
                "Resource Gone",
                new HttpHeaders() // Ensure headers are not null
        );
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(builder);
        when(privatePaymentResourceHandler.getPaymentSession(anyString())).thenReturn(paymentGetPaymentSession);
        when(paymentGetPaymentSession.execute()).thenThrow(apiErrorResponseException);
        ReflectionTestUtils.setField(paymentsProcessedApiClient, "skipGoneResource", true
        );
        ReflectionTestUtils.setField(paymentsProcessedApiClient, "skipGoneResourceId", RESOURCE_ID
        );
        paymentsProcessedApiClient.getPayment(RESOURCE_ID);
        verify(privatePaymentResourceHandler, times(1)).getPaymentSession("/payments/" + RESOURCE_ID);
    }

    @Test
    void shouldHandleGoneResourceDifferentPaymentIdWhenSendingGetRequest() throws Exception {
        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privatePayment()).thenReturn(privatePaymentResourceHandler);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(
                HttpStatus.GONE.value(),
                "Resource Gone",
                new HttpHeaders() // Ensure headers are not null
        );
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(builder);
        when(privatePaymentResourceHandler.getPaymentSession(anyString())).thenReturn(paymentGetPaymentSession);
        when(paymentGetPaymentSession.execute()).thenThrow(apiErrorResponseException);
        ReflectionTestUtils.setField(paymentsProcessedApiClient, "skipGoneResource", true
        );
        ReflectionTestUtils.setField(paymentsProcessedApiClient, "skipGoneResourceId", RESOURCE_ID);
        PaymentsProcessedApiClient spyClient = spy(paymentsProcessedApiClient);
        spyClient.getPayment(RESOURCE_ID + "1");
        verify(spyClient, times(1)).checkSkipGoneResource(RESOURCE_ID + "1", true);
        Assertions.assertFalse(spyClient.checkSkipGoneResource(RESOURCE_ID + "1", true), "Expected checkSkipGoneResource to return false");
    }

    @Test
    void shouldThrowRetryableExceptionForAnyNonWebclientExceptionPaymentsPatchUri() {
        // Arrange
        String invalidPaymentsPatchUri = "invalid_uri";
        PaymentPatchRequestApi paymentPatchRequestApi = mock(PaymentPatchRequestApi.class);

        // Mocking the WebClient chain to simulate an error response
        when(webClient.patch()).thenThrow(new IllegalArgumentException("Invalid URI"));

        // Act & Assert
        Assertions.assertThrows(RetryableException.class, () ->
                paymentsProcessedApiClient.patchPayment(invalidPaymentsPatchUri, paymentPatchRequestApi)
        );
    }


    public static <T> ApiResponse<T> getAPIResponse(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), null, data);
    }
}
