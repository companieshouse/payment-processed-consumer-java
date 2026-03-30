package uk.gov.companieshouse.paymentprocessed.consumer.factory;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.payment.PaymentPatchRequestApi;

import java.time.Instant;

@Component
public interface PaymentPatchRequestApiFactory {
    PaymentPatchRequestApi createPaymentPatchRequest(String status, Instant paidAt, String paymentReference);

    PaymentPatchRequestApi createPaymentRefundPatchRequest(String refundId, String paymentReference);

}
