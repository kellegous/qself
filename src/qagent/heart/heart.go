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

func (r *Stats) Hr() float64 {
	if len(r.rrs) < r.min {
		return -1.0
	}

	return Hr(r.rrs)
}

func Hr(rrs []uint16) float64 {
	s := 0.0
	n := len(rrs)
	for i := 0; i < n; i++ {
		s += float64(rrs[i])
	}
	return 60000.0 / (s / float64(n))
}

func HrvRmssd(rrs []uint16) float64 {
	s := 0.0
	n := len(rrs)
	for i := 1; i < n; i++ {
		d := rrs[i] - rrs[i-1]
		s += float64(d * d)
	}
	return math.Sqrt(s / float64(n-1))
}

func HrvLnRmssd20(rrs []uint16) float64 {
	return math.Log(HrvRmssd(rrs)) * 20
}

func (r *Stats) HrvLnRmssd20() float64 {
	if len(r.rrs) < r.min {
		return -1.0
	}
	return HrvLnRmssd20(r.rrs)
}

func (r *Stats) HrvRmssd() float64 {
	if len(r.rrs) < r.min {
		return -1.0
	}

	return HrvRmssd(r.rrs)
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
