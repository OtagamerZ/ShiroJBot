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
import com.kuuhaku.model.enums.Role;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.utils.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Entity
@DynamicUpdate
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "account", indexes = @Index(columnList = "debit, balance DESC"))
public class Account extends DAO implements Blacklistable {
	@Id
	@Column(name = "uid", nullable = false)
	private String uid;

	@Column(name = "name")
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false)
	private Role role = Role.USER;

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
	@Fetch(FetchMode.SUBSELECT)
	private List<Deck> decks = new ArrayList<>();

	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private List<Transaction> transactions = new ArrayList<>();

	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private Set<DynamicProperty> dynamicProperties = new LinkedHashSet<>();

	@Column(name = "blacklisted", nullable = false)
	private boolean blacklisted;

	@Column(name = "created_at", nullable = false)
	private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("GMT-3"));

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
		return Main.getApp().getMainShard().getUserById(uid);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Role getRole() {
		return role;
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

			a.addTransaction(value, true, reason);
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

			a.addTransaction(value, false, reason);
		});
	}

	public int getGems() {
		return gems;
	}

	public void addGem() {
		gems++;
	}

	public void addGems(int value) {
		gems += value;
	}

	public List<Profile> getProfiles() {
		return profiles;
	}

	public Profile getProfile(Member member) {
		return profiles.stream()
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
		while (decks.size() < settings.getDeckCapacity()) {
			Deck d = new Deck(this);
			decks.add(d);
			d.save();
		}

		return decks;
	}

	public Deck getCurrentDeck() {
		return getDecks().stream()
				.filter(Deck::isCurrent)
				.findFirst().orElse(null);
	}

	public List<Transaction> getTransactions() {
		return transactions;
	}

	public void addTransaction(long value, boolean input, String reason) {
		transactions.add(new Transaction(this, value, input, reason));
	}

	public Set<DynamicProperty> getDynamicProperties() {
		return dynamicProperties;
	}

	public DynamicProperty getDynamicProperty(String id) {
		return dynamicProperties.stream()
				.filter(dp -> dp.getId().getId().equals(id))
				.findFirst().orElse(new DynamicProperty(this, id, ""));
	}

	public void addDynamicProperties(String id, Object value) {
		dynamicProperties.add(new DynamicProperty(this, id, String.valueOf(value)));
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
