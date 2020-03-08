package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.domain.AccountClosedEvent;
import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.domain.AccountOpenedEvent;
import lt.rieske.accounts.domain.AccountSnapshot;
import lt.rieske.accounts.domain.MoneyDepositedEvent;
import lt.rieske.accounts.domain.MoneyWithdrawnEvent;
import lt.rieske.accounts.eventsourcing.Event;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;


class MessagePackAccountEventSerializer implements EventSerializer<AccountEventsVisitor> {

    private static final int ACCOUNT_SNAPSHOT = 0;
    private static final int ACCOUNT_OPENED = 1;
    private static final int MONEY_DEPOSITED = 2;
    private static final int MONEY_WITHDRAWN = 3;
    private static final int ACCOUNT_CLOSED = 4;

    @Override
    public byte[] serialize(Event<AccountEventsVisitor> event) {
        var packer = MessagePack.newDefaultBufferPacker();
        var serializingVisitor = new Serializer(packer);
        event.accept(serializingVisitor);
        return packer.toByteArray();
    }

    @Override
    public Event<AccountEventsVisitor> deserialize(byte[] serializedEvent) {
        try (var unpacker = MessagePack.newDefaultUnpacker(serializedEvent)) {
            int eventType = unpacker.unpackInt();
            return deserializeEvent(eventType, unpacker);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Event<AccountEventsVisitor> deserializeEvent(int eventType, MessageUnpacker unpacker) throws IOException {
        switch (eventType) {
            case ACCOUNT_SNAPSHOT:
                return new AccountSnapshot<>(unpackUUID(unpacker), unpackUUID(unpacker), unpacker.unpackLong(), unpacker.unpackBoolean());
            case ACCOUNT_OPENED:
                return new AccountOpenedEvent<>(unpackUUID(unpacker));
            case MONEY_DEPOSITED:
                return new MoneyDepositedEvent<>(unpacker.unpackLong(), unpacker.unpackLong());
            case MONEY_WITHDRAWN:
                return new MoneyWithdrawnEvent<>(unpacker.unpackLong(), unpacker.unpackLong());
            case ACCOUNT_CLOSED:
                return new AccountClosedEvent<>();
            default:
                throw new IllegalArgumentException("Unrecognized serialized event type: " + eventType);
        }
    }

    private static void packUUID(MessagePacker packer, UUID uuid) throws IOException {
        packer.packLong(uuid.getMostSignificantBits()).packLong(uuid.getLeastSignificantBits());
    }

    private static UUID unpackUUID(MessageUnpacker unpacker) throws IOException {
        return new UUID(unpacker.unpackLong(), unpacker.unpackLong());
    }

    private static class Serializer implements AccountEventsVisitor {

        private final MessageBufferPacker packer;

        private Serializer(MessageBufferPacker packer) {
            this.packer = packer;
        }

        @Override
        public void visit(AccountSnapshot snapshot) {
            try {
                packer.packInt(ACCOUNT_SNAPSHOT);
                packUUID(packer, snapshot.accountId());
                packUUID(packer, snapshot.ownerId());
                packer.packLong(snapshot.balance())
                        .packBoolean(snapshot.open())
                        .close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void visit(AccountOpenedEvent event) {
            try {
                packer.packInt(ACCOUNT_OPENED);
                packUUID(packer, event.ownerId());
                packer.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void visit(MoneyDepositedEvent event) {
            try {
                packer.packInt(MONEY_DEPOSITED)
                        .packLong(event.amountDeposited())
                        .packLong(event.balance())
                        .close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void visit(MoneyWithdrawnEvent event) {
            try {
                packer.packInt(MONEY_WITHDRAWN)
                        .packLong(event.amountWithdrawn())
                        .packLong(event.balance())
                        .close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void visit(AccountClosedEvent event) {
            try {
                packer.packInt(ACCOUNT_CLOSED).close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
