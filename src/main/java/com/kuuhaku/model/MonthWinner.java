package com.kuuhaku.model;

import com.kuuhaku.utils.ExceedEnums;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
public class MonthWinner {
	@Id
	private int id;
	private String exceed;
	private final LocalDate expiry = LocalDate.now().plusWeeks(1);

	public String getExceed() {
		return exceed;
	}

	public void setExceed(String exceed) {
		this.exceed = exceed;
	}

	public LocalDate getExpiry() {
		return expiry;
	}
}
