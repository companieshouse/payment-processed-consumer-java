package uk.gov.companieshouse.payment_processed_consumer.apiclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

@ExtendWith(MockitoExtension.class)
public class ApiClientServiceImplTest {

    @Mock
    private InternalApiClient internalApiClient;

    @InjectMocks
    private ApiClientServiceImpl apiClientService;

    @BeforeEach
    public void setUp() {

        when(ApiSdkManager.getPrivateSDK()).thenReturn(internalApiClient);
    }

    @Test
    public void testGetPaymentsApiClient() {
        String paymentsApiUrl = "http://ApiClientServiceImplTest.com";
        apiClientService.paymentsApiUrl = paymentsApiUrl;

        InternalApiClient result = apiClientService.getPaymentsApiClient();

        verify(internalApiClient).setBasePaymentsPath(paymentsApiUrl);
        verify(internalApiClient).setBasePath(paymentsApiUrl);
        assertEquals(internalApiClient, result);
    }
}