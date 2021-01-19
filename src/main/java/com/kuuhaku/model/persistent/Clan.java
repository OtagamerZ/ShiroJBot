/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.model.enums.ClanHierarchy;
import com.kuuhaku.model.enums.ClanPermission;
import com.kuuhaku.model.enums.ClanTier;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "clan")
public class Clan {
	@Id
	@Column(columnDefinition = "CHAR(64) NOT NULL")
	private String hash;

	@Column(columnDefinition = "VARCHAR(20) NOT NULL")
	private String name;

	@Column(columnDefinition = "VARCHAR(256)")
	private String motd;

	@Lob
	private byte[] icon = null;

	@Lob
	private byte[] banner = null;

	@Enumerated(value = EnumType.STRING)
	private ClanTier tier = ClanTier.PARTY;

	@Column(columnDefinition = "LONG NOT NULL DEFAULT 0")
	private long vault = 0;

	@OneToOne(fetch = FetchType.EAGER)
	private DeckStash deck = new DeckStash();

	@ElementCollection(fetch = FetchType.EAGER)
	private List<String> transactions = new ArrayList<>();

	@Enumerated(value = EnumType.STRING)
	@ElementCollection(fetch = FetchType.EAGER)
	private Map<String, ClanHierarchy> members = new HashMap<>();

	@Enumerated(value = EnumType.STRING)
	@ElementCollection(fetch = FetchType.EAGER)
	private Map<ClanHierarchy, Integer> permissions = new HashMap<>() {{
		put(ClanHierarchy.LEADER, 0xf);
		put(ClanHierarchy.SUBLEADER, 0xf);
		put(ClanHierarchy.CAPTAIN, 0x7);
		put(ClanHierarchy.MEMBER, 0x1);
	}};

	public Clan(String name, String leader) {
		this.hash = Helper.hash(name.toLowerCase().getBytes(StandardCharsets.UTF_8), "SHA-256");
		this.name = name;
		this.members.put(leader, ClanHierarchy.LEADER);
	}

	public Clan() {
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
		try (ByteArrayInputStream bais = new ByteArrayInputStream(icon)) {
			return ImageIO.read(bais);
		} catch (IOException e) {
			return null;
		}
	}

	public void setIcon(BufferedImage icon) {
		this.icon = Helper.getBytes(icon);
	}

	public BufferedImage getBanner() {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(banner)) {
			return ImageIO.read(bais);
		} catch (IOException e) {
			return null;
		}
	}

	public void setBanner(BufferedImage banner) {
		this.banner = Helper.getBytes(banner);
	}

	public String getLeader() {
		return members.entrySet().stream()
				.filter(e -> e.getValue() == ClanHierarchy.LEADER)
				.findFirst()
				.orElse(Pair.of(null, null))
				.getKey();
	}

	public String getSubLeader() {
		return members.entrySet().stream()
				.filter(e -> e.getValue() == ClanHierarchy.SUBLEADER)
				.findFirst()
				.orElse(Pair.of(null, null))
				.getKey();
	}

	public void setLeader(String leader) {
		members.put(leader, ClanHierarchy.LEADER);
	}

	public List<String> getFromHierarchy(ClanHierarchy ch) {
		return members.entrySet().stream()
				.filter(e -> e.getValue() == ch)
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}

	public void transfer() {
		String leader = getLeader();
		String sub = getSubLeader();
		members.put(leader, ClanHierarchy.SUBLEADER);
		members.put(sub, ClanHierarchy.LEADER);
	}

	public void promote(String id) {
		ClanHierarchy ch = members.get(id);
		ClanHierarchy next = Helper.getNext(ch, ClanHierarchy.MEMBER, ClanHierarchy.CAPTAIN, ClanHierarchy.SUBLEADER);
		members.put(id, Helper.getOr(next, ch));
	}

	public void demote(String id) {
		ClanHierarchy ch = members.get(id);
		ClanHierarchy previous = Helper.getPrevious(ch, ClanHierarchy.MEMBER, ClanHierarchy.CAPTAIN, ClanHierarchy.SUBLEADER);
		members.put(id, Helper.getOr(previous, ch));
	}

	public void kick(String id, User u) {
		members.remove(id);
		transactions.add(u.getAsTag() + " expulsou o membro com ID " + id);
	}

	public void invite(String id, User u) {
		members.put(id, ClanHierarchy.MEMBER);
		transactions.add(u.getAsTag() + " adicionou o membro com ID " + id);
	}

	public void leave(String id) {
		if (members.remove(id) == ClanHierarchy.LEADER) {
			String next = members.entrySet().stream().min(Comparator.comparingInt(e -> e.getValue().ordinal()))
					.orElse(Pair.of(null, null))
					.getKey();

			members.put(next, ClanHierarchy.LEADER);
		}
	}

	public ClanHierarchy getHierarchy(String id) {
		return members.get(id);
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
		transactions.add(u.getAsTag() + " depositou " + amount + " créditos");
	}

	public void withdraw(long amount, User u) {
		this.vault -= amount;
		transactions.add(u.getAsTag() + " sacou " + amount + " créditos");
	}

	public DeckStash getDeck() {
		return deck;
	}

	public List<String> getTransactions() {
		return transactions;
	}

	public Map<String, ClanHierarchy> getMembers() {
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

	public boolean hasPermission(String id, ClanPermission ch) {
		return ClanPermission.getPermissions(permissions.get(members.get(id))).contains(ch);
	}
}
