package account

import "testing"

func TestReplayEvents(t *testing.T) {
	id := AggregateId{1}
	ownerId := OwnerId{2}
	store := inmemoryEeventstore{}
	store.Append([]sequencedEvent{
		{id, 1, AccountOpenedEvent{id, ownerId}},
		{id, 1, MoneyDepositedEvent{42, 42}},
	})

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
	store := inmemoryEeventstore{}
	es := NewEventStream(&store)

	id := AggregateId{1}
	ownerId := OwnerId{2}
	event := AccountOpenedEvent{id, ownerId}
	es.append(event, id)

	seqEvent := es.uncomittedEvents[0]
	assertEqual(t, seqEvent.event, event)
	assertEqual(t, seqEvent.aggregateId, id)
	assertEqual(t, seqEvent.seq, 1)
}

func TestCommitInSequence(t *testing.T) {
	store := inmemoryEeventstore{}
	es := NewEventStream(&store)

	id := AggregateId{1}
	ownerId := OwnerId{2}

	a := account{}
	accountOpenedEvent, err := a.Open(id, ownerId)
	if err != nil {
		t.Error(err)
	}
	es.append(accountOpenedEvent, id)

	depositEvent, err := a.Deposit(42)
	if err != nil {
		t.Error(err)
	}
	es.append(depositEvent, id)

	es.commit()

	assertEqual(t, 0, len(es.uncomittedEvents))
	assertEqual(t, 2, len(store.events))

	secOpenedEvent := store.events[0]
	assertEqual(t, secOpenedEvent.event, accountOpenedEvent)
	assertEqual(t, secOpenedEvent.aggregateId, id)
	assertEqual(t, secOpenedEvent.seq, 1)

	secDepositedEvent := store.events[1]
	assertEqual(t, secDepositedEvent.event, depositEvent)
	assertEqual(t, secDepositedEvent.aggregateId, id)
	assertEqual(t, secDepositedEvent.seq, 2)
}

func assertEqual(t *testing.T, a, b interface{}) {
	if a != b {
		t.Errorf("Expected %v, got %v", b, a)
	}
}
