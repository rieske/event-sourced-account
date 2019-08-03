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
	if a.balance != 0 {
		t.Error("balance should be zero")
	}
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
	if a.balance != 42 {
		t.Error("balance should be set")
	}
}

func TestDepositAccumulatesBalance(t *testing.T) {
	a := Account{}

	accountId := AccountId{1}
	ownerId := OwnerId{42}
	_, _ = a.Open(accountId, ownerId)

	_, _ = a.Deposit(1)
	_, _ = a.Deposit(2)

	if a.balance != 3 {
		t.Error("balance should be accumulated")
	}
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
	if a.balance != 0 {
		t.Error("balance should be zero")
	}
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
