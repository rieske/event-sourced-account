package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.eventsourcing.Event;

interface EventSerializer<E extends Event> {
    byte[] serialize(Event event);
    E deserialize(byte[] serializedEvent);
}
