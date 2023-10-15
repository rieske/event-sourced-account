package lt.rieske.accounts.domain;

public record MoneyWithdrawnEvent(long amountWithdrawn, long balance) implements AccountEvent {
}
