package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.domain.AccountEvent;
import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.EventVisitor;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;


class MessagePackAccountEventSerializer implements EventSerializer<AccountEvent> {

    private static final int ACCOUNT_SNAPSHOT = 0;
    private static final int ACCOUNT_OPENED = 1;
    private static final int MONEY_DEPOSITED = 2;
    private static final int MONEY_WITHDRAWN = 3;
    private static final int ACCOUNT_CLOSED = 4;

    @Override
    public byte[] serialize(Event event) {
        var packer = MessagePack.newDefaultBufferPacker();
        var serializer = new Serializer(packer);
        if (event instanceof AccountEvent accountEvent) {
            serializer.visit(accountEvent);
        } else {
            throw new IllegalArgumentException("Can not serialize " + event.getClass());
        }
        return packer.toByteArray();
    }

    @Override
    public AccountEvent deserialize(byte[] serializedEvent) {
        try (var unpacker = MessagePack.newDefaultUnpacker(serializedEvent)) {
            int eventType = unpacker.unpackInt();
            return deserializeEvent(eventType, unpacker);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static AccountEvent deserializeEvent(int eventType, MessageUnpacker unpacker) throws IOException {
        return switch (eventType) {
            case ACCOUNT_SNAPSHOT -> new AccountEvent.AccountSnapshot(unpackUUID(unpacker), unpackUUID(unpacker), unpacker.unpackLong(), unpacker.unpackBoolean());
            case ACCOUNT_OPENED -> new AccountEvent.AccountOpenedEvent(unpackUUID(unpacker));
            case MONEY_DEPOSITED -> new AccountEvent.MoneyDepositedEvent(unpacker.unpackLong(), unpacker.unpackLong());
            case MONEY_WITHDRAWN -> new AccountEvent.MoneyWithdrawnEvent(unpacker.unpackLong(), unpacker.unpackLong());
            case ACCOUNT_CLOSED -> new AccountEvent.AccountClosedEvent();
            default -> throw new IllegalArgumentException("Unrecognized serialized event type: " + eventType);
        };
    }

    private static void packUUID(MessagePacker packer, UUID uuid) throws IOException {
        packer.packLong(uuid.getMostSignificantBits()).packLong(uuid.getLeastSignificantBits());
    }

    private static UUID unpackUUID(MessageUnpacker unpacker) throws IOException {
        return new UUID(unpacker.unpackLong(), unpacker.unpackLong());
    }

    private static class Serializer implements EventVisitor<AccountEvent> {

        private final MessageBufferPacker packer;

        private Serializer(MessageBufferPacker packer) {
            this.packer = packer;
        }

        @Override
        public void visit(AccountEvent event) {
            switch (event) {
                case AccountEvent.AccountSnapshot snapshot -> {
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
                case AccountEvent.AccountOpenedEvent accountOpened -> {
                    try {
                        packer.packInt(ACCOUNT_OPENED);
                        packUUID(packer, accountOpened.ownerId());
                        packer.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                case AccountEvent.AccountClosedEvent accountClosed -> {
                    try {
                        packer.packInt(ACCOUNT_CLOSED).close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                case AccountEvent.MoneyDepositedEvent moneyDeposited -> {
                    try {
                        packer.packInt(MONEY_DEPOSITED)
                                .packLong(moneyDeposited.amountDeposited())
                                .packLong(moneyDeposited.balance())
                                .close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                case AccountEvent.MoneyWithdrawnEvent moneyWithdrawn -> {
                    try {
                        packer.packInt(MONEY_WITHDRAWN)
                                .packLong(moneyWithdrawn.amountWithdrawn())
                                .packLong(moneyWithdrawn.balance())
                                .close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        }
    }
}
