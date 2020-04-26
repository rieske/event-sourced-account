package lt.rieske.accounts.api;

import lt.rieske.accounts.domain.AccountClosedEvent;
import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.domain.AccountOpenedEvent;
import lt.rieske.accounts.domain.AccountSnapshot;
import lt.rieske.accounts.domain.MoneyDepositedEvent;
import lt.rieske.accounts.domain.MoneyWithdrawnEvent;
import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.SequencedEvent;

import java.util.List;


// not thread safe - do not reuse
public class EventStreamJsonSerializer implements AccountEventsVisitor {

    private StringBuilder sb;

    String toJson(List<SequencedEvent<AccountEventsVisitor>> events) {
        sb = new StringBuilder();
        sb.append("[");
        events.forEach(e -> {
            sb.append("{");
            appendJsonLong("sequenceNumber", e.sequenceNumber());
            separateField();
            appendJsonString("transactionId", e.transactionId().toString());
            e.event().accept(this);
            sb.append("},");
        });
        if (!events.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]");
        return sb.toString();
    }

    private void appendJsonString(String name, String value) {
        sb.append("\"").append(name).append("\":\"").append(value).append("\"");
    }

    private void appendJsonLong(String name, long value) {
        sb.append("\"").append(name).append("\":").append(value);
    }

    private void appendEventType(Event<AccountEventsVisitor> event) {
        appendJsonString("type", event.getClass().getSimpleName());
    }

    private void appendBalance(long balance) {
        appendJsonLong("balance", balance);
    }

    private void separateField() {
        sb.append(",");
    }

    @Override
    public void visit(AccountSnapshot event) {
        separateField();
        appendJsonString("type", event.getClass().getSimpleName());
    }

    @Override
    public void visit(AccountOpenedEvent event) {
        separateField();
        appendEventType(event);
        separateField();
        appendJsonString("ownerId", event.ownerId().toString());
    }

    @Override
    public void visit(MoneyDepositedEvent event) {
        separateField();
        appendEventType(event);
        separateField();
        appendJsonLong("amountDeposited", event.amountDeposited());
        separateField();
        appendBalance(event.balance());
    }

    @Override
    public void visit(MoneyWithdrawnEvent event) {
        separateField();
        appendEventType(event);
        separateField();
        appendJsonLong("amountWithdrawn", event.amountWithdrawn());
        separateField();
        appendBalance(event.balance());
    }

    @Override
    public void visit(AccountClosedEvent event) {
        separateField();
        appendEventType(event);
    }
}
