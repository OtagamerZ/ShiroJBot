package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.kuuhaku.controller.postgresql.DebuffDAO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "applieddebuff")
public class AppliedDebuff {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String debuff;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime expiration = null;

	public AppliedDebuff() {
	}

	public AppliedDebuff(Debuff debuff, long duration) {
		this.debuff = debuff.getId();
		this.expiration = ZonedDateTime.now(ZoneId.of("GMT-3")).plus(duration, ChronoUnit.SECONDS);
	}

	public Debuff getDebuff() {
		return DebuffDAO.getDebuff(debuff);
	}

	public ZonedDateTime getExpiration() {
		return expiration;
	}

	public void setExpiration(ZonedDateTime expiration) {
		this.expiration = expiration;
	}

	public boolean expired() {
		return ZonedDateTime.now(ZoneId.of("GMT-3")).isAfter(expiration);
	}
}
