package account

import "fmt"

type UUID [16]byte

func (b UUID) String() string {
	return fmt.Sprintf("%X-%X-%X-%X-%X", b[0:4], b[4:6], b[6:8], b[8:10], b[10:])
}
