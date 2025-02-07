package uk.gov.companieshouse.payment_processed_consumer.apiclient;


import uk.gov.companieshouse.api.InternalApiClient;

public interface IApiClientService {

   public InternalApiClient getPaymentsApiClient();

}

