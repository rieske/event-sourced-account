## Accounts

A simple, frameworkless event sourced Account implementation.


Features:
- events implement the visitor pattern, thus no need for reflective access to apply
them to the aggregate class.
- an in memory event store example is provided in the test source tree, 
stressing the required consistency guarantees that a real event store should provide.
- optional pluggable snapshotting

