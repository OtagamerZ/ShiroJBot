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

import com.kuuhaku.model.enums.ClanTier;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clan")
public class Clan {
	@Id
	private String name;

	@Lob
	private byte[] icon = null;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String leader = "";

	@Enumerated(value = EnumType.STRING)
	private ClanTier tier = ClanTier.PARTY;

	@Column(columnDefinition = "LONG NOT NULL DEFAULT 0")
	private long vault = 0;

	@OneToOne(fetch = FetchType.EAGER)
	private DeckStash deck = new DeckStash();

	@ElementCollection(fetch = FetchType.EAGER)
	private List<String> transactions = new ArrayList<>();

	public String getName() {
		return name;
	}

	public BufferedImage getIcon() {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(icon)) {
			return ImageIO.read(bais);
		} catch (IOException e) {
			return null;
		}
	}

	public void setIcon(BufferedImage icon, User u) {
		this.icon = Helper.getBytes(icon);
	}

	public String getLeader() {
		return leader;
	}

	public void setLeader(String leader) {
		this.leader = leader;
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
}
