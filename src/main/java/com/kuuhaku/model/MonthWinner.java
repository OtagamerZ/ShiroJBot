package com.kuuhaku.model;

import com.kuuhaku.utils.ExceedEnums;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
public class MonthWinner {
	@Id
	private int id;
	private final String exceed;
	private final LocalDate expiry;

	public MonthWinner(ExceedEnums ex, LocalDate expiry) {
		this.exceed = ex.getName();
		this.expiry = expiry;
	}

	public String getExceed() {
		return exceed;
	}

	public LocalDate getExpiry() {
		return expiry;
	}
}
