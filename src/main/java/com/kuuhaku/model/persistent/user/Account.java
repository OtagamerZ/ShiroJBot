/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Blacklistable;
import com.kuuhaku.interfaces.annotations.WhenNull;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Role;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.persistent.converter.RoleFlagConverter;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.collections4.bag.HashBag;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Entity
@DynamicUpdate
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "account", indexes = @Index(columnList = "balance DESC"))
public class Account extends DAO<Account> implements Blacklistable {
	@Id
	@Column(name = "uid", nullable = false)
	private String uid;

	@Column(name = "name")
	private String name;

	@Column(name = "role", nullable = false)
	@Convert(converter = RoleFlagConverter.class)
	private EnumSet<Role> roles = EnumSet.noneOf(Role.class);

	@Column(name = "balance", nullable = false)
	private long balance;

	@Column(name = "debit", nullable = false)
	private long debit;

	@Column(name = "gems", nullable = false)
	private int gems;

	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private List<Profile> profiles = new ArrayList<>();

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "uid")
	@Fetch(FetchMode.JOIN)
	private AccountSettings settings;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "uid")
	@Fetch(FetchMode.JOIN)
	private Kawaipon kawaipon;

	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderColumn(name = "index")
	@Fetch(FetchMode.SUBSELECT)
	private List<Deck> decks = new ArrayList<>();

	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private List<Transaction> transactions = new ArrayList<>();

	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private Set<DynamicProperty> dynamicProperties = new LinkedHashSet<>();

	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private Set<AccountTitle> titles = new HashSet<>();

	@Type(JsonBinaryType.class)
	@Column(name = "inventory", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject inventory = new JSONObject();

	@Column(name = "blacklisted", nullable = false)
	private boolean blacklisted;

	@Column(name = "created_at", nullable = false)
	private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("GMT-3"));

	@Column(name = "last_daily")
	private ZonedDateTime lastDaily;

	@Column(name = "last_vote")
	private ZonedDateTime lastVote;

	@Column(name = "vote_streak", nullable = false)
	private int voteStreak;

	public Account() {
	}

	@WhenNull
	public Account(String uid) {
		this.uid = uid;
		this.settings = new AccountSettings(uid);
		this.kawaipon = new Kawaipon(this);
	}

	public String getUid() {
		return uid;
	}

	public User getUser() {
		return Main.getApp().getUserById(uid);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean hasRole(Role role) {
		return Utils.containsAny(roles, Role.DEVELOPER, role);
	}

	public long getBalance() {
		return balance;
	}

	protected void setBalance(long balance) {
		this.balance = balance;
	}

	public long getDebit() {
		return debit;
	}

	protected void setDebit(long debit) {
		this.debit = debit;
	}

	public void addCR(long value, String reason) {
		if (value <= 0) return;

		apply(this.getClass(), uid, a -> {
			a.setDebit(a.getDebit() - value);
			if (a.getDebit() < 0) {
				a.setBalance(-a.getDebit() + a.getBalance());
				a.setDebit(0);
			}

			a.addTransaction(value, true, reason, Currency.CR);
		});
	}

	public void consumeCR(long value, String reason) {
		if (value <= 0) return;

		apply(this.getClass(), uid, a -> {
			a.setBalance(a.getBalance() - value);
			if (a.getBalance() < 0) {
				a.setDebit(-a.getBalance() + a.getDebit());
				a.setBalance(0);
			}

			a.addTransaction(value, false, reason, Currency.CR);
		});
	}

	public int getGems() {
		return gems;
	}

	protected void setGems(int gems) {
		this.gems = gems;
	}

	public void addGems(int value, String reason) {
		if (value <= 0) return;

		apply(this.getClass(), uid, a -> {
			a.setGems(a.getGems() + value);
			a.addTransaction(value, true, reason, Currency.GEM);
		});
	}

	public void consumeGems(int value, String reason) {
		if (value <= 0) return;

		apply(this.getClass(), uid, a -> {
			a.setGems(a.getGems() - value);
			a.addTransaction(value, false, reason, Currency.GEM);
		});
	}

	public boolean hasEnough(int value, Currency currency) {
		if (value == 0) return true;

		return switch (currency) {
			case CR -> getBalance() - getDebit() >= value;
			case GEM -> getGems() >= value;
		};
	}

	public List<Profile> getProfiles() {
		return profiles;
	}

	public Profile getProfile(Member member) {
		return profiles.parallelStream()
				.filter(p -> p.getId().getGid().equals(member.getGuild().getId()))
				.findFirst().orElse(new Profile(member));
	}

	public void addProfile(Member member) {
		profiles.add(new Profile(member));
	}

	public AccountSettings getSettings() {
		return Utils.getOr(settings, DAO.find(AccountSettings.class, uid));
	}

	public Kawaipon getKawaipon() {
		return Utils.getOr(kawaipon, DAO.find(Kawaipon.class, uid));
	}

	public List<Deck> getDecks() {
		boolean update = false;
		while (decks.size() < settings.getDeckCapacity()) {
			Deck d = new Deck(this);
			if (decks.isEmpty()) {
				d.setCurrent(true);
			}

			decks.add(d);
			update = true;
		}

		if (update) save();
		return decks;
	}

	public Deck getCurrentDeck() {
		return getDecks().parallelStream()
				.filter(Deck::isCurrent)
				.findFirst().orElse(null);
	}

	public List<Transaction> getTransactions() {
		return transactions;
	}

	public void addTransaction(long value, boolean input, String reason, Currency currency) {
		transactions.add(new Transaction(this, value, input, reason, currency));
	}

	public Set<DynamicProperty> getDynamicProperties() {
		return dynamicProperties;
	}

	public DynamicProperty getDynamicProperty(String id) {
		return dynamicProperties.parallelStream()
				.filter(dp -> dp.getId().getId().equals(id))
				.findFirst().orElse(new DynamicProperty(this, id, ""));
	}

	public String getDynValue(String id) {
		return getDynValue(id, "");
	}

	public String getDynValue(String id, String defaultValue) {
		return dynamicProperties.parallelStream()
				.filter(dp -> dp.getId().getId().equals(id))
				.map(DynamicProperty::getValue)
				.findFirst().orElse(defaultValue);
	}

	public AccountTitle getTitle() {
		return titles.parallelStream()
				.filter(AccountTitle::isCurrent)
				.findFirst().orElse(null);
	}

	public synchronized Title checkTitles() {
		List<Title> titles = DAO.queryAll(Title.class, """
				SELECT t
				FROM Title t
				LEFT JOIN AccountTitle at ON at.title.id = t.id AND at.account.uid = ?1
				WHERE at IS NULL
				AND COALESCE(t.condition, '') <> ''
				""", uid
		);

		for (Title title : titles) {
			if (title.check(this)) {
				this.titles.add(new AccountTitle(this, title));
				return title;
			}
		}

		return null;
	}

	public Set<AccountTitle> getTitles() {
		return titles;
	}

	public boolean hasTitle(String title) {
		return titles.parallelStream()
				.map(AccountTitle::getTitle)
				.anyMatch(t -> t.getId().equals(title));
	}

	public boolean addTitle(String title) {
		return titles.add(new AccountTitle(this, DAO.find(Title.class, title)));
	}

	public JSONObject getInventory() {
		return inventory;
	}

	public HashBag<UserItem> getItems() {
		HashBag<UserItem> items = new HashBag<>();
		for (Map.Entry<String, Object> e : inventory.entrySet()) {
			UserItem ui = DAO.find(UserItem.class, e.getKey());
			if (ui != null) {
				JSONObject info = new JSONObject(e.getValue());
				items.add(ui, info.getInt("count"));
			}
		}

		return items;
	}

	@Override
	public boolean isBlacklisted() {
		return blacklisted;
	}

	public void setBlacklisted(boolean blacklisted) {
		this.blacklisted = blacklisted;
	}

	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	public boolean isOldUser() {
		ZonedDateTime old = ZonedDateTime.of(LocalDateTime.of(2022, 2, 12, 0, 0), ZoneId.of("GMT-3"));
		return createdAt.isBefore(old);
	}

	public ZonedDateTime getLastDaily() {
		return lastDaily;
	}

	public long collectDaily() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-3"));

		if (lastDaily != null && now.isBefore(lastDaily.plusDays(1))) {
			return now.until(lastDaily.plusDays(1), ChronoUnit.MILLIS);
		} else if (hasEnough(100000, Currency.CR)) {
			return -1;
		}

		lastDaily = now;
		save();

		addCR(10000, "Daily");

		return 0;
	}

	public ZonedDateTime getLastVote() {
		return lastVote;
	}

	public void addVote() {
		voteStreak = getStreak() + 1;
		lastVote = ZonedDateTime.now(ZoneId.of("GMT-3"));
	}

	public boolean voted() {
		if (lastVote == null) return false;
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-3"));

		return now.isBefore(lastVote.plus(12, ChronoUnit.HOURS));
	}

	public int getStreak() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-3"));
		if (lastVote != null && now.isAfter(lastVote.plus(25, ChronoUnit.HOURS))) {
			voteStreak = 0;
		}

		return voteStreak;
	}

	public int getRanking() {
		return DAO.queryNative(Integer.class, """
				SELECT x.rank
				FROM (
				     SELECT p.uid
				          , rank() OVER (ORDER BY p.xp DESC)
				     FROM profile p
				     ) x
				WHERE x.uid = ?1
				""", uid);
	}

	public int getHighestLevel() {
		return DAO.queryNative(Integer.class, "SELECT CAST(SQRT(MAX(xp) / 100) AS INT) FROM profile WHERE uid = ?1", uid);
	}

	public I18N getEstimateLocale() {
		return I18N.valueOf(Utils.getOr(
				DAO.queryNative(String.class,
						"""
								SELECT gc.locale
								FROM guild_config gc
								INNER JOIN profile p ON gc.gid = p.gid
								WHERE p.uid = ?1
								GROUP BY gc.locale
								ORDER BY COUNT(gc.locale) DESC
								""", uid
				), "PT"
		));
	}

	public Couple getCouple() {
		return DAO.query(Couple.class, "SELECT c FROM Couple c WHERE ?1 = c.id.first OR ?1 = c.id.second", uid);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Account account = (Account) o;
		return Objects.equals(uid, account.uid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uid);
	}
}
