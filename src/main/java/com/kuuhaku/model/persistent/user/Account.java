/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.AutoMake;
import com.kuuhaku.interfaces.Blacklistable;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Role;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.persistent.converter.RoleFlagConverter;
import com.kuuhaku.model.persistent.shoukan.DailyDeck;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.MatchHistory;
import com.kuuhaku.model.records.id.DynamicPropertyId;
import com.kuuhaku.model.records.id.ProfileId;
import com.kuuhaku.util.Bit32;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "account", indexes = @Index(columnList = "balance DESC"))
public class Account extends DAO<Account> implements AutoMake<Account>, Blacklistable {
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

	@OneToOne(cascade = ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "uid")
	@Fetch(FetchMode.JOIN)
	private AccountSettings settings;

	@OneToMany(mappedBy = "account", cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	private Set<Deck> decks = new HashSet<>();

	@OneToMany(mappedBy = "account", cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	private Set<AccountTitle> titles = new HashSet<>();

	@JdbcTypeCode(SqlTypes.JSON)
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

	@Column(name = "vote_awarded", nullable = false)
	private boolean voteAwarded = true;

	@Column(name = "last_transfer")
	private ZonedDateTime lastTransfer;

	@Column(name = "vote_streak", nullable = false)
	private int voteStreak;

	@Override
	public Account make(JSONObject args) {
		this.uid = args.getString("uid");
		return this;
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

	public boolean hasRole(Role... roles) {
		return Utils.containsAny(this.roles, roles);
	}

	public static boolean hasRole(String uid, boolean and, Role... roles) {
		int flags = 0;
		for (Role r : roles) {
			flags = Bit32.set(flags, r.ordinal(), true);
		}

		if (and) {
			return DAO.queryNative(Boolean.class, "SELECT bool(role & 8) OR bool(role & ?2) = ?2 FROM account WHERE uid = ?1",
					uid, flags
			);
		} else {
			return DAO.queryNative(Boolean.class, "SELECT bool(role & (8 | ?2)) FROM account WHERE uid = ?1",
					uid, flags
			);
		}
	}

	public long getBalance() {
		return balance;
	}

	protected void setBalance(long balance) {
		this.balance = balance;
	}

	public long getUsable() {
		return getBalance() - getReserved();
	}

	public long getDebit() {
		return debit;
	}

	protected void setDebit(long debit) {
		this.debit = debit;
	}

	public long getReserved() {
		return DAO.queryNative(Long.class, "SELECT sum(buyout_price) FROM market_order WHERE kawaipon_uid = ?1", uid);
	}

	public long getTransferred() {
		return DAO.queryNative(Long.class, "SELECT transf_total(?1)", uid);
	}

	public boolean hasChanged() {
		return !DAO.queryNative(Boolean.class, """
				SELECT balance = ?2
				   AND debit = ?3
				   AND gems = ?4
				   AND (inventory @> cast(?5 AS JSONB) AND inventory <@ cast(?5 AS JSONB))
				FROM account
				WHERE uid = ?1
				""", uid, balance, debit, gems, inventory.toString()
		);
	}

	public void addCR(long value, String reason) {
		if (value <= 0) return;

		apply(a -> {
			long liquid = value;
			if (a.getDebit() > 0) {
				long deducted = Math.min(liquid, a.getDebit());
				a.setDebit(a.getDebit() - deducted);
				liquid -= deducted;
			}

			a.setBalance(a.getBalance() + liquid);
			a.addTransaction(value, true, reason, Currency.CR);
		});
	}

	public void consumeCR(long value, String reason) {
		if (value <= 0) return;

		apply(a -> {
			a.setBalance(a.getBalance() - value);
			if (a.getBalance() < 0) {
				a.setDebit(-a.getBalance() + a.getDebit());
				a.setBalance(0);
			}

			a.addTransaction(value, false, reason, Currency.CR);
		});
	}

	public void transfer(long value, String uid) {
		if (value <= 0) return;

		Account target = DAO.find(Account.class, uid);
		apply(a -> {
			a.setLastTransfer(ZonedDateTime.now(ZoneId.of("GMT-3")));
			a.setBalance(a.getBalance() - value);
			if (a.getBalance() < 0) {
				a.setDebit(-a.getBalance() + a.getDebit());
				a.setBalance(0);
			}

			target.addCR(value, "Received from " + getName());
			a.addTransaction(value, false, "Transferred to " + target.getName(), Currency.CR);
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

		apply(a -> {
			a.setGems(a.getGems() + value);
			a.addTransaction(value, true, reason, Currency.GEM);
		});
	}

	public void consumeGems(int value, String reason) {
		if (value <= 0) return;

		apply(a -> {
			a.setGems(a.getGems() - value);
			a.addTransaction(value, false, reason, Currency.GEM);
		});
	}

	public boolean hasEnough(int value, Currency currency, String item) {
		if (currency == Currency.ITEM) {
			return hasEnough(value, item);
		} else {
			return hasEnough(value, currency);
		}
	}

	public boolean hasEnough(int value, Currency currency) {
		if (value == 0) return true;
		else if (value < 0) return false;

		return switch (currency) {
			case CR -> getBalance() - getReserved() - getDebit() >= value;
			case GEM -> getGems() >= value;
			case ITEM -> false;
		};
	}

	public boolean hasEnough(int value, String item) {
		if (value == 0) return true;
		else if (value < 0) return false;

		return getItemCount(item) >= value;
	}

	public List<Profile> getProfiles() {
		return DAO.queryAll(Profile.class, "SELECT p FROM Profile p WHERE p.id.uid = ?1 ORDER BY p.xp DESC", uid);
	}

	public Profile getProfile(Member member) {
		return DAO.find(Profile.class, new ProfileId(member.getId(), member.getGuild().getId()));
	}

	public AccountSettings getSettings() {
		if (settings == null) {
			this.settings = new AccountSettings(uid);
		}

		return settings;
	}

	public Kawaipon getKawaipon() {
		return DAO.find(Kawaipon.class, uid);
	}

	public List<Deck> getDecks() {
		boolean update = false;
		int cap = 2 + getItemCount("extra_deck");

		while (decks.size() < cap) {
			decks.add(new Deck(this));
			update = true;
		}

		if (update) {
			save();
			return refresh().getDecks();
		}

		return Stream.concat(decks.stream(), Stream.of(new DailyDeck(this)))
				.sorted(Comparator.comparingInt(Deck::getId))
				.toList();
	}

	public Deck getDeck() {
		List<Deck> decks = getDecks();
		for (Deck d : decks) {
			if (d.isCurrent()) return d;
		}

		return decks.getFirst();
	}

	public void addTransaction(long value, boolean input, String reason, Currency currency) {
		DAO.applyNative(Transaction.class, """
				INSERT INTO transaction (account_uid, date, input, reason, value, currency)
				VALUES (?1, current_timestamp AT TIME ZONE 'BRT', ?2, ?3, ?4, ?5)
				""", uid, input, reason, value, currency.name());
	}

	public DynamicProperty getDynamicProperty(String id) {
		DynamicProperty prop = DAO.query(DynamicProperty.class, "SELECT dp FROM DynamicProperty dp WHERE id = ?1", new DynamicPropertyId(uid, id));
		if (prop == null) {
			return new DynamicProperty(this, id, "");
		}

		return prop;
	}

	public String getDynValue(String id) {
		return getDynValue(id, "");
	}

	public String getDynValue(String id, String defaultValue) {
		return DynamicProperty.get(uid, id, defaultValue);
	}

	public void setDynValue(String id, Object value) {
		DynamicProperty.update(uid, id, value);
	}

	public void setDynValue(String id, Function<String, Object> value) {
		DynamicProperty prop = getDynamicProperty(id);
		prop.setValue(value.apply(getDynValue(id)));
		prop.save();
	}

	public AccountTitle getTitle() {
		return titles.parallelStream()
				.filter(AccountTitle::isCurrent)
				.findAny().orElse(null);
	}

	public Set<AccountTitle> getTitles() {
		return titles;
	}

	public synchronized Title checkTitles(I18N locale) {
		List<Title> titles = DAO.queryAll(Title.class, """
				SELECT t
				FROM Title t
				LEFT JOIN AccountTitle at ON at.title.id = t.id AND at.account.uid = ?1
				WHERE at IS NULL
				AND COALESCE(t.condition, '') <> ''
				""", uid
		);

		for (Title title : titles) {
			if (title.check(locale, this)) {
				this.titles.add(new AccountTitle(this, title));
				return title;
			}
		}

		return null;
	}

	public boolean hasTitle(Title title) {
		return titles.parallelStream().anyMatch(t -> t.getTitle().equals(title));
	}

	public boolean addTitle(String title) {
		return titles.add(new AccountTitle(this, DAO.find(Title.class, title)));
	}

	public JSONObject getInventory() {
		return inventory;
	}

	public int getItemCount(String id) {
		return inventory.getInt(id.toUpperCase());
	}

	public UserItem getItem(String id) {
		if (getItemCount(id) > 0) {
			return DAO.find(UserItem.class, id.toUpperCase());
		}

		return null;
	}

	public void addItem(UserItem item, int amount) {
		addItem(item.getId(), amount);
	}

	public void addItem(String id, int amount) {
		apply(a -> a.getInventory().compute(id.toUpperCase(), (k, v) -> {
			if (v == null) return amount;

			return ((Number) v).intValue() + amount;
		}));
	}

	public boolean consumeItem(UserItem item) {
		return consumeItem(item.getId());
	}

	public boolean consumeItem(String id) {
		return consumeItem(id, 1);
	}

	public boolean consumeItem(UserItem item, int amount) {
		return consumeItem(item.getId(), amount);
	}

	public boolean consumeItem(String id, int amount) {
		return consumeItem(id, amount, false);
	}

	public boolean consumeItem(String id, int amount, boolean force) {
		if (amount <= 0) return false;

		AtomicReference<Boolean> consumed = new AtomicReference<>();
		apply(a -> {
			int rem = a.getInventory().getInt(id.toUpperCase());
			if (rem < amount && !force) return;

			if (rem - amount == 0) {
				a.getInventory().remove(id.toUpperCase());
			} else {
				a.getInventory().put(id.toUpperCase(), rem - amount);
			}

			if (consumed.get() == null) {
				consumed.set(true);
			}
		});

		if (consumed.get() == null) {
			consumed.set(false);
		}

		return consumed.get();
	}

	public Map<UserItem, Integer> getItems() {
		return inventory.entrySet().parallelStream()
				.filter(e -> ((Number) e.getValue()).intValue() > 0)
				.map(e -> new Pair<>(DAO.find(UserItem.class, e.getKey()), ((Number) e.getValue()).intValue()))
				.filter(p -> p.getFirst() != null)
				.collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
	}

	public double getWinrate() {
		return DAO.queryNative(Double.class, "SELECT user_winrate(?1)", uid);
	}

	public int getShoukanRanking() {
		return DAO.queryNative(Integer.class, "SELECT user_shoukan_ranking(?1)", uid);
	}

	public List<MatchHistory> getMatches() {
		return DAO.queryAll(MatchHistory.class, """
				SELECT mh
				FROM MatchHistory mh
				WHERE ?1 IN (mh.info.top.id.uid, mh.info.bottom.id.uid)
				ORDER BY mh.id DESC
				""", uid);
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

	public void addVote(boolean weekend) {
		Account acc = refresh();
		if (acc.isVoteAwarded()) return;

		int streak = acc.getStreak() + 1;
		acc.setStreak(streak);
		acc.setVoteAwarded(true);
		acc.save();

		int cr = (int) (((weekend ? 1500 : 1000) - Math.min((balance + getTransferred()) / 2000, 800)) * streak);
		acc.addCR(cr, "Daily");

		int gems = 0;
		if (streak > 0 && streak % 7 == 0) {
			gems = Math.min((int) Calc.getFibonacci(streak / 7), 3);
			acc.addGems(gems, "Vote streak " + acc.getStreak());
		}

		EmbedBuilder eb = new EmbedBuilder()
				.setColor(gems > 0 ? Color.red : Color.orange)
				.setThumbnail(Constants.ORIGIN_RESOURCES + "assets/" + (gems > 0 ? "gem_icon.png" : "cr_icon.png"))
				.setDescription(getEstimateLocale().get("str/daily_message", cr, gems, streak))
				.setTimestamp(acc.getLastVote());

		User user = getUser();
		if (user == null) return;

		user.openPrivateChannel()
				.flatMap(c -> c.sendMessageEmbeds(eb.build()))
				.queue(null, Utils::doNothing);

		if (getSettings().isRemindVote()) {
			new Reminder(this, null, getEstimateLocale().get("str/vote_reminder"), 12 * Constants.MILLIS_IN_HOUR).save();
		}
	}

	public boolean hasVoted() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-3"));
		boolean voted = false;
		if (lastVote != null) {
			voted = now.isBefore(lastVote.plusHours(12));
		}

		if (voted && !voteAwarded) {
			addVote(now.get(ChronoField.DAY_OF_WEEK) >= DayOfWeek.SATURDAY.getValue());
		}

		return voted;
	}

	protected void setLastVote(ZonedDateTime lastVote) {
		this.lastVote = lastVote;
	}

	public boolean isVoteAwarded() {
		return voteAwarded;
	}

	public void setVoteAwarded(boolean voteAwarded) {
		this.voteAwarded = voteAwarded;
	}

	protected void setStreak(int voteStreak) {
		this.voteStreak = voteStreak;
	}

	public int getStreak() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-3"));
		if (lastVote != null && now.isAfter(lastVote.plusHours(24))) {
			voteStreak = 0;
		}

		return voteStreak;
	}

	public ZonedDateTime getLastTransfer() {
		return lastTransfer;
	}

	public void setLastTransfer(ZonedDateTime lastTransfer) {
		this.lastTransfer = lastTransfer;
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
		return DAO.queryNative(Integer.class, "SELECT cast(sqrt(max(xp) / 100) AS INT) FROM profile WHERE uid = ?1", uid);
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
								ORDER BY count(gc.locale) DESC
								""", uid
				), "PT"
		));
	}

	public String getBalanceFooter(I18N locale) {
		return locale.get("currency/cr", balance) + " | " + locale.get("currency/gem", gems);
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
		return Objects.hashCode(uid);
	}
}
