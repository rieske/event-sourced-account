package lt.rieske.accounts.infrastructure.serialization;

import lt.rieske.accounts.eventsourcing.Event;

import java.util.List;

public class ProtobufEventSerializer<T> implements EventSerializer<T> {

    @Override
    public byte[] serialize(Event event) {
        return new byte[0];
    }

    @Override
    public Event<T> deserialize(byte[] serializedEvent) {
        return null;
    }

    @Override
    public List<Event<T>> deserialize(List<byte[]> serializedEvents) {
        return null;
    }
}
