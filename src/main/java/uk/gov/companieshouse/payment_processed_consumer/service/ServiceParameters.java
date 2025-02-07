package uk.gov.companieshouse.payment_processed_consumer.service;

import java.util.Objects;
import payments.payment_processed;

public class ServiceParameters {

    private final  payment_processed data;

    public ServiceParameters(payment_processed data){
        this.data =data;
    }

    public payment_processed getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceParameters)) {
            return false;
        }
        ServiceParameters that = (ServiceParameters) o;
        return Objects.equals(getData(), that.getData());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getData());
    }
}


