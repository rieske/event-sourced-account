package account

import (
	"errors"
	"fmt"
)

type Account struct {
	id      *uuid
	ownerId *uuid
	balance int64
	open    bool
}

func (a *Account) Open(accountId, ownerId uuid) (Event, error) {
	if a.id != nil || a.ownerId != nil {
		return nil, errors.New("Account already open")
	}

	event := AccountOpenedEvent{accountId, ownerId}
	a.applyAccountOpened(event)
	return event, nil
}

func (a *Account) Deposit(amount int64) (Event, error) {
	if amount < 0 {
		return nil, errors.New("Can not deposit negative amount")
	}
	if !a.open {
		return nil, errors.New("Account not open")
	}
	if amount == 0 {
		return nil, nil
	}

	event := MoneyDepositedEvent{amount, a.balance + amount}
	a.applyMoneyDeposited(event)
	return event, nil
}

func (a *Account) applyAccountOpened(event AccountOpenedEvent) {
	a.id = &event.accountId
	a.ownerId = &event.ownerId
	a.balance = 0
	a.open = true
}

func (a *Account) applyMoneyDeposited(event MoneyDepositedEvent) {
	a.balance = event.balance
}

type Event interface {
	//Serialize() []byte
}

type AccountOpenedEvent struct {
	accountId uuid
	ownerId   uuid
}

type MoneyDepositedEvent struct {
	amountDeposited int64
	balance         int64
}

func (e AccountOpenedEvent) String() string {
	return fmt.Sprintf("AccountOpenedEvent{ownerId: %s}", e.ownerId)
}

/*func (e AccountOpenedEvent) Serialize() []byte {
	return e.ownerId
}*/
