package com.kuuhaku.model.persistent;

import javax.persistence.*;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "reminder")
public class Reminder {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String description;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean repeating;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long period;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime nextReminder;

	public Reminder() {
	}

	public Reminder(String uid, String description, int time, ChronoUnit unit, boolean repeating) {
		this.uid = uid;
		this.description = description;
		this.period = Duration.of(time, unit).toMillis();
		this.nextReminder = ZonedDateTime.now(ZoneId.of("GMT-3")).plus(period, ChronoUnit.MILLIS);
		this.repeating = repeating;
	}

	public Reminder(String uid, String description, long period, boolean repeating) {
		this.uid = uid;
		this.description = description;
		this.period =period;
		this.nextReminder = ZonedDateTime.now(ZoneId.of("GMT-3")).plus(period, ChronoUnit.MILLIS);
		this.repeating = repeating;
	}

	public String getUid() {
		return uid;
	}

	public String getDescription() {
		return description;
	}

	public boolean isRepeating() {
		return repeating;
	}

	public long getPeriod() {
		return period;
	}

	public ZonedDateTime getNextReminder() {
		return nextReminder;
	}

	public void scheduleNext() {
		nextReminder = nextReminder.plus(period, ChronoUnit.MILLIS);
	}

	public boolean expired() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-3"));

		return now.isAfter(nextReminder);
	}
}
