package heart

import (
	"math"
	"testing"
)

func closeEnough(a, b, thresh float32) bool {
	return math.Abs(float64(a-b)) < float64(thresh)
}

func TestCircularityOfRate(t *testing.T) {
	r := NewStats(8, 8)
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

func TestHr(t *testing.T) {
	r := NewStats(4, 4)
	for i := 0; i < 4; i++ {
		r.AddInterval(1000)
	}

	if !closeEnough(60.0, r.Hr(), 0.01) {
		t.Errorf("expected hr of 60.0, got %0.2f", r.Hr())
	}
}
