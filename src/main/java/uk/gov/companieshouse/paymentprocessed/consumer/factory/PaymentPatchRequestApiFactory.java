package uk.gov.companieshouse.paymentprocessed.consumer.factory;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.payment.PaymentPatchRequestApi;

import java.util.Date;

@Component
public interface PaymentPatchRequestApiFactory {
    PaymentPatchRequestApi createPaymentPatchRequest(String status, Date paidAt, String paymentReference);

    PaymentPatchRequestApi createPaymentRefundPatchRequest(String refundId, String paymentReference);

}
