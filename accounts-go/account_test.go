package main

import (
	"fmt"
	"testing"
)

func TestOpenAccount(t *testing.T) {
	a := Account{}

	accountId := AccountId{1}
	ownerId := OwnerId{42}
	event, err := a.Open(accountId, ownerId)
	if err != nil {
		t.Error(err)
	}

	fmt.Println(event)

	if event == nil {
		t.Error("event expected")
	}
	if *a.id != accountId {
		t.Error("account id should be set")
	}
	if *a.ownerId != ownerId {
		t.Error("owner id should be set")
	}
	if a.open != true {
		t.Error("account should be open")
	}
	expectBalance(t, a, 0)
}

func TestOpenAccountAlreadyOpen(t *testing.T) {
	a := Account{}

	accountId := AccountId{1}
	ownerId := OwnerId{42}
	_, _ = a.Open(accountId, ownerId)
	event, err := a.Open(accountId, ownerId)
	if err == nil || err.Error() != "Account already open" {
		t.Error("account already open expected")
	}
	if event != nil {
		t.Error("event not expected")
	}
}

func TestDeposit(t *testing.T) {
	a := Account{}

	accountId := AccountId{1}
	ownerId := OwnerId{42}
	_, _ = a.Open(accountId, ownerId)

	event, err := a.Deposit(42)
	if err != nil {
		t.Error(err)
	}

	if event == nil {
		t.Error("event expected")
	}
	expectBalance(t, a, 42)
}

func TestDepositAccumulatesBalance(t *testing.T) {
	a := Account{}

	accountId := AccountId{1}
	ownerId := OwnerId{42}
	_, _ = a.Open(accountId, ownerId)

	_, _ = a.Deposit(1)
	_, _ = a.Deposit(2)

	expectBalance(t, a, 3)
}

func TestCanNotDepositNegativeAmount(t *testing.T) {
	a := Account{}

	accountId := AccountId{1}
	ownerId := OwnerId{42}
	_, _ = a.Open(accountId, ownerId)

	_, err := a.Deposit(-1)

	if err == nil || err.Error() != "Can not deposit negative amount" {
		t.Error("error expected - can not deposit negative amount")
	}
	expectBalance(t, a, 0)
}

func TestZeroDepositShouldNotEmitEvent(t *testing.T) {
	a := Account{}

	accountId := AccountId{1}
	ownerId := OwnerId{42}
	_, _ = a.Open(accountId, ownerId)

	event, err := a.Deposit(0)

	if err != nil {
		t.Error("no error expected")
	}
	if event != nil {
		t.Error("no event expected")
	}
}

func TestShouldRequireOpenAccountForDeposit(t *testing.T) {
	a := Account{}

	event, err := a.Deposit(0)

	if err == nil || err.Error() != "Account not open" {
		t.Error("error expected - can not deposit to not open account")
	}
	if event != nil {
		t.Error("no event expected")
	}
}

func TestShouldApplyEvents(t *testing.T) {
	a := Account{}

	accountId := AccountId{1}
	ownerId := OwnerId{42}

	events := []Event{
		AccountOpenedEvent{accountId, ownerId},
		MoneyDepositedEvent{1, 1},
		MoneyDepositedEvent{2, 3},
	}

	for _, e := range events {
		e.apply(&a)
	}

	if *a.id != accountId {
		t.Error("account id should be set")
	}
	if *a.ownerId != ownerId {
		t.Error("owner id should be set")
	}
	if a.open != true {
		t.Error("account should be open")
	}
	expectBalance(t, a, 3)
}

func expectBalance(t *testing.T, a Account, balance int64) {
	if a.balance != balance {
		t.Errorf("balance should be %d", balance)
	}
}
