package main

import (
	"errors"
	"fmt"
)

type AccountId UUID
type OwnerId UUID

type Account struct {
	id      *AccountId
	ownerId *OwnerId
	balance int64
	open    bool
}

func (a *Account) Open(accountId AccountId, ownerId OwnerId) (Event, error) {
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
	apply(account *Account)
	//Serialize() []byte
}

type AccountOpenedEvent struct {
	accountId AccountId
	ownerId   OwnerId
}

func (e AccountOpenedEvent) String() string {
	return fmt.Sprintf("AccountOpenedEvent{ownerId: %s}", e.ownerId)
}

func (e AccountOpenedEvent) apply(account *Account) {
	account.applyAccountOpened(e)
}

type MoneyDepositedEvent struct {
	amountDeposited int64
	balance         int64
}

func (e MoneyDepositedEvent) apply(account *Account) {
	account.applyMoneyDeposited(e)
}

/*func (e AccountOpenedEvent) Serialize() []byte {
	return e.ownerId
}*/
