package lt.rieske.accounts.infrastructure.serialization;

import lt.rieske.accounts.eventsourcing.Event;

import java.util.List;

public interface EventSerializer {
    byte[] serialize(Event event);

    Event deserialize(byte[] serializedEvent);
    List<Event> deserialize(List<byte[]> serializedEvents);
}
