package uk.gov.companieshouse.payment_processed_consumer.model;

public class PaymentLinks {

    private String Journey;
    private String Resource;
    private String Self;

    public PaymentLinks(
            String Journey,
            String Resource,
            String Self
    ) {
        this.Journey = Journey;
        this.Resource = Resource;
        this.Self = Self;
    }

    public PaymentLinks() {

    }

    public String getResource() {
        return Resource;
    }

}
