package main

import (
	"fmt"
	"os"
)

func loadEvents(version int64, accountId UUID) []Event {
	return []Event{}
}

func main() {
	events := loadEvents(10, UUID{})
	fmt.Println(events)

	account := Account{}
	for _, e := range events {
		e.apply(&account)
	}

	event, err := account.Open(AccountId{}, OwnerId{})
	if err != nil {
		os.Exit(1)
	}
	appendEvent(event)
}

func appendEvent(e Event) {
	fmt.Println(e)
}