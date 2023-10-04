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

import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Blacklistable;
import com.kuuhaku.interfaces.annotations.WhenNull;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Role;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.persistent.converter.RoleFlagConverter;
import com.kuuhaku.model.persistent.id.ProfileId;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.shoukan.history.Match;
import com.kuuhaku.util.Bit;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import com.ygimenez.json.JSONUtils;
import jakarta.persistence.*;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static jakarta.persistence.CascadeType.ALL;

@Entity
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

	@OneToOne(fetch = FetchType.LAZY, cascade = ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "uid")
	@Fetch(FetchMode.JOIN)
	private AccountSettings settings;

	@OneToOne(fetch = FetchType.LAZY, cascade = ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "uid")
	@Fetch(FetchMode.JOIN)
	private Kawaipon kawaipon;

	@OneToMany(mappedBy = "account", cascade = ALL, orphanRemoval = true)
	@OrderColumn(name = "index")
	@Fetch(FetchMode.SUBSELECT)
	private List<Deck> decks = new ArrayList<>();

	@OneToMany(mappedBy = "account", cascade = ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private List<Transaction> transactions = new ArrayList<>();

	@OneToMany(mappedBy = "account", cascade = ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private Set<DynamicProperty> dynamicProperties = new LinkedHashSet<>();

	@OneToMany(mappedBy = "account", cascade = ALL, orphanRemoval = true)
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

	@Column(name = "last_transfer")
	private ZonedDateTime lastTransfer;

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

	public boolean hasRole(Role... roles) {
		return Utils.containsAny(this.roles, roles);
	}

	public static boolean hasRole(String uid, boolean and, Role... roles) {
		int flags = 0;
		for (Role r : roles) {
			flags = Bit.set(flags, r.ordinal(), true);
		}

		if (and) {
			return DAO.queryNative(Boolean.class, "SELECT bool(role & 8) OR (role & ?2) = ?2 FROM account WHERE uid = ?1",
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

	public long getDebit() {
		return debit;
	}

	protected void setDebit(long debit) {
		this.debit = debit;
	}

	public void addCR(long value, String reason) {
		if (value <= 0) return;

		apply(getClass(), uid, a -> {
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

		apply(getClass(), uid, a -> {
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
		apply(getClass(), this.uid, a -> {
			a.setLastTransfer(ZonedDateTime.now(ZoneId.of("GMT-3")));
			a.setBalance(a.getBalance() - value);
			if (a.getBalance() < 0) {
				a.setDebit(-a.getBalance() + a.getDebit());
				a.setBalance(0);
			}

			target.addCR(value, "Received from " + target.getName());
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

		apply(getClass(), uid, a -> {
			a.setGems(a.getGems() + value);
			a.addTransaction(value, true, reason, Currency.GEM);
		});
	}

	public void consumeGems(int value, String reason) {
		if (value <= 0) return;

		apply(getClass(), uid, a -> {
			a.setGems(a.getGems() - value);
			a.addTransaction(value, false, reason, Currency.GEM);
		});
	}

	public boolean hasEnough(int value, Currency currency) {
		if (value == 0) return true;
		else if (value < 0) return false;

		return switch (currency) {
			case CR -> getBalance() - getDebit() >= value;
			case GEM -> getGems() >= value;
		};
	}

	public List<Profile> getProfiles() {
		return DAO.queryAll(Profile.class, "SELECT p FROM Profile p WHERE p.id.uid = ?1 ORDER BY p.xp DESC", uid);
	}

	public Profile getProfile(Member member) {
		return DAO.find(Profile.class, new ProfileId(uid, member.getGuild().getId()));
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
				.findAny().orElse(null);
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
				.findAny().orElse(new DynamicProperty(this, id, ""));
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

	public AccountTitle getTitle() {
		return titles.parallelStream()
				.filter(AccountTitle::isCurrent)
				.findAny().orElse(null);
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
			if (title.check(this, locale)) {
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
		apply(getClass(), uid, a ->
				a.getInventory().compute(id, (k, v) -> {
					if (v == null) return amount;

					return ((Number) v).intValue() + amount;
				})
		);
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

		AtomicBoolean consumed = new AtomicBoolean();
		apply(getClass(), uid, a -> {
			int rem = a.getInventory().getInt(id.toUpperCase());
			if (rem < amount && !force) return;

			if (rem - amount == 0) {
				a.getInventory().remove(id.toUpperCase());
			} else {
				a.getInventory().put(id.toUpperCase(), rem - amount);
			}

			consumed.set(true);
		});

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

	public double getShoukanRanking() {
		return DAO.queryNative(Integer.class, "SELECT user_shoukan_ranking(?1)", uid);
	}

	public List<Match> getMatches() {
		return DAO.queryAllUnmapped("""
						SELECT jsonb_build_object('info', info, 'turns', turns)
						FROM v_matches
						WHERE has(players, ?1)
						""", uid
				).stream()
				.peek(o -> System.out.println(o[0]))
				.map(o -> JSONUtils.fromJSON(String.valueOf(o[0]), Match.class))
				.toList();
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
		voteStreak++;
		apply(getClass(), uid, a -> {
			a.setStreak(a.getStreak() + 1);
			a.setLastVote(ZonedDateTime.now(ZoneId.of("GMT-3")));
		});
	}

	public boolean voted() {
		if (lastVote == null) return false;
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-3"));

		return now.isBefore(lastVote.plusHours(12));
	}

	protected void setLastVote(ZonedDateTime lastVote) {
		this.lastVote = lastVote;
	}

	protected void setStreak(int voteStreak) {
		this.voteStreak = voteStreak;
	}

	public int getStreak() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-3"));
		if (lastVote != null && now.isAfter(lastVote.plusHours(25))) { // TODO Return to 24
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
