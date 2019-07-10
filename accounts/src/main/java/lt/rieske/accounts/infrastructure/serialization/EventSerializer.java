package lt.rieske.accounts.infrastructure.serialization;

import lt.rieske.accounts.eventsourcing.Event;

import java.util.List;

public interface EventSerializer<T> {
    byte[] serialize(Event event);

    Event<T> deserialize(byte[] serializedEvent);
    List<Event<T>> deserialize(List<byte[]> serializedEvents);
}
