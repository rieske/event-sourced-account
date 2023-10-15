package lt.rieske.accounts.eventsourcing;

public interface EventVisitor<T extends Event> {
    void visit(T event);
}
