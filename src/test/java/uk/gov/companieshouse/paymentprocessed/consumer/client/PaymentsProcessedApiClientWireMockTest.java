package uk.gov.companieshouse.paymentprocessed.consumer.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils.getPaymentPatchRequestApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;
import uk.gov.companieshouse.api.model.payment.PaymentPatchRequestApi;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException;

@WireMockTest(httpPort = 9843)
class PaymentsProcessedApiClientWireMockTest {

    public static final String HTTP_LOCALHOST_9843 = "http://localhost:9843";
    public static final String PAYMENTS = "/payments";
    public static final String HTTP_LOCALHOST_8080_PAYMENTS = HTTP_LOCALHOST_9843 + PAYMENTS;

    private final PaymentsProcessedApiClient paymentsProcessedApiClient = new PaymentsProcessedApiClient(
            null, new ResponseHandler(), configuredMapper(), RestClient.create(),
            null, null, null);

    private static ObjectMapper configuredMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Test
    void shouldHandleSuccessfulPatchRequest() {
        // Arrange
        PaymentPatchRequestApi paymentPatchRequestApi = getPaymentPatchRequestApi();

        stubFor(patch(urlEqualTo(PAYMENTS))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NO_CONTENT.value())));

        // Act & Assert
        paymentsProcessedApiClient.patchPayment(HTTP_LOCALHOST_8080_PAYMENTS, paymentPatchRequestApi);

        // Verify
        verify(1, patchRequestedFor(urlEqualTo(PAYMENTS)));
    }

    @Test
    void shouldHandleBadRequest() {
        // Arrange
        PaymentPatchRequestApi paymentPatchRequestApi = getPaymentPatchRequestApi();

        stubFor(patch(urlEqualTo(PAYMENTS))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())));

        // Act & Assert
        assertThrows(NonRetryableException.class, () ->
                paymentsProcessedApiClient.patchPayment(HTTP_LOCALHOST_8080_PAYMENTS, paymentPatchRequestApi));

        // Verify
        verify(1, patchRequestedFor(urlEqualTo(PAYMENTS)));
    }

    @Test
    void shouldHandleConflict() {
        // Arrange
        PaymentPatchRequestApi paymentPatchRequestApi = getPaymentPatchRequestApi();

        stubFor(patch(urlEqualTo(PAYMENTS))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CONFLICT.value())));

        // Act & Assert
        assertThrows(NonRetryableException.class, () ->
                paymentsProcessedApiClient.patchPayment(HTTP_LOCALHOST_8080_PAYMENTS, paymentPatchRequestApi));

        // Verify
        verify(1, patchRequestedFor(urlEqualTo(PAYMENTS)));
    }

    @Test
    void shouldHandleServerError() {
        // Arrange
        PaymentPatchRequestApi paymentPatchRequestApi = getPaymentPatchRequestApi();

        stubFor(patch(urlEqualTo(PAYMENTS))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        // Act & Assert
        assertThrows(RetryableException.class, () ->
                paymentsProcessedApiClient.patchPayment(HTTP_LOCALHOST_8080_PAYMENTS, paymentPatchRequestApi));

        // Verify
        verify(1, patchRequestedFor(urlEqualTo(PAYMENTS)));
    }

    @Test
    void shouldThrowTimeoutExceptionUsingWireMock() {
        // Arrange
        PaymentPatchRequestApi paymentPatchRequestApi = getPaymentPatchRequestApi();
        // Simulate a delayed response
        stubFor(patch(urlEqualTo(PAYMENTS))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withFixedDelay(7000)));

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(6));
        RestClient restClient = RestClient.builder()
                .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
                .build();

        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(null, new ResponseHandler(),
                configuredMapper(), restClient, null, null, null);

        // Act & Assert
        Assertions.assertThrows(Exception.class, () ->
                client.patchPayment(HTTP_LOCALHOST_8080_PAYMENTS, paymentPatchRequestApi)
        );

        // Verify
        verify(1, patchRequestedFor(urlEqualTo(PAYMENTS)));
    }
}
