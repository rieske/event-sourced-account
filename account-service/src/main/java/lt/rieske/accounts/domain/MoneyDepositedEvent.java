package lt.rieske.accounts.domain;

public record MoneyDepositedEvent(long amountDeposited, long balance) implements AccountEvent {
}
