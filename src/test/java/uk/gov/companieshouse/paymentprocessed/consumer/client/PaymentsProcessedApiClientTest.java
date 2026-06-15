package uk.gov.companieshouse.paymentprocessed.consumer.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.payment.PaymentPatchRequestApi;
import uk.gov.companieshouse.api.model.payment.PaymentResponse;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.logging.DataMapHolder;
import uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentsProcessedApiClientTest {

    private MockRestServiceServer server;

    private org.springframework.web.client.RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        this.restClient = builder.build();
    }

    @Test
    void shouldSendSuccessfulGetRequest() throws Exception {
        InternalApiClient mockInternal = mock(InternalApiClient.class, RETURNS_DEEP_STUBS);
        Supplier<InternalApiClient> supplier = () -> mockInternal;

        PaymentResponse expected = TestUtils.getPaymentResponse().getData();
        when(mockInternal.privatePayment().getPaymentSession("/payments/123").execute().getData())
                .thenReturn(expected);

        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(supplier,
                mock(ResponseHandler.class), new ObjectMapper(), null,
                "http://payments", null, false);

        clearInvocations(mockInternal.privatePayment());
        Optional<PaymentResponse> result = client.getPayment("123");
        assertThat(result).isPresent().contains(expected);
        assertThat(result.get().getLinks().getResource()).isEqualTo(TestUtils.RESOURCE_LINK);
        verify(mockInternal.privatePayment(), times(1)).getPaymentSession("/payments/123");
    }

    @Test
    void shouldSendSuccessfulPatchRequest() {

        server.expect(requestTo("http://localhost:8080/payments/1"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("Content-Type", "application/merge-patch+json"))
                .andRespond(withStatus(HttpStatus.OK));

        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(null,
                mock(ResponseHandler.class), configuredMapper(), restClient,
                "http://payments", null, false);

        client.patchPayment("http://localhost:8080/payments/1", new PaymentPatchRequestApi());
        server.verify();
    }

    @Test
    void shouldHandleApiErrorExceptionWhenSendingGetRequest() throws Exception {
        InternalApiClient mockInternal = mock(InternalApiClient.class, RETURNS_DEEP_STUBS);
        Supplier<InternalApiClient> supplier = () -> mockInternal;

        ApiErrorResponseException apiEx = mock(ApiErrorResponseException.class);
        when(apiEx.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST.value());

        when(mockInternal.privatePayment().getPaymentSession("/payments/123").execute()).thenThrow(apiEx);

        ResponseHandler handler = mock(ResponseHandler.class);
        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(supplier,
                handler, new ObjectMapper(), null,
                "http://payments", null, false);

        clearInvocations(mockInternal.privatePayment());
        client.getPayment("123");

        verify(handler).handle("GET Payment", "123", apiEx);
        verify(mockInternal.privatePayment(), times(1)).getPaymentSession("/payments/123");
    }

    @Test
    void shouldHandleURIValidationExceptionWhenSendingGetRequest() throws Exception {
        InternalApiClient mockInternal = mock(InternalApiClient.class, RETURNS_DEEP_STUBS);
        Supplier<InternalApiClient> supplier = () -> mockInternal;

        URIValidationException uriEx = mock(URIValidationException.class);
        when(mockInternal.privatePayment().getPaymentSession("/payments/123").execute()).thenThrow(uriEx);

        ResponseHandler handler = mock(ResponseHandler.class);
        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(supplier,
                handler, new ObjectMapper(), null,
                "http://payments", null, false);

        clearInvocations(mockInternal.privatePayment());
        client.getPayment("123");

        verify(handler).handle("GET Payment", uriEx);
        verify(mockInternal.privatePayment(), times(1)).getPaymentSession("/payments/123");
    }

    @Test
    void shouldHandleJsonProcessingExceptionWhenSendingGetRequest() throws Exception {
        InternalApiClient mockInternal = mock(InternalApiClient.class, RETURNS_DEEP_STUBS);
        Supplier<InternalApiClient> supplier = () -> mockInternal;

        PaymentResponse resp = new PaymentResponse();
        when(mockInternal.privatePayment().getPaymentSession("/payments/123").execute().getData())
                .thenReturn(resp);

        ObjectMapper mockMapper = mock(ObjectMapper.class);
        JsonProcessingException jsonEx = mock(JsonProcessingException.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(jsonEx);

        ResponseHandler handler = mock(ResponseHandler.class);
        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(supplier,
                handler, mockMapper, null,
                "http://payments", null, false);

        clearInvocations(mockInternal.privatePayment());
        Optional<PaymentResponse> result = client.getPayment("123");

        assertThat(result).isPresent().contains(resp);
        verify(handler).handle("GET Payment", "123", jsonEx);
        verify(mockInternal.privatePayment(), times(1)).getPaymentSession("/payments/123");
    }

    @Test
    void shouldHandleGoneResourceWhenSendingGetRequest() throws Exception {
        InternalApiClient mockInternal = mock(InternalApiClient.class, RETURNS_DEEP_STUBS);
        Supplier<InternalApiClient> supplier = () -> mockInternal;

        ApiErrorResponseException goneEx = mock(ApiErrorResponseException.class);
        when(goneEx.getStatusCode()).thenReturn(HttpStatus.GONE.value());
        when(mockInternal.privatePayment().getPaymentSession("/payments/123").execute()).thenThrow(goneEx);

        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(supplier,
                mock(ResponseHandler.class), new ObjectMapper(), null,
                "http://payments", "123", true);

        clearInvocations(mockInternal.privatePayment());
        Optional<PaymentResponse> result = client.getPayment("123");
        assertThat(result).isEmpty();
        verify(mockInternal.privatePayment(), times(1)).getPaymentSession("/payments/123");
    }

    @Test
    void shouldHandleGoneResourceDifferentPaymentIdWhenSendingGetRequest() throws Exception {
        InternalApiClient mockInternal = mock(InternalApiClient.class, RETURNS_DEEP_STUBS);
        Supplier<InternalApiClient> supplier = () -> mockInternal;

        ApiErrorResponseException goneEx = mock(ApiErrorResponseException.class);
        when(goneEx.getStatusCode()).thenReturn(HttpStatus.GONE.value());
        when(mockInternal.privatePayment().getPaymentSession("/payments/123").execute()).thenThrow(goneEx);

        ResponseHandler handler = mock(ResponseHandler.class);
        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(supplier,
                handler, new ObjectMapper(), null,
                "http://payments", "DIFFERENT", true);

        clearInvocations(mockInternal.privatePayment());
        Optional<PaymentResponse> result = client.getPayment("123");
        assertThat(result).isEmpty();
        verify(handler).handle("GET Payment", "123", goneEx);
        verify(mockInternal.privatePayment(), times(1)).getPaymentSession("/payments/123");
    }

    @Test
    void shouldHandleRetryableExceptionWhenSendingPatchRequest() {
        RestClient mockRest = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(mockRest.patch()
                .uri(anyString())
                .contentType(any())
                .body(any(PaymentPatchRequestApi.class))
                .headers(any())
                .retrieve()
                .toBodilessEntity()).thenThrow(new RuntimeException("boom"));

        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(null,
                mock(ResponseHandler.class), configuredMapper(), mockRest,
                "http://payments", null, false);

        uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException exception =
                assertThrows(uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException.class,
                        () -> client.patchPayment("http://localhost:8080/payments/1", new PaymentPatchRequestApi()));
        assertThat(exception).hasMessageContaining("Error response calling Patch Payment");
    }

    @Test
    void shouldThrowRetryableExceptionForAnyNonRestClientExceptionPaymentsPatchUri() {
        RestClient mockRest = mock(RestClient.class);
        when(mockRest.patch()).thenThrow(new RuntimeException("boom"));

        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(null,
                mock(ResponseHandler.class), configuredMapper(), mockRest,
                "http://payments", null, false);

        assertThrows(uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException.class,
                () -> client.patchPayment("/payments/1", new PaymentPatchRequestApi()));
    }

    @Test
    void shouldHandleNonRetryableExceptionWhenSendingPatchRequest() {
        server.expect(requestTo("http://localhost:8080/payments/1"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("Content-Type", "application/merge-patch+json"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseHandler handler = mock(ResponseHandler.class);
        doThrow(new NonRetryableException("Mocked NonRetryableException"))
                .when(handler)
                .handle(eq("Patch Payment"),
                        eq("http://localhost:8080/payments/1"),
                        org.mockito.ArgumentMatchers.any(RestClientResponseException.class));

        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(null,
                handler, configuredMapper(), restClient,
                "http://payments", null, false);

        PaymentPatchRequestApi patchRequest = new PaymentPatchRequestApi();
        NonRetryableException exception = assertThrows(NonRetryableException.class,
                () -> client.patchPayment("http://localhost:8080/payments/1", patchRequest));
        assertThat(exception).hasMessageContaining("Mocked NonRetryableException");

        server.verify();
        verify(handler).handle(eq("Patch Payment"),
                eq("http://localhost:8080/payments/1"),
                any(RestClientResponseException.class));
    }

    @Test
    void shouldAddXRequestIdHeaderWhenRequestIdIsValid() {
        server.expect(requestTo("http://localhost:8080/payments/1"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("x-request-id", "req-1"))
                .andRespond(withStatus(HttpStatus.OK));

        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(null,
                mock(ResponseHandler.class), configuredMapper(), restClient,
                "http://payments", null, false);

        uk.gov.companieshouse.paymentprocessed.consumer.logging.DataMapHolder.initialise("req-1");

        client.patchPayment("http://localhost:8080/payments/1", new PaymentPatchRequestApi());

        server.verify();
        DataMapHolder.clear();
    }

    @Test
    void shouldAddXRequestIdHeaderWhenRequestIdIsNull() {

        server.expect(requestTo("http://localhost:8080/payments/1"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(headerDoesNotExist("x-request-id"))
                .andRespond(withStatus(HttpStatus.OK));

        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(null,
                mock(ResponseHandler.class), configuredMapper(), restClient,
                "http://payments", null, false);

        uk.gov.companieshouse.paymentprocessed.consumer.logging.DataMapHolder.initialise(null);

        client.patchPayment("http://localhost:8080/payments/1", new PaymentPatchRequestApi());

        server.verify();
        DataMapHolder.clear();
    }

    @Test
    void shouldAddXRequestIdHeaderWhenRequestIdIsBlank() {

        server.expect(requestTo("http://localhost:8080/payments/1"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(headerDoesNotExist("x-request-id"))
                .andRespond(withStatus(HttpStatus.OK));

        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(null,
                mock(ResponseHandler.class), configuredMapper(), restClient,
                "http://payments", null, false);

        uk.gov.companieshouse.paymentprocessed.consumer.logging.DataMapHolder.initialise("   ");

        client.patchPayment("http://localhost:8080/payments/1", new PaymentPatchRequestApi());

        server.verify();
        DataMapHolder.clear();
    }

    @Test
    void shouldHandleJsonProcessingExceptionWhenLoggingRequestValue() throws Exception {
        server.expect(requestTo("http://localhost:8080/payments/1"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.OK));

        ObjectMapper mockMapper = mock(ObjectMapper.class);
        JsonProcessingException jsonEx = mock(JsonProcessingException.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(jsonEx);

        ResponseHandler handler = mock(ResponseHandler.class);
        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(null,
                handler, mockMapper, restClient,
                "http://payments", null, false);

        client.patchPayment("/payments/1", new PaymentPatchRequestApi());

        server.verify();
        verify(handler).handle("Patch Payment", "/payments/1", jsonEx);
    }

    private static ObjectMapper configuredMapper() {
        ObjectMapper m = new ObjectMapper();
        m.findAndRegisterModules();
        return m;
    }

}
