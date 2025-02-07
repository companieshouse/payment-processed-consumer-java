package uk.gov.companieshouse.payment_processed_consumer.apiclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

@Component
public class ApiClientServiceImpl implements IApiClientService {

    @Value("${payments.api.url}")
    public String paymentsApiUrl;

    @Override
    public InternalApiClient getPaymentsApiClient(){
        InternalApiClient paymentsApiClient = ApiSdkManager.getPrivateSDK();
        paymentsApiClient.setBasePaymentsPath(paymentsApiUrl);
        paymentsApiClient.setBasePath(paymentsApiUrl);
        System.out.println(paymentsApiClient.getHttpClient());
        return paymentsApiClient;

    }



}
