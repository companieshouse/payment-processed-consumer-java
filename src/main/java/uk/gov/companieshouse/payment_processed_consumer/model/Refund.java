package uk.gov.companieshouse.payment_processed_consumer.model;

import java.time.LocalDateTime;


public class Refund {

    private String refundID;
    private LocalDateTime createdAt;
    private int amount;
    private String status;
    private String externalRefundURL;
    private String refundReference;

    public Refund(String refundID, LocalDateTime createdAt, int amount, String status,
            String externalRefundURL, String refundReference) {
        this.refundID = refundID;
        this.createdAt = createdAt;
        this.amount = amount;
        this.status = status;
        this.externalRefundURL = externalRefundURL;
        this.refundReference = refundReference;
    }

    public Object getRefundId() {
        return null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    public String getRefundReference() {
        return refundReference;
    }

}
