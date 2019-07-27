package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.SequencedEvent;

import java.util.List;
import java.util.stream.Stream;


interface EventSerializer<T> {
    byte[] serialize(Event event);

    Event<T> deserialize(byte[] serializedEvent);
    Stream<SequencedEvent<T>> deserialize(List<SerializedEvent> serializedEvents);
}
