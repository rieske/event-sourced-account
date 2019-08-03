package main

import "errors"

type eventStore interface {
	getEvents(id AccountId, version int64) []Event
}

type eventStream struct {
	eventStore *eventStore
	versions   map[AccountId]int64
}

func (stream *eventStream) replay(a *Account, id AccountId) error {
	events := (*stream.eventStore).getEvents(id, 0)
	var currentVersion int64 = 0
	for _, e := range events {
		e.apply(a)
		currentVersion += 1
	}

	if currentVersion != 0 {
		return errors.New("Aggregate not found")
	}

	stream.versions[id] = currentVersion
	return nil
}
