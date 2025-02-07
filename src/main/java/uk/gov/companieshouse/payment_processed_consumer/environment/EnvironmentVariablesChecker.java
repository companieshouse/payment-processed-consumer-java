package uk.gov.companieshouse.payment_processed_consumer.environment;

import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.environment.exception.EnvironmentVariableException;
import uk.gov.companieshouse.environment.impl.EnvironmentReaderImpl;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.payment_processed_consumer.PaymentProcessedConsumerApplication;


public class EnvironmentVariablesChecker {
    public static final Logger LOGGER = LoggerFactory.getLogger(
            PaymentProcessedConsumerApplication.APPLICATION_NAME_SPACE);

    public enum RequiredEnvironmentVariables {
        BROKER_ADDRESS("KAFKA_BROKER_ADDRESS"),
        CHS_API_KEY("CHS_API_KEY"),
        SCHEMA_REGISTRY_URL("SCHEMA_REGISTRY_URL"),
        PAYMENTS_API_URL("PAYMENTS_API_URL");


        private final String name;

        RequiredEnvironmentVariables(String name) { this.name = name; }

        public String getName() { return this.name; }

    }

    public static boolean allRequiredEnvironmentVariablesPresent() {
        EnvironmentReader environmentReader = new EnvironmentReaderImpl();
        var allVariablesPresent = true;
        LOGGER.info("Checking all environment variables present");
        for(RequiredEnvironmentVariables param : RequiredEnvironmentVariables.values()) {
            try{
                environmentReader.getMandatoryString(param.getName());
            } catch (EnvironmentVariableException eve) {
                allVariablesPresent = false;
                LOGGER.error(String.format("Required config item %s missing", param.getName()));
            }
        }

        return allVariablesPresent;
    }



}
