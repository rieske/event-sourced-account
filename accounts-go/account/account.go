package account

import (
	"errors"
	"fmt"
)

type AggregateId UUID
type OwnerId UUID

type account struct {
	id      *AggregateId
	ownerId *OwnerId
	balance int64
	open    bool
}

type AccountSnapshot struct {
	id      AggregateId
	ownerId OwnerId
	balance int64
	open    bool
}

func NewAccount() *account {
	return &account{}
}

func (a account) Id() AggregateId {
	return *a.id
}

func (a *account) Snapshot() (*AccountSnapshot, error) {
	if !a.open {
		return nil, errors.New("account not open")
	}

	return &AccountSnapshot{*a.id, *a.ownerId, a.balance, a.open}, nil
}

func (a *account) Open(accountId AggregateId, ownerId OwnerId) (Event, error) {
	if a.id != nil || a.ownerId != nil {
		return nil, errors.New("account already open")
	}

	event := AccountOpenedEvent{accountId, ownerId}
	a.applyAccountOpened(event)
	return event, nil
}

func (a *account) Deposit(amount int64) (Event, error) {
	if amount < 0 {
		return nil, errors.New("Can not deposit negative amount")
	}
	if !a.open {
		return nil, errors.New("account not open")
	}
	if amount == 0 {
		return nil, nil
	}

	event := MoneyDepositedEvent{amount, a.balance + amount}
	a.applyMoneyDeposited(event)
	return event, nil
}

func (a *account) applyAccountOpened(event AccountOpenedEvent) {
	a.id = &event.accountId
	a.ownerId = &event.ownerId
	a.balance = 0
	a.open = true
}

func (a *account) applyMoneyDeposited(event MoneyDepositedEvent) {
	a.balance = event.balance
}

type Event interface {
	apply(account *account)
	//Serialize() []byte
}

type AccountOpenedEvent struct {
	accountId AggregateId
	ownerId   OwnerId
}

func (e AccountOpenedEvent) String() string {
	return fmt.Sprintf("AccountOpenedEvent{ownerId: %s}", e.ownerId)
}

func (e AccountOpenedEvent) apply(account *account) {
	account.applyAccountOpened(e)
}

type MoneyDepositedEvent struct {
	amountDeposited int64
	balance         int64
}

func (e MoneyDepositedEvent) apply(account *account) {
	account.applyMoneyDeposited(e)
}

/*func (e AccountOpenedEvent) Serialize() []byte {
	return e.ownerId
}*/
