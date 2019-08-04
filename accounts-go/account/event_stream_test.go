package account

import "testing"

type testEventStore struct {
	e []Event
}

func (es *testEventStore) events(id AccountId, version int64) []Event {
	return es.e
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
