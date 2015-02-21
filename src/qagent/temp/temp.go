package temp

func FromRaw(raw uint16) float32 {
	return float32(raw) / 100.0
}

func ToRaw(tmp float32) uint16 {
	return uint16(tmp * 100.0)
}
