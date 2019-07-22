package lt.rieske.accounts.infrastructure.serialization;

import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.SequencedEvent;
import lt.rieske.accounts.infrastructure.SerializedEvent;

import java.util.List;

public interface EventSerializer<T> {
    byte[] serialize(Event event);

    Event<T> deserialize(byte[] serializedEvent);
    List<SequencedEvent<T>> deserialize(List<SerializedEvent> serializedEvents);
}
