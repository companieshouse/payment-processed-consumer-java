package uk.gov.companieshouse.payment_processed_consumer.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.model.payment.PaymentApi;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.payment_processed_consumer.model.PaymentPatchRequest;



@Component
public class PaymentProcessorService implements Service {

    private final PaymentRequest paymentRequest;
    private final Logger logger;

    public PaymentProcessorService(PaymentRequest paymentRequest, Logger logger) {
        this.paymentRequest = paymentRequest;
        this.logger = logger;
    }

    // Will determine whether payment is a refund or not, the transactionURI is amended if so.
    @Override
    public void processMessage(ServiceParameters parameters) throws ApiErrorResponseException {
        final var message = parameters.getData();
        final var resourceId = message.getPaymentResourceId();
        final var refundId = message.getRefundId();


        logger.info("Processing message " + message + " for resource ID " + resourceId + ".");

        PaymentApi paymentResponse = paymentRequest.getPaymentResponse(resourceId);
        String transactionURI = paymentResponse.getLinks().get("Resource");
        if (!refundId.isEmpty()) {
            transactionURI += "/refunds";
        }
        PaymentPatchRequest patchRequest = createTransactionPatchRequest(refundId, resourceId, paymentResponse);
        paymentRequest.updateTransaction(transactionURI, patchRequest);
    }

    // Builds the PatchRequest to send to updateTransaction
    @Override
    public PaymentPatchRequest createTransactionPatchRequest(String refundId, String resourceId, PaymentApi paymentResponse) {
        PaymentPatchRequest patchRequest = new PaymentPatchRequest();
        if (!refundId.isEmpty()) {
            paymentResponse.getRefunds().stream()
                    .filter(refund -> refund.getId().equals(refundId))
                    .findFirst()
                    .ifPresent(refund -> {
                        patchRequest.setRefundReference(refund.getId());
                        patchRequest.setRefundStatus(String.valueOf(refund.getStatus()));
                        patchRequest.setRefundProcessedAt(
                                LocalDateTime.parse(refund.getCreatedAt()));
                    });
        } else {
            patchRequest.setStatus(paymentResponse.getStatus());
            patchRequest.setPaymentReference(resourceId);
            patchRequest.setPaidAt(LocalDateTime.parse(paymentResponse.getCompletedAt()));
        }
        return patchRequest;
    }
}
