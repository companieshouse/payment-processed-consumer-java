package uk.gov.companieshouse.payment_processed_consumer.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.logging.Logger;

@Configuration
public class LoggingUtilsConfiguration {

    @Bean
    public LoggingUtils getLoggingUtils(Logger logger) {
        return new LoggingUtils(logger);
    }
}