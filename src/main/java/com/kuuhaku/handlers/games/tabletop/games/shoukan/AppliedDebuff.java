package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.kuuhaku.controller.postgresql.DebuffDAO;
import com.kuuhaku.model.persistent.id.CompositeDebuffId;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "applieddebuff")
@IdClass(CompositeDebuffId.class)
public class AppliedDebuff {
	@Id
	@Column(columnDefinition = "INT NOT NULL")
	private int hero;

	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String debuff;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime expiration = null;

	public AppliedDebuff() {
	}

	public AppliedDebuff(Hero hero, Debuff debuff, long duration) {
		this.hero = hero.getId();
		this.uid = hero.getUid();
		this.debuff = debuff.getId();
		this.expiration = ZonedDateTime.now(ZoneId.of("GMT-3")).plus(duration, ChronoUnit.SECONDS);
	}

	public int getHero() {
		return hero;
	}

	public String getUid() {
		return uid;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AppliedDebuff that = (AppliedDebuff) o;
		return hero == that.hero && Object.equals(uid, that.uid) && Objects.equals(debuff, that.debuff);
	}

	@Override
	public int hashCode() {
		return Objects.hash(hero, uid, debuff);
	}
}
