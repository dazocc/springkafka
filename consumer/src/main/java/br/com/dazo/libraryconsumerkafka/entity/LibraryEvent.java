package br.com.dazo.libraryconsumerkafka.entity;


import lombok.*;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
public class LibraryEvent {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private LibraryEventType libraryEventType;

    @OneToOne(mappedBy = "libraryEvent", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Book book;

}
