package br.com.dazo.libraryconsumerkafka.repository;

import br.com.dazo.libraryconsumerkafka.entity.LibraryEvent;
import org.springframework.data.repository.CrudRepository;

public interface LibraryEventsRepository extends CrudRepository<LibraryEvent,Long> {
}
