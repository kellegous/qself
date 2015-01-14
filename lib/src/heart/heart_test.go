package heart

import (
	"testing"
)

func TestCircularityOfRate(t *testing.T) {
	r := NewRates(8)
	for i := 0; i < 8; i++ {
		if r.Len() != i {
			t.Errorf("expected length of %d, got %d", i, r.Len())
		}
		r.AddInterval(uint16(i * 100))
	}

	for i := 0; i < 16; i++ {
		if r.Len() != 8 {
			t.Errorf("expected length of 8, got %d", r.Len())
		}
		r.AddInterval(uint16(i * 100))
	}
}
