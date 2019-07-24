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
            appendJsonLong("sequenceNumber", e.getSequenceNumber());
            sb.append(",");
            appendJsonString("transactionId", e.getTransactionId().toString());
            e.getEvent().apply(this);
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
        appendJsonString("ownerId", event.getOwnerId().toString());
    }

    @Override
    public void visit(MoneyDepositedEvent event) {
        sb.append(",");
        appendJsonString("type", event.getClass().getSimpleName());
        sb.append(",");
        appendJsonLong("amountDeposited", event.getAmountDeposited());
        sb.append(",");
        appendJsonLong("balance", event.getBalance());
    }

    @Override
    public void visit(MoneyWithdrawnEvent event) {
        sb.append(",");
        appendJsonString("type", event.getClass().getSimpleName());
        sb.append(",");
        appendJsonLong("amountWithdrawn", event.getAmountWithdrawn());
        sb.append(",");
        appendJsonLong("balance", event.getBalance());
    }

    @Override
    public void visit(AccountClosedEvent event) {
        sb.append(",");
        appendJsonString("type", event.getClass().getSimpleName());
    }
}
