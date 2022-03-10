package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "applieddebuff")
public class AppliedDebuff {
	@Id
	@ManyToOne(fetch = FetchType.EAGER)
	private Debuff debuff;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime expiration = null;

	public AppliedDebuff() {
	}

	public AppliedDebuff(Debuff debuff, long duration) {
		this.debuff = debuff;
		this.expiration = ZonedDateTime.now(ZoneId.of("GMT-3")).plus(duration, ChronoUnit.SECONDS);
	}

	public Debuff getDebuff() {
		return debuff;
	}

	public void setDebuff(Debuff debuff) {
		this.debuff = debuff;
	}

	public ZonedDateTime getExpiration() {
		return expiration;
	}

	public void setExpiration(ZonedDateTime expiration) {
		this.expiration = expiration;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AppliedDebuff that = (AppliedDebuff) o;
		return Objects.equals(debuff, that.debuff);
	}

	@Override
	public int hashCode() {
		return Objects.hash(debuff);
	}
}
