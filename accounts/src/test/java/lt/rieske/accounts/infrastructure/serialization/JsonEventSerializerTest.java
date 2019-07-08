package lt.rieske.accounts.infrastructure.serialization;

import lt.rieske.accounts.domain.AccountOpenedEvent;
import lt.rieske.accounts.eventsourcing.Event;
import org.junit.Test;
import org.reflections.Reflections;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonEventSerializerTest {

    private final JsonEventSerializer serializer = new JsonEventSerializer();

    @Test
    public void canDeserializeAccountEvents() {
        var reflections = new Reflections("lt.rieske.accounts");
        for (var concreteEventClass : reflections.getSubTypesOf(Event.class)) {
            var serializedEmptyPayload = "{\"@t\":\"" + concreteEventClass.getSimpleName() + "\"}";
            var event = serializer.deserialize(serializedEmptyPayload.getBytes());
            assertThat(event).isInstanceOf(concreteEventClass);
        }
    }

    @Test
    public void serializesAndDeserializesAnEvent() {
        var event = new AccountOpenedEvent(UUID.randomUUID(), UUID.randomUUID());

        var serializedEvent = serializer.serialize(event);
        var deserializedEvent = serializer.deserialize(serializedEvent);

        assertThat(deserializedEvent).isEqualTo(event);
    }


}
