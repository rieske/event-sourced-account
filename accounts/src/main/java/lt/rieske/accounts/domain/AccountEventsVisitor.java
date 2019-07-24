package lt.rieske.accounts.domain;

public interface AccountEventsVisitor {
    void visit(AccountSnapshot snapshot);
    void visit(AccountOpenedEvent event);
    void visit(MoneyDepositedEvent event);
    void visit(MoneyWithdrawnEvent event);
    void visit(AccountClosedEvent event);
}
