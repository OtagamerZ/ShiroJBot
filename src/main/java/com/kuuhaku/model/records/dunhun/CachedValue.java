package com.kuuhaku.model.records.dunhun;

public record CachedValue(double flat, double inc, double mult) {
	public double apply(double base) {
		return (base + flat) * (1 + inc) * mult;
	}
}
