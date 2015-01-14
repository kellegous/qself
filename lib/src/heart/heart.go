package heart

type Rates struct {
	rrs []uint16
	idx int
}

func (r *Rates) AddInterval(i uint16) {
	n, c := len(r.rrs), cap(r.rrs)
	if n < c {
		r.rrs = append(r.rrs, i)
	} else {
		r.rrs[r.idx] = i
		r.idx = (r.idx + 1) % c
	}
}

func (r *Rates) Reset() {
	r.rrs = r.rrs[0:0]
	r.idx = 0
}

func (r *Rates) Len() int {
	return len(r.rrs)
}

func NewRates(n int) *Rates {
	return &Rates{
		rrs: make([]uint16, 0, n),
		idx: 0,
	}
}
