package br.com.dazo.libraryconsumerkafka.consumer;

import br.com.dazo.libraryconsumerkafka.entity.LibraryEvent;
import br.com.dazo.libraryconsumerkafka.entity.LibraryEventType;
import br.com.dazo.libraryconsumerkafka.repository.LibraryEventsRepository;
import br.com.dazo.libraryconsumerkafka.service.LibraryEventsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
public class LibraryEventsConsumerIntegrationTest {

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaTemplate<Long, String> kafkaTemplate;

    @Autowired
    KafkaListenerEndpointRegistry endpointRegistry;

    @Autowired
    LibraryEventsRepository libraryEventsRepository;

    @Autowired
    ObjectMapper objectMapper;

    @SpyBean
    LibraryEventsConsumer libraryEventsConsumerSpy;

    @SpyBean
    LibraryEventsService libraryEventsServiceSpy;


    @BeforeEach
    void setUp() {
        for (MessageListenerContainer messageListenerContainer : endpointRegistry.getListenerContainers()){
            ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        }
    }

    @AfterEach
    void tearDown() {
        libraryEventsRepository.deleteAll();
    }

    @Test
    void publishNewLibraryEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
        //given
        String json = " {\"id\":null,\"libraryEventType\":\"NEW\",\"book\":{\"id\":456,\"name\":\"Kafka Using Spring Boot\",\"author\":\"Dazo\"}}";
        kafkaTemplate.sendDefault(json).get();

        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

        //then
        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        List<LibraryEvent> libraryEventList = (List<LibraryEvent>) libraryEventsRepository.findAll();
        assert libraryEventList.size() ==1;
        libraryEventList.forEach(libraryEvent -> {
            assert libraryEvent.getId()!=null;
            assertEquals(456, libraryEvent.getBook().getId());
        });
    }

    @Test
    void publishUpdateLibraryEvent() throws JsonProcessingException, ExecutionException, InterruptedException {
        String json = " {\"id\":null,\"libraryEventType\":\"NEW\",\"book\":{\"id\":456,\"name\":\"Kafka Using Spring Boot\",\"author\":\"Dazo\"}}";

        LibraryEvent libraryEvent = objectMapper.readValue(json, LibraryEvent.class);

        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventsRepository.save(libraryEvent);

        libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
        libraryEvent.getBook().setName("Kafka Update");
        libraryEvent.getBook().setLibraryEvent(null);

        String updateJson = objectMapper.writeValueAsString(libraryEvent);

        kafkaTemplate.sendDefault(libraryEvent.getId(), updateJson).get();

        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        LibraryEvent persistedLibraryEvent = libraryEventsRepository.findById(libraryEvent.getId()).get();
        assertEquals("Kafka Update", persistedLibraryEvent.getBook().getName());

    }

    @Test
    void publishModifyLibraryEvent_Not_A_Valid_LibraryEventId() throws JsonProcessingException, InterruptedException, ExecutionException {

        Long id = 123L;
        String json = "{\"libraryEventId\":" + id + ",\"libraryEventType\":\"UPDATE\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"Dazo\"}}";
        System.out.println(json);
        kafkaTemplate.sendDefault(id, json).get();

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        Optional<LibraryEvent> libraryEventOptional = libraryEventsRepository.findById(id);
        assertFalse(libraryEventOptional.isPresent());
    }

    @Test
    void publishModifyLibraryEvent_Null_LibraryEventId() throws JsonProcessingException, InterruptedException, ExecutionException {

        Long id = null;
        String json = "{\"id\":" + id + ",\"libraryEventType\":\"UPDATE\",\"book\":{\"id\":456,\"name\":\"Kafka Using Spring Boot\",\"author\":\"Dazo\"}}";
        kafkaTemplate.sendDefault(id, json).get();

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);


        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));
    }

    @Test
    void publishModifyLibraryEvent_000_LibraryEventId() throws JsonProcessingException, InterruptedException, ExecutionException {
        //given
        Long id = 0L;
        String json = "{\"id\":" + id + ",\"libraryEventType\":\"UPDATE\",\"book\":{\"id\":456,\"name\":\"Kafka Using Spring Boot\",\"author\":\"Dazo\"}}";
        kafkaTemplate.sendDefault(id, json).get();
        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

        //verify(libraryEventsConsumerSpy, times(3)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy, times(4)).processLibraryEvent(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy, times(1 )).handleRecovery(isA(ConsumerRecord.class));

    }

}
