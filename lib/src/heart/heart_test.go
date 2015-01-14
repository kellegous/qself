package heart

import (
	"testing"
)

func TestCircularityOfRate(t *testing.T) {
	r := NewStats(8)
	for i := 0; i < 8; i++ {
		if r.len() != i {
			t.Errorf("expected length of %d, got %d", i, r.len())
		}
		r.AddInterval(uint16(i * 100))
	}

	for i := 0; i < 16; i++ {
		if r.len() != 8 {
			t.Errorf("expected length of 8, got %d", r.len())
		}
		r.AddInterval(uint16(i * 100))
	}
}
