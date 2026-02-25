package uk.gov.companieshouse.paymentprocessed.consumer.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;
import payments.payment_processed;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.payment.PaymentLinks;
import uk.gov.companieshouse.api.model.payment.PaymentPatchRequestApi;
import uk.gov.companieshouse.api.model.payment.PaymentResponse;
import uk.gov.companieshouse.api.model.payment.RefundModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static uk.gov.companieshouse.paymentprocessed.consumer.client.PaymentsProcessedApiClientTest.getAPIResponse;

public class TestUtils {

    public static final String RESOURCE_LINK = "/transactions/174365-968117-586962/payment";

    public static final String GET_URI = "/payments/P9hl8PWQQrKRBk1Zmc";


    public static ApiResponse<PaymentResponse> getPaymentResponse() throws ParseException {
        PaymentResponse paymentResponse = new PaymentResponse();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        // Set values directly using setters
        paymentResponse.setAmount("55.00");
        paymentResponse.setCompletedAt(formatter.parse("2025-09-24T06:44:32.354Z"));
        paymentResponse.setCreatedAt(formatter.parse("2025-09-24T06:44:27.854Z"));
        paymentResponse.setDescription("Application to register a Companies House authorised agent");

        // Set links
        PaymentLinks links = new PaymentLinks();
        links.setJourney("https://payments.local.org/payments/Bq286888xzSfXk/pay");
        links.setResource(RESOURCE_LINK);
        links.setSelf("payments/Bq286888xzSfXk");
        paymentResponse.setLinks(links);

        paymentResponse.setPaymentMethod("credit-card");
        paymentResponse.setReference("Register_ACSP_174365-968117-586962");
        paymentResponse.setStatus("paid");
        paymentResponse.setEtag("34e92e90a981a9686b45a56204e98d7d1fef86bbb446bf0c2cf5c679");
        paymentResponse.setKind("payment-session#payment-session");

        return getAPIResponse(paymentResponse);
    }


    public static PaymentPatchRequestApi getPaymentPatchRequestApi() throws ParseException {
        PaymentPatchRequestApi paymentPatchRequestApi = new PaymentPatchRequestApi();

        // Set values directly using setters
        paymentPatchRequestApi.setStatus("paid");
        paymentPatchRequestApi.setPaymentReference("Register_ACSP_174365-968117-586962");
        // Parse the date and set it
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date date = formatter.parse("2025-09-24T06:44:32.354Z");
        paymentPatchRequestApi.setPaidAt(date);

        return paymentPatchRequestApi;
    }

    public static ObjectMapper getObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @NotNull
    public static payment_processed getPaymentProcessed() {
        payment_processed paymentProcessed = new payment_processed();
        paymentProcessed.setAttempt(1);
        paymentProcessed.setPaymentResourceId("P9hl8PWQQrKRBk1Zmc");
        return paymentProcessed;
    }

    public static ApiResponse<PaymentResponse> getPaymentResponseRefund() throws ParseException {
        PaymentResponse paymentResponse = new PaymentResponse();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        // Set values using setters
        paymentResponse.setAmount("55.00");
        paymentResponse.setCompletedAt(formatter.parse("2025-09-24T06:44:32.354Z"));
        paymentResponse.setCreatedAt(formatter.parse("2025-09-24T06:44:27.854Z"));
        paymentResponse.setDescription("Application to register a Companies House authorised agent");

        // Set links
        PaymentLinks links = new PaymentLinks();
        links.setJourney("https://payments.local.org/payments/Bq286888xzSfXk/pay");
        links.setResource(RESOURCE_LINK);
        links.setSelf("payments/Bq286888xzSfXk");
        paymentResponse.setLinks(links);

        paymentResponse.setPaymentMethod("credit-card");
        paymentResponse.setReference("Register_ACSP_174365-968117-586962");
        paymentResponse.setStatus("paid");
        paymentResponse.setEtag("34e92e90a981a9686b45a56204e98d7d1fef86bbb446bf0c2cf5c679");
        paymentResponse.setKind("payment-session#payment-session");

        // Set refunds
        List<RefundModel> refunds = new ArrayList<>();
        RefundModel refund1 = new RefundModel();
        refund1.setRefundId("R123");
        refund1.setCreatedAt(formatter.parse("2025-09-23T10:15:30.000Z"));
        refund1.setAmount(1000);
        refund1.setStatus("approved");
        refund1.setExternalRefundUrl("https://example.com/refund/R123");
        refund1.setRefundReference("REF123");

        RefundModel refund2 = new RefundModel();
        refund2.setRefundId("R124");
        refund2.setCreatedAt(formatter.parse("2025-09-22T09:10:25.000Z"));
        refund2.setAmount(500);
        refund2.setStatus("pending");
        refund2.setExternalRefundUrl("https://example.com/refund/R124");
        refund2.setRefundReference("REF124");

        refunds.add(refund1);
        refunds.add(refund2);
        paymentResponse.setRefunds(refunds);

        return getAPIResponse(paymentResponse);
    }
}
