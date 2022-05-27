package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.kuuhaku.controller.postgresql.DebuffDAO;
import com.kuuhaku.model.persistent.id.AppliedDebuffId;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "applieddebuff")
@IdClass(AppliedDebuffId.class)
public class AppliedDebuff {
	@Id
	@Column(columnDefinition = "INT NOT NULL")
	private int heroId;

	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String heroUid;

	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String debuff;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime expiration = null;

	public AppliedDebuff() {
	}

	public AppliedDebuff(Hero hero, Debuff debuff, long duration) {
		this.heroId = hero.getId();
		this.heroUid = hero.getUid();
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
