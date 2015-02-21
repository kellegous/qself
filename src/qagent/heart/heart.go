package heart

import "math"

type Stats struct {
	rrs []uint16
	min int
	idx int
}

func (r *Stats) CanReport() bool {
	return len(r.rrs) >= r.min
}

func (r *Stats) AddInterval(i uint16) bool {
	n, c := len(r.rrs), cap(r.rrs)
	if n < c {
		r.rrs = append(r.rrs, i)
	} else {
		r.rrs[r.idx] = i
		r.idx = (r.idx + 1) % c
	}

	return len(r.rrs) >= r.min
}

func (r *Stats) Reset() {
	r.rrs = r.rrs[0:0]
	r.idx = 0
}

func (r *Stats) len() int {
	return len(r.rrs)
}

func (r *Stats) Hr() float32 {
	n := len(r.rrs)

	if n < r.min {
		return 0.0
	}

	var s float32
	for i := 0; i < n; i++ {
		s += float32(r.rrs[i])
	}

	if s <= 0.0 {
		return 0.0
	}

	return 60000.0 / (s / float32(n))
}

func (r *Stats) HrvLnRmssd20() float64 {
	return math.Log(r.HrvRmssd()) * 20.0
}

func (r *Stats) HrvRmssd() float64 {
	n := len(r.rrs)

	if n < r.min {
		return -1.0
	}

	s := 0.0
	for i := 1; i < n; i++ {
		d := r.rrs[i] - r.rrs[i-1]
		s += float64(d * d)
	}

	return math.Sqrt(s / float64(n-1))
}

func (r *Stats) HrvPnn20() float64 {
	n := len(r.rrs)
	if n < r.min {
		return -1.0
	}

	c := 0
	for i := 1; i < n; i++ {
		d := math.Abs(float64(r.rrs[i] - r.rrs[i-1]))
		if d > 20 {
			c++
		}
	}

	return float64(c) / float64(n-1)
}

func NewStats(min, n int) *Stats {
	return &Stats{
		rrs: make([]uint16, 0, n),
		idx: 0,
		min: min,
	}
}
