package account

import (
	"fmt"
	"testing"
)

func TestOpenAccount(t *testing.T) {
	a := Account{}

	accountId := uuid{1}
	ownerId := uuid{42}
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

	accountId := uuid{1}
	ownerId := uuid{42}
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

	accountId := uuid{1}
	ownerId := uuid{42}
	_, _ = a.Open(accountId, ownerId)

	event, err := a.deposit(42)
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

	accountId := uuid{1}
	ownerId := uuid{42}
	_, _ = a.Open(accountId, ownerId)

	_, _ = a.deposit(1)
	_, _ = a.deposit(2)

	if a.balance != 3 {
		t.Error("balance should be accumulated")
	}
}