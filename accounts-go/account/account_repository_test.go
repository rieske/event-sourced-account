package account

import "testing"

func TestAccountRepository_Open(t *testing.T) {
	store := inmemoryEeventstore{}
	repo := NewAccountRepository(&store)

	id := AggregateId{1}
	ownerId := OwnerId{2}
	err := repo.Open(id, ownerId)
	expectNoError(t, err)
}

func TestAccountRepository_CanNotOpenDuplicateAccount(t *testing.T) {
	store := inmemoryEeventstore{}
	repo := NewAccountRepository(&store)

	id := AggregateId{1}
	ownerId := OwnerId{2}
	err := repo.Open(id, ownerId)
	expectNoError(t, err)

	err = repo.Open(id, ownerId)
	// FIXME: we want duplicate account error here
	expectError(t, err, "Concurrent modification error")
}

func TestAccountRepository_CanOpenDistinctAccounts(t *testing.T) {
	store := inmemoryEeventstore{}
	repo := NewAccountRepository(&store)

	ownerId := OwnerId{2}
	err := repo.Open(AggregateId{1}, ownerId)
	expectNoError(t, err)

	err = repo.Open(AggregateId{2}, ownerId)
	expectNoError(t, err)
}

func TestAccountRepository_CanNotDepositWhenNoAccountExists(t *testing.T) {
	store := inmemoryEeventstore{}
	repo := NewAccountRepository(&store)

	id := AggregateId{1}
	err := repo.Deposit(id, 42)
	expectError(t, err, "Aggregate not found")
}

func TestAccountRepository_Deposit(t *testing.T) {
	store := inmemoryEeventstore{}
	repo := NewAccountRepository(&store)

	id := AggregateId{1}
	ownerId := OwnerId{2}
	err := repo.Open(id, ownerId)
	expectNoError(t, err)

	err = repo.Deposit(id, 42)
	expectNoError(t, err)
}
