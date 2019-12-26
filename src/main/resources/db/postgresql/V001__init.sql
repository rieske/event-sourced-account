CREATE TABLE Event(
    aggregateId UUID NOT NULL,
    sequenceNumber BIGINT NOT NULL,
    transactionId UUID NOT NULL,
    payload BYTEA NOT NULL,
    PRIMARY KEY (aggregateId, sequenceNumber)
);

CREATE TABLE Snapshot(
    aggregateId UUID NOT NULL,
    sequenceNumber BIGINT NOT NULL,
    payload BYTEA NOT NULL,
    PRIMARY KEY(aggregateId)
);

CREATE TABLE Transaction(
    aggregateId UUID NOT NULL,
    transactionId UUID NOT NULL,
    PRIMARY KEY (aggregateId, transactionId)
);