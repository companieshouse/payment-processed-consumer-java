package uk.gov.companieshouse.paymentprocessed.consumer.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import java.net.http.HttpClient;

@Configuration
public class AppConfig {

    @Value("${internal.api-key}")
    private String chsInternalApiKey;

    @Value("${read.timeout.milliseconds}")
    private int readTimeoutMilliseconds;

    @Value("${connection.timeout.milliseconds}")
    private int connectionTimeoutMilliseconds;

    @Value("${payments.api.url}")
    private String paymentsApiUrl;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new JavaTimeModule());
    }

    @Bean
    Supplier<InternalApiClient> internalApiClientSupplier() {
        return () -> new InternalApiClient(new ApiKeyHttpClient(
                chsInternalApiKey));
    }

    @Bean
    public RestClient restClient() {
        HttpClient httpClient=  HttpClient.newBuilder()
                .connectTimeout((Duration.ofMillis(connectionTimeoutMilliseconds)))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeoutMilliseconds);
        return RestClient.builder()
                .baseUrl(paymentsApiUrl)
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> headers.setBasicAuth(chsInternalApiKey,""))
                .build();
    }

}