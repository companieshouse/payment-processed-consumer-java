package uk.gov.companieshouse.paymentprocessed.consumer.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
public class AppConfig {

    @Value("${internal.api-key}")
    private String chsInternalApiKey;

    @Value("${timeout.milliseconds}")
    private int timeoutMilliseconds;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new JavaTimeModule());
    }

    @Bean
    Supplier<InternalApiClient> internalApiClientSupplier() {
        return () -> new InternalApiClient(new ApiKeyHttpClient(
                chsInternalApiKey));
    }

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofMillis(timeoutMilliseconds));
        return WebClient.builder()
                .defaultHeaders(headers ->
                        headers.setBasicAuth(chsInternalApiKey, "")
                ).clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

}