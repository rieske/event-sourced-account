package lt.rieske.accounts.api;

import lt.rieske.accounts.domain.AccountClosedEvent;
import lt.rieske.accounts.domain.AccountEvent;
import lt.rieske.accounts.domain.AccountOpenedEvent;
import lt.rieske.accounts.domain.AccountSnapshot;
import lt.rieske.accounts.domain.MoneyDepositedEvent;
import lt.rieske.accounts.domain.MoneyWithdrawnEvent;
import lt.rieske.accounts.eventsourcing.SequencedEvent;

import java.util.List;


// not thread safe - do not reuse
public class EventStreamJsonSerializer {

    private StringBuilder sb;

    String toJson(List<SequencedEvent<AccountEvent>> events) {
        sb = new StringBuilder();
        sb.append("[");
        events.forEach(e -> {
            sb.append("{");
            appendJsonLong("sequenceNumber", e.sequenceNumber());
            separateField();
            appendJsonString("transactionId", e.transactionId().toString());
            serializeAccountEvent(e.event());
            sb.append("},");
        });
        if (!events.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]");
        return sb.toString();
    }

    private void serializeAccountEvent(AccountEvent event) {
        switch (event) {
            case AccountSnapshot accountSnapshot -> {
                separateField();
                appendJsonString("type", event.getClass().getSimpleName());
            }
            case AccountOpenedEvent accountOpened -> {
                separateField();
                appendEventType(event);
                separateField();
                appendJsonString("ownerId", accountOpened.ownerId().toString());
            }
            case AccountClosedEvent accountClosed -> {
                separateField();
                appendEventType(event);
            }
            case MoneyDepositedEvent moneyDeposited -> {
                separateField();
                appendEventType(event);
                separateField();
                appendJsonLong("amountDeposited", moneyDeposited.amountDeposited());
                separateField();
                appendBalance(moneyDeposited.balance());
            }
            case MoneyWithdrawnEvent moneyWithdrawn -> {
                separateField();
                appendEventType(event);
                separateField();
                appendJsonLong("amountWithdrawn", moneyWithdrawn.amountWithdrawn());
                separateField();
                appendBalance(moneyWithdrawn.balance());
            }
        }
    }

    private void appendJsonString(String name, String value) {
        sb.append("\"").append(name).append("\":\"").append(value).append("\"");
    }

    private void appendJsonLong(String name, long value) {
        sb.append("\"").append(name).append("\":").append(value);
    }

    private void appendEventType(AccountEvent event) {
        appendJsonString("type", event.getClass().getSimpleName());
    }

    private void appendBalance(long balance) {
        appendJsonLong("balance", balance);
    }

    private void separateField() {
        sb.append(",");
    }
}
