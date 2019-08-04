package account

import (
	"errors"
)

type eventStore interface {
	events(id AccountId, version int64) []Event
}

type eventStream struct {
	eventStore *eventStore
	versions   map[AccountId]int64
}

func NewEventStream(es eventStore) *eventStream {
	return &eventStream{&es, make(map[AccountId]int64)}
}

func (stream *eventStream) replay(id AccountId) (*account, error) {
	events := (*stream.eventStore).events(id, 0)
	var currentVersion int64 = 0

	a := NewAccount()
	for _, e := range events {
		e.apply(a)
		currentVersion += 1
	}

	if currentVersion == 0 {
		return nil, errors.New("Aggregate not found")
	}

	stream.versions[id] = currentVersion
	return a, nil
}

func (stream *eventStream) append(e Event, a *account) {

}

/*
private final List<SequencedEvent<E>> uncommittedEvents = new ArrayList<>();

    @Override
    public void append(Event<E> event, A aggregate, UUID aggregateId) {
        event.accept(aggregate);
        var currentVersion = aggregateVersions.compute(aggregateId, (id, version) -> version != null ? version + 1 : 1);
        uncommittedEvents.add(new SequencedEvent<>(aggregateId, currentVersion, null, event));
    }

    void commit(UUID transactionId) {
        eventStore.append(uncommittedEvents, transactionId);
        uncommittedEvents.clear();
    }
*/
