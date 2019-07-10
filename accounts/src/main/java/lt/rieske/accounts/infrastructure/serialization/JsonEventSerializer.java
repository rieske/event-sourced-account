package lt.rieske.accounts.infrastructure.serialization;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lt.rieske.accounts.domain.AccountClosedEvent;
import lt.rieske.accounts.domain.AccountOpenedEvent;
import lt.rieske.accounts.domain.AccountSnapshot;
import lt.rieske.accounts.domain.MoneyDepositedEvent;
import lt.rieske.accounts.domain.MoneyWithdrawnEvent;
import lt.rieske.accounts.eventsourcing.Event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

public class JsonEventSerializer implements EventSerializer {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .addMixIn(Event.class, PolymorphicEventMixIn.class);

    @Override
    public byte[] serialize(Event event) {
        try {
            return objectMapper.writeValueAsBytes(event);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Event deserialize(byte[] serializedEvent) {
        try {
            return objectMapper.readValue(serializedEvent, Event.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    @Override
    public List<Event> deserialize(List<byte[]> serializedEvents) {
        return null;
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