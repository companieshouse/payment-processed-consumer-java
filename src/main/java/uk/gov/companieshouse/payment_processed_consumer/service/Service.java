package uk.gov.companieshouse.payment_processed_consumer.service;

import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.model.payment.PaymentApi;
import uk.gov.companieshouse.payment_processed_consumer.model.PaymentPatchRequest;

public interface Service {

    public void processMessage(ServiceParameters parameters) throws ApiErrorResponseException;
    public PaymentPatchRequest createTransactionPatchRequest(String refundId, String resourceId, PaymentApi
            paymentResponse);

}
