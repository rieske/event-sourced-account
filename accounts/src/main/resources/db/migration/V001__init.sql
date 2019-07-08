CREATE TABLE Event(
    aggregateId BINARY(16) NOT NULL,
    sequenceNumber BIGINT UNSIGNED NOT NULL,
    payload LONGBLOB NOT NULL,
    PRIMARY KEY(aggregateId, sequenceNumber)
) ENGINE = InnoDB DEFAULT CHARSET=utf8;