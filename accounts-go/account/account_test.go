package account

import (
	"testing"
)

func TestOpenAccount(t *testing.T) {
	a := account{}

	accountId := AggregateId{1}
	ownerId := OwnerId{42}
	event, err := a.Open(accountId, ownerId)
	if err != nil {
		t.Error(err)
	}

	expectEvent(t, event)
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
	a := account{}

	accountId := AggregateId{1}
	ownerId := OwnerId{42}
	_, _ = a.Open(accountId, ownerId)
	event, err := a.Open(accountId, ownerId)
	expectError(t, err, "account already open")
	expectNoEvent(t, event)
}

func TestDeposit(t *testing.T) {
	a := account{}

	accountId := AggregateId{1}
	ownerId := OwnerId{42}
	_, _ = a.Open(accountId, ownerId)

	event, err := a.Deposit(42)
	if err != nil {
		t.Error(err)
	}

	expectEvent(t, event)
	expectBalance(t, a, 42)
}

func TestDepositAccumulatesBalance(t *testing.T) {
	a := account{}

	accountId := AggregateId{1}
	ownerId := OwnerId{42}
	_, _ = a.Open(accountId, ownerId)

	_, _ = a.Deposit(1)
	_, _ = a.Deposit(2)

	expectBalance(t, a, 3)
}

func TestCanNotDepositNegativeAmount(t *testing.T) {
	a := account{}

	accountId := AggregateId{1}
	ownerId := OwnerId{42}
	_, _ = a.Open(accountId, ownerId)

	_, err := a.Deposit(-1)

	expectError(t, err, "Can not deposit negative amount")
	expectBalance(t, a, 0)
}

func TestZeroDepositShouldNotEmitEvent(t *testing.T) {
	a := account{}

	accountId := AggregateId{1}
	ownerId := OwnerId{42}
	_, _ = a.Open(accountId, ownerId)

	event, err := a.Deposit(0)

	expectNoError(t, err)
	expectNoEvent(t, event)
}

func TestShouldRequireOpenAccountForDeposit(t *testing.T) {
	a := account{}

	event, err := a.Deposit(0)

	expectError(t, err, "account not open")
	expectNoEvent(t, event)
}

func TestShouldApplyEvents(t *testing.T) {
	a := account{}

	accountId := AggregateId{1}
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

func expectError(t *testing.T, err error, message string) {
	if err == nil || err.Error() != message {
		t.Errorf("error expected - %s", message)
	}
}

func expectNoError(t *testing.T, err error) {
	if err != nil {
		t.Error("no error expected, got:", err)
	}
}

func expectEvent(t *testing.T, event Event) {
	if event == nil {
		t.Error("event expected")
	}
}

func expectNoEvent(t *testing.T, event Event) {
	if event != nil {
		t.Error("no event expected")
	}
}

func expectBalance(t *testing.T, a account, balance int64) {
	if a.balance != balance {
		t.Errorf("balance should be %d", balance)
	}
}
