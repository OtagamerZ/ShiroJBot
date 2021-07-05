/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent;

import com.kuuhaku.controller.postgresql.LogDAO;
import com.kuuhaku.model.enums.ClanHierarchy;
import com.kuuhaku.model.enums.ClanPermission;
import com.kuuhaku.model.enums.ClanTier;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "clan")
public class Clan {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(20) NOT NULL")
	private String name;

	@Column(columnDefinition = "VARCHAR(256)")
	private String motd;

	@Column(columnDefinition = "TEXT")
	private String icon = null;

	@Column(columnDefinition = "TEXT")
	private String banner = null;

	@Enumerated(value = EnumType.STRING)
	private ClanTier tier = ClanTier.PARTY;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long vault = 0;

	@Temporal(TemporalType.DATE)
	private Calendar paidRent = Calendar.getInstance();

	@ElementCollection(fetch = FetchType.LAZY)
	@JoinColumn(name = "clan_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<String> transactions = new ArrayList<>();

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "clan_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<ClanMember> members = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@JoinColumn(name = "clan_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Map<ClanHierarchy, Integer> permissions = new HashMap<>() {{
		put(ClanHierarchy.LEADER, 0xf);
		put(ClanHierarchy.SUBLEADER, 0xf);
		put(ClanHierarchy.CAPTAIN, 0x7);
		put(ClanHierarchy.MEMBER, 0x1);
	}};

	public Clan(String name, String leader) {
		this.name = name;
		members.add(new ClanMember(leader, ClanHierarchy.LEADER));
	}

	public Clan() {
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getMotd() {
		return motd;
	}

	public void setMotd(String motd) {
		this.motd = motd;
	}

	public BufferedImage getIcon() {
		return Helper.btoa(icon);
	}

	public void setIcon(BufferedImage icon) {
		this.icon = Helper.atob(Helper.scaleAndCenterImage(icon, 256, 256), "png");
	}

	public BufferedImage getBanner() {
		return Helper.btoa(banner);
	}

	public void setBanner(BufferedImage banner) {
		this.banner = Helper.atob(Helper.scaleAndCenterImage(banner, 512, 256), "png");
	}

	public ClanMember getLeader() {
		return members.stream()
				.filter(ClanMember::isLeader)
				.findFirst()
				.orElse(null);
	}

	public ClanMember getSubLeader() {
		return members.stream()
				.filter(ClanMember::isSubLeader)
				.findFirst()
				.orElse(null);
	}

	public ClanMember getNextInHierarchy() {
		return members.stream()
				.filter(cm -> !cm.isLeader())
				.min(Comparator
						.<ClanMember>comparingInt(cm -> cm.getRole().ordinal())
						.thenComparing(ClanMember::getJoinedAt))
				.orElse(null);
	}

	public ClanMember getMember(String id) {
		return members.stream()
				.filter(cm -> cm.getUid().equals(id))
				.findFirst()
				.orElse(null);
	}

	public List<ClanMember> getFromHierarchy(ClanHierarchy ch) {
		return members.stream()
				.filter(cm -> cm.getRole() == ch)
				.collect(Collectors.toList());
	}

	public void transfer() {
		ClanMember leader = getLeader();
		ClanMember sub = getNextInHierarchy();

		leader.setRole(ClanHierarchy.SUBLEADER);
		sub.setRole(ClanHierarchy.LEADER);
		transactions.add(LogDAO.getUsername(leader.getUid()) + " transferiu a posse do clã para " + LogDAO.getUsername(sub.getUid()) + ".");
	}

	public void promote(String id, User u) {
		ClanMember cm = getMember(id);
		cm.promote();
		transactions.add(u.getAsTag() + " promoveu " + LogDAO.getUsername(id) + ".");
	}

	public void promote(User tgt, User u) {
		ClanMember cm = getMember(tgt.getId());
		cm.promote();
		transactions.add(u.getAsTag() + " promoveu " + tgt.getAsTag() + ".");
	}

	public void demote(String id, User u) {
		ClanMember cm = getMember(id);
		cm.demote();
		transactions.add(u.getAsTag() + " rebaixou " + LogDAO.getUsername(id) + ".");
	}

	public void demote(User tgt, User u) {
		ClanMember cm = getMember(tgt.getId());
		cm.demote();
		transactions.add(u.getAsTag() + " rebaixou " + tgt.getAsTag() + ".");
	}

	public void kick(String id, User u) {
		members.removeIf(cm -> cm.getUid().equals(id));
		transactions.add(u.getAsTag() + " expulsou " + LogDAO.getUsername(id) + ".");
	}

	public void kick(User tgt, User u) {
		members.removeIf(cm -> cm.getUid().equals(tgt.getId()));
		transactions.add(u.getAsTag() + " expulsou " + tgt.getAsTag() + ".");
	}

	public void invite(String id, User u) {
		members.add(new ClanMember(id, ClanHierarchy.MEMBER));
		transactions.add(u.getAsTag() + " convidou " + LogDAO.getUsername(id) + " para o clã.");
	}

	public void invite(User tgt, User u) {
		members.add(new ClanMember(tgt.getId(), ClanHierarchy.MEMBER));
		transactions.add(u.getAsTag() + " convidou " + tgt.getAsTag() + " para o clã.");
	}

	public void leave(String id) {
		ClanMember cm = getMember(id);
		if (cm.isLeader()) {
			transfer();
		}

		members.remove(cm);
		transactions.add(LogDAO.getUsername(id) + " saiu do clã.");
	}

	public ClanHierarchy getHierarchy(String id) {
		return getMember(id).getRole();
	}

	public ClanTier getTier() {
		return tier;
	}

	public void setTier(ClanTier tier) {
		this.tier = tier;
	}

	public long getVault() {
		return vault;
	}

	public void deposit(long amount, User u) {
		this.vault += amount;
		transactions.add(u.getAsTag() + " depositou " + Helper.separate(amount) + " créditos.");
	}

	public void withdraw(long amount, User u) {
		this.vault -= amount;
		transactions.add(u.getAsTag() + " sacou " + Helper.separate(amount) + " créditos.");
	}

	public void upgrade(User u) {
		this.vault -= tier.getNext().getCost();
		this.tier = this.tier.getNext();
		transactions.add(u.getAsTag() + " evoluiu o tier do clã por " + Helper.separate(tier.getCost()) + " créditos.");
	}

	public boolean hasPaidRent() {
		Calendar c = Calendar.getInstance();
		return paidRent.get(Calendar.MONTH) == c.get(Calendar.MONTH);
	}

	public void payRent(User u) {
		this.vault -= tier.getRent();
		this.paidRent = Calendar.getInstance();
		transactions.add(u.getAsTag() + " pagou o aluguel de " + Helper.separate(tier.getRent()) + " créditos.");
	}

	public void payRent() {
		this.vault -= tier.getRent();
		this.paidRent = Calendar.getInstance();
		transactions.add("Aluguel pago de " + Helper.separate(tier.getRent()) + " créditos pago automaticamente.");
	}

	public void changeName(User u, String name) {
		this.vault -= 100000;
		this.name = name;
		transactions.add(u.getAsTag() + " trocou o nome do clã por 100.000 créditos.");
	}

	public List<String> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<String> transactions) {
		this.transactions = transactions;
	}

	public List<ClanMember> getMembers() {
		members.sort(
				Comparator.<ClanMember>comparingInt(cm -> cm.getRole().ordinal())
						.thenComparing(ClanMember::getJoinedAt)
		);
		return members;
	}

	public Map<ClanHierarchy, Integer> getPermissions() {
		return permissions;
	}

	public EnumSet<ClanPermission> getPermissions(ClanHierarchy ch) {
		return ClanPermission.getPermissions(permissions.get(ch));
	}

	public void setPermissions(ClanHierarchy ch, EnumSet<ClanPermission> cp) {
		permissions.put(ch, ClanPermission.getFlags(cp));
	}

	public void setPermissions(Map<ClanHierarchy, Integer> permissions) {
		this.permissions = permissions;
	}

	public boolean isLocked(String id, ClanPermission ch) {
		return !ClanPermission.getPermissions(permissions.get(getMember(id).getRole())).contains(ch);
	}
}
