package account

import "errors"

type inmemoryEeventstore struct {
	events []sequencedEvent
}

func (es *inmemoryEeventstore) Events(id AggregateId, version int) []Event {
	events := []Event{}
	for _, e := range es.events {
		if e.aggregateId == id {
			events = append(events, e.event)
		}
	}
	return events
}

func (es *inmemoryEeventstore) Append(events []sequencedEvent) error {
	for _, e := range events {
		if e.seq <= es.latestVersion(e.aggregateId) {
			return errors.New("Concurrent modification error")
		}

		es.events = append(es.events, e)
	}
	return nil
}

func (es *inmemoryEeventstore) latestVersion(id AggregateId) int {
	aggVersions := map[AggregateId]int{}
	for _, e := range es.events {
		aggVersions[e.aggregateId] = e.seq
	}
	return aggVersions[id]
}
