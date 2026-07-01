package uk.gov.companieshouse.paymentprocessed.consumer.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

@Configuration
public class AppConfig {

    @Value("${internal.api-key}")
    private String chsInternalApiKey;

    @Value("${timeout.milliseconds}")
    private int timeoutMilliseconds;

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
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMilliseconds));

        return RestClient.builder()
                .baseUrl(paymentsApiUrl)
                .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
                .defaultHeaders(headers -> headers.setBasicAuth(chsInternalApiKey,""))
                .build();
    }

}