package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.eventsourcing.Event;


interface EventSerializer<T> {
    byte[] serialize(Event<T> event);
    Event<T> deserialize(byte[] serializedEvent);
}
