CREATE TABLE Event(
    aggregateId BINARY(16) NOT NULL,
    sequenceNumber BIGINT NOT NULL,
    transactionId BINARY(16) NOT NULL,
    payload BLOB NOT NULL,
    PRIMARY KEY (aggregateId, sequenceNumber),
    INDEX (aggregateId, transactionId)
) ENGINE = InnoDB DEFAULT CHARSET=UTF8MB4;

CREATE TABLE Snapshot(
    aggregateId BINARY(16) NOT NULL,
    sequenceNumber BIGINT NOT NULL,
    payload BLOB NOT NULL,
    PRIMARY KEY(aggregateId)
) ENGINE = InnoDB DEFAULT CHARSET=UTF8MB4;
