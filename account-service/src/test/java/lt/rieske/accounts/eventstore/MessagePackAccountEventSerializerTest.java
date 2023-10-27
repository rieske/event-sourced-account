package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.domain.AccountEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessagePackAccountEventSerializerTest {

    private final MessagePackAccountEventSerializer serializer = new MessagePackAccountEventSerializer();

    @Test
    void serializesAndDeserializesAnEvent() {
        var event = new AccountEvent.AccountOpenedEvent(UUID.randomUUID());

        var serializedEvent = serializer.serialize(event);
        var deserializedEvent = serializer.deserialize(serializedEvent);

        assertThat(deserializedEvent).isEqualTo(event);
    }

}
