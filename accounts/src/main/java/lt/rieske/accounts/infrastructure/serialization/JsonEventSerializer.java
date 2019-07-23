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
import lt.rieske.accounts.eventsourcing.SequencedEvent;
import lt.rieske.accounts.infrastructure.SerializedEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;


public class JsonEventSerializer<T> implements EventSerializer<T> {

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
    public Event<T> deserialize(byte[] serializedEvent) {
        try {
            return objectMapper.readValue(serializedEvent, Event.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<SequencedEvent<T>> deserialize(List<SerializedEvent> serializedEvents) {
        return serializedEvents.stream()
                .map(se -> new SequencedEvent<>(se.getAggregateId(), se.getSequenceNumber(), deserialize(se.getPayload())))
                .collect(Collectors.toList());
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