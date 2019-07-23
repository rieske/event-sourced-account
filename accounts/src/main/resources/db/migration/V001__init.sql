CREATE TABLE Event(
    aggregateId BINARY(16) NOT NULL,
    sequenceNumber BIGINT UNSIGNED NOT NULL,
    transactionId BINARY(16) NOT NULL,
    payload LONGBLOB NOT NULL,
    PRIMARY KEY (aggregateId, sequenceNumber),
    UNIQUE KEY idempotency_constraint(aggregateId, transactionId)
) ENGINE = InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE Snapshot(
    aggregateId BINARY(16) NOT NULL,
    sequenceNumber BIGINT UNSIGNED NOT NULL,
    payload LONGBLOB NOT NULL,
    PRIMARY KEY(aggregateId, sequenceNumber)
) ENGINE = InnoDB DEFAULT CHARSET=utf8;