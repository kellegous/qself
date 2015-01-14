package heart

type Stats struct {
	rrs []uint16
	idx int
}

func (r *Stats) AddInterval(i uint16) {
	n, c := len(r.rrs), cap(r.rrs)
	if n < c {
		r.rrs = append(r.rrs, i)
	} else {
		r.rrs[r.idx] = i
		r.idx = (r.idx + 1) % c
	}
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

	if n < cap(r.rrs) {
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

func NewStats(n int) *Stats {
	return &Stats{
		rrs: make([]uint16, 0, n),
		idx: 0,
	}
}
