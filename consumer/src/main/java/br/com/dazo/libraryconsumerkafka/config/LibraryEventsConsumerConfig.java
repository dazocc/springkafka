package br.com.dazo.libraryconsumerkafka.config;

import br.com.dazo.libraryconsumerkafka.service.LibraryEventsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.tomcat.jni.Library;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class LibraryEventsConsumerConfig {

    @Autowired
    LibraryEventsService libraryEventsService;

    @Bean
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setConcurrency(3);
        factory.setErrorHandler((thrownException, data) -> {
            log.info("Exception in consumirConfig is {} and the record is {}", thrownException.getMessage(), data);
        } );

        factory.setRetryTemplate(retryTemplate());
        factory.setRecoveryCallback(recoveryCallback());
        // factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    private RecoveryCallback<Object> recoveryCallback() {

        return context -> {

            if (context.getLastThrowable().getCause() instanceof RecoverableDataAccessException) {

                ConsumerRecord<Long, String> consumerRecord = (ConsumerRecord<Long, String>) context.getAttribute("record");

                libraryEventsService.handleRecovery(consumerRecord);

            } else {
                throw new RuntimeException(context.getLastThrowable().getMessage());
            }

            return null;
        };
    }


    private RetryTemplate retryTemplate() {

        RetryTemplate retryTemplate = new RetryTemplate();

        retryTemplate.setRetryPolicy(simpleRetryPolicy());
        retryTemplate.setBackOffPolicy(backOffPolicy());

        return retryTemplate;
    }

    private BackOffPolicy backOffPolicy() {

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(1000);
        return fixedBackOffPolicy;

    }

    private RetryPolicy simpleRetryPolicy() {

        Map<Class<? extends Throwable>, Boolean> exceptionsMap = new HashMap<>();
        exceptionsMap.put(IllegalArgumentException.class, false);
        exceptionsMap.put(RecoverableDataAccessException.class, true);

        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(3, exceptionsMap, true);

        simpleRetryPolicy.setMaxAttempts(3);

        return simpleRetryPolicy;

    }
}