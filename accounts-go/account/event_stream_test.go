package account

import "testing"

type testEventStore struct {
	e []Event
}

func (es *testEventStore) events(id AccountId, version int) []Event {
	return es.e
}

func (es *testEventStore) append(events []sequencedEvent) {
	panic("implement me")
}

func TestReplayEvents(t *testing.T) {
	id := AccountId{1}
	ownerId := OwnerId{2}
	store := testEventStore{[]Event{
		AccountOpenedEvent{id, ownerId},
		MoneyDepositedEvent{42, 42},
	}}
	es := NewEventStream(&store)

	a, err := es.replay(id)
	expectNoError(t, err)
	if a == nil {
		t.Error("Account expected")
	}

	if *a.id != id {
		t.Errorf("Account id expected %v, got %v", id, a.id)
	}
	if *a.ownerId != ownerId {
		t.Error("owner id should be set")
	}
	if a.open != true {
		t.Error("account should be open")
	}
	expectBalance(t, *a, 42)

	version := es.versions[id]
	if version != 2 {
		t.Errorf("Version 2 expected, got: %v", version)
	}
}

func TestAppendEvent(t *testing.T) {
	store := testEventStore{[]Event{}}
	es := NewEventStream(&store)

	id := AccountId{1}
	ownerId := OwnerId{2}
	event := AccountOpenedEvent{id, ownerId}
	es.append(event, id)

	seqEvent := es.uncomittedEvents[0]
	assertEqual(t, seqEvent.event, event)
	assertEqual(t, seqEvent.aggregateId, id)
	assertEqual(t, seqEvent.seq, 1)
}

func assertEqual(t *testing.T, a, b interface{}) {
	if a != b {
		t.Errorf("Expected %v, got %v", b, a)
	}
}
