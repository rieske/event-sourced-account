package lt.rieske.accounts.eventstore.serialization;

import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.SequencedEvent;
import lt.rieske.accounts.eventstore.SerializedEvent;

import java.util.List;


public interface EventSerializer<T> {
    byte[] serialize(Event event);

    Event<T> deserialize(byte[] serializedEvent);
    List<SequencedEvent<T>> deserialize(List<SerializedEvent> serializedEvents);
}
