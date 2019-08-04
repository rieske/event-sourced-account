package account

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

func (es *inmemoryEeventstore) Append(events []sequencedEvent) {
	for _, e := range events {
		es.events = append(es.events, e)
	}
}
