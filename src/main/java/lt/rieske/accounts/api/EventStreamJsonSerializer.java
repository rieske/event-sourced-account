package lt.rieske.accounts.api;

import lt.rieske.accounts.domain.AccountClosedEvent;
import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.domain.AccountOpenedEvent;
import lt.rieske.accounts.domain.AccountSnapshot;
import lt.rieske.accounts.domain.MoneyDepositedEvent;
import lt.rieske.accounts.domain.MoneyWithdrawnEvent;
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
            sb.append(",");
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
        sb.append("\"");
        sb.append(name);
        sb.append("\":\"");
        sb.append(value);
        sb.append("\"");
    }

    private void appendJsonLong(String name, long value) {
        sb.append("\"");
        sb.append(name);
        sb.append("\":");
        sb.append(value);
    }

    @Override
    public void visit(AccountSnapshot event) {
        sb.append(",");
        appendJsonString("type", event.getClass().getSimpleName());
    }

    @Override
    public void visit(AccountOpenedEvent event) {
        sb.append(",");
        appendJsonString("type", event.getClass().getSimpleName());
        sb.append(",");
        appendJsonString("ownerId", event.ownerId().toString());
    }

    @Override
    public void visit(MoneyDepositedEvent event) {
        sb.append(",");
        appendJsonString("type", event.getClass().getSimpleName());
        sb.append(",");
        appendJsonLong("amountDeposited", event.amountDeposited());
        sb.append(",");
        appendJsonLong("balance", event.balance());
    }

    @Override
    public void visit(MoneyWithdrawnEvent event) {
        sb.append(",");
        appendJsonString("type", event.getClass().getSimpleName());
        sb.append(",");
        appendJsonLong("amountWithdrawn", event.amountWithdrawn());
        sb.append(",");
        appendJsonLong("balance", event.balance());
    }

    @Override
    public void visit(AccountClosedEvent event) {
        sb.append(",");
        appendJsonString("type", event.getClass().getSimpleName());
    }
}
