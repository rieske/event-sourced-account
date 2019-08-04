package account

import (
	"errors"
)

type sequencedEvent struct {
	aggregateId AccountId
	seq         int
	event       Event
}

type eventStore interface {
	events(id AccountId, version int) []Event
	append(events []sequencedEvent)
}

type eventStream struct {
	eventStore       *eventStore
	versions         map[AccountId]int
	uncomittedEvents []sequencedEvent
}

func NewEventStream(es eventStore) *eventStream {
	return &eventStream{&es, map[AccountId]int{}, nil}
}

func (s *eventStream) replay(id AccountId) (*account, error) {
	events := (*s.eventStore).events(id, 0)
	var currentVersion = 0

	a := NewAccount()
	for _, e := range events {
		e.apply(a)
		currentVersion += 1
	}

	if currentVersion == 0 {
		return nil, errors.New("Aggregate not found")
	}

	s.versions[id] = currentVersion
	return a, nil
}

func (s *eventStream) append(e Event, id AccountId) {
	currentVersion := s.versions[id] + 1
	se := sequencedEvent{id, currentVersion, e}
	s.uncomittedEvents = append(s.uncomittedEvents, se)
}

func (s *eventStream) commit() {
	(*s.eventStore).append(s.uncomittedEvents)
	s.uncomittedEvents = nil
}
