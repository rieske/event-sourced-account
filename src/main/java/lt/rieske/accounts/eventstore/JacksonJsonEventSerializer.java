package lt.rieske.accounts.eventstore;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lt.rieske.accounts.domain.AccountClosedEvent;
import lt.rieske.accounts.domain.AccountOpenedEvent;
import lt.rieske.accounts.domain.AccountSnapshot;
import lt.rieske.accounts.domain.MoneyDepositedEvent;
import lt.rieske.accounts.domain.MoneyWithdrawnEvent;
import lt.rieske.accounts.eventsourcing.Event;

import java.io.IOException;
import java.io.UncheckedIOException;


class JacksonJsonEventSerializer<T> implements EventSerializer<T> {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .addMixIn(Event.class, PolymorphicEventMixIn.class);

    @Override
    public byte[] serialize(Event<T> event) {
        try {
            return objectMapper.writeValueAsBytes(event);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Event<T> deserialize(byte[] serializedEvent) {
        try {
            return objectMapper.readValue(serializedEvent, new TypeReference<Event<T>>() {});
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@t")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = AccountOpenedEvent.class),
            @JsonSubTypes.Type(value = MoneyWithdrawnEvent.class),
            @JsonSubTypes.Type(value = MoneyDepositedEvent.class),
            @JsonSubTypes.Type(value = AccountClosedEvent.class),
            @JsonSubTypes.Type(value = AccountSnapshot.class)
    })
    private interface PolymorphicEventMixIn {
    }
}