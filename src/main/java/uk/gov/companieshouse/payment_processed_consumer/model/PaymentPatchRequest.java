package uk.gov.companieshouse.payment_processed_consumer.model;

import java.time.LocalDateTime;

public class PaymentPatchRequest {

    private  LocalDateTime paidAt;
    private  String status;
    private  String paymentReference;
    private  String refundID;
    private  String refundReference;
    private  String refundStatus;
    private  LocalDateTime refundProcessedAt;

    public PaymentPatchRequest(LocalDateTime paidAt, String status, String paymentReference,
            String refundID, String refundReference, String refundStatus,
            LocalDateTime refundProcessedAt) {
        this.paidAt = paidAt;
        this.status = status;
        this.paymentReference = paymentReference;
        this.refundID = refundID;
        this.refundReference = refundReference;
        this.refundStatus = refundStatus;
        this.refundProcessedAt = refundProcessedAt;
    }

    public PaymentPatchRequest() {

    }


    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getRefundReference() {
        return refundReference;
    }

    public void setRefundReference(String refundReference) {
        this.refundReference = refundReference;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public void setRefundProcessedAt(LocalDateTime refundProcessedAt) {
        this.refundProcessedAt = refundProcessedAt;
    }

    public LocalDateTime getRefundProcessedAt() {
        return refundProcessedAt;
    }

    public String getRefundStatus() {
        return refundID;
    }

    public String getStatus() {

        return status;
    }

    public String getPaymentReference() {

        return paymentReference;
    }

    public LocalDateTime getPaidAt() {

        return paidAt;
    }
}
