package temp

func FromRaw(raw uint16) float64 {
	return float64(raw) / 100.0
}

func ToRaw(tmp float64) uint16 {
	return uint16(tmp * 100.0)
}

func Average(vals []uint16) float64 {
	s := 0.0
	n := len(vals)
	for i := 0; i < n; i++ {
		s += FromRaw(vals[i])
	}
	return s / float64(n)
}
