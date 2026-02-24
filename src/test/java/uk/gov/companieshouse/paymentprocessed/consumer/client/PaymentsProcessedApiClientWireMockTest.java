package uk.gov.companieshouse.paymentprocessed.consumer.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import uk.gov.companieshouse.api.model.payment.PaymentPatchRequestApi;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException;

import java.text.ParseException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils.getPaymentPatchRequestApi;

@WireMockTest(httpPort = 8080)
class PaymentsProcessedApiClientWireMockTest {

    public static final String HTTP_LOCALHOST_8080 = "http://localhost:8080";
    public static final String PAYMENTS = "/payments";
    public static final String HTTP_LOCALHOST_8080_PAYMENTS = HTTP_LOCALHOST_8080 + PAYMENTS;

    private final PaymentsProcessedApiClient paymentsProcessedApiClient = new PaymentsProcessedApiClient(
            null, new ResponseHandler(), new ObjectMapper(), WebClient.create(),
            null, null, null);

    @Test
    void shouldHandleSuccessfulPatchRequest() throws ParseException {
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
    void shouldHandleBadRequest() throws ParseException {
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
    void shouldHandleConflict() throws ParseException {
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
    void shouldHandleServerError() throws ParseException {
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
    void shouldThrowTimeoutExceptionUsingWireMock() throws ParseException {
        // Arrange
        PaymentPatchRequestApi paymentPatchRequestApi = getPaymentPatchRequestApi();
        // Simulate a delayed response
        stubFor(patch(urlEqualTo(PAYMENTS))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withFixedDelay(7000))); // 7-second delay

        // Configure WebClient with a timeout
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(6)); // 6-second timeout
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        PaymentsProcessedApiClient client = new PaymentsProcessedApiClient(null, new ResponseHandler(), new ObjectMapper(), webClient, null, null, null);

        // Act & Assert
        Assertions.assertThrows(Exception.class, () ->
                client.patchPayment(HTTP_LOCALHOST_8080_PAYMENTS, paymentPatchRequestApi)
        );

        // Verify
        verify(1, patchRequestedFor(urlEqualTo(PAYMENTS)));
    }
}
