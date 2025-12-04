package uk.gov.companieshouse.paymentprocessed.consumer.factory;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.payment.PaymentPatchRequestApi;

import java.util.Date;

@Component
public class PaymentPatchRequestApiFactoryImpl implements PaymentPatchRequestApiFactory {
    @Override
    public PaymentPatchRequestApi createPaymentPatchRequest(String status, Date paidAt, String paymentReference) {
        PaymentPatchRequestApi paymentPatchRequestApi = new PaymentPatchRequestApi();
        paymentPatchRequestApi.setPaidAt(paidAt);
        paymentPatchRequestApi.setStatus(status);
        paymentPatchRequestApi.setPaymentReference(paymentReference);
        return paymentPatchRequestApi;
    }

    @Override
    public PaymentPatchRequestApi createPaymentRefundPatchRequest(String refundId, String paymentReference) {
        PaymentPatchRequestApi paymentPatchRequestApi = new PaymentPatchRequestApi();
        paymentPatchRequestApi.setRefundId(refundId);
        paymentPatchRequestApi.setPaymentReference(paymentReference);
        return paymentPatchRequestApi;
    }
}
