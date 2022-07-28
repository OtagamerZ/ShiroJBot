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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Blacklistable;
import com.kuuhaku.interfaces.annotations.WhenNull;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.id.ProfileId;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import net.dv8tion.jda.api.entities.Member;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;
import org.jdesktop.swingx.graphics.BlendComposite;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@DynamicUpdate
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "profile", indexes = @Index(columnList = "xp DESC"))
public class Profile extends DAO<Profile> implements Blacklistable {
	private static final Dimension SIZE = new Dimension(950, 600);

	@EmbeddedId
	private ProfileId id;

	@Column(name = "xp", nullable = false)
	private long xp;

	@OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private List<VoiceData> voiceData = new ArrayList<>();

	@ElementCollection
	@Column(name = "warn")
	@CollectionTable(name = "profile_warns")
	private List<String> warns = new ArrayList<>();

	@ManyToOne(optional = false)
	@JoinColumn(name = "uid", nullable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("uid")
	private Account account;

	@ManyToOne(optional = false)
	@JoinColumn(name = "gid", nullable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("gid")
	private GuildConfig guild;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumns({
			@PrimaryKeyJoinColumn(name = "uid"),
			@PrimaryKeyJoinColumn(name = "gid")
	})
	@Fetch(FetchMode.JOIN)
	private ProfileSettings settings;

	public Profile() {
	}

	public Profile(Member member) {
		this(new ProfileId(member.getId(), member.getGuild().getId()));
	}

	@WhenNull
	public Profile(ProfileId id) {
		this.id = id;
		this.account = DAO.find(Account.class, id.getUid());
		this.guild = DAO.find(GuildConfig.class, id.getGid());
		this.settings = new ProfileSettings(id);
	}

	public ProfileId getId() {
		return id;
	}

	public long getXp() {
		return xp;
	}

	public void addXp(long value) {
		xp += value;
	}

	public int getLevel() {
		return (int) Math.ceil(Math.sqrt(xp / 100d));
	}

	public List<VoiceData> getVoiceData() {
		return voiceData;
	}

	public List<String> getWarns() {
		return warns;
	}

	public Account getAccount() {
		return account;
	}

	public GuildConfig getGuild() {
		return guild;
	}

	public ProfileSettings getSettings() {
		return Utils.getOr(settings, DAO.find(ProfileSettings.class, id));
	}

	@Override
	public boolean isBlacklisted() {
		return account.isBlacklisted();
	}

	public BufferedImage render(I18N locale) {
		BufferedImage mask = IO.getResourceAsImage("assets/profile_mask.webp");
		BufferedImage overlay = Graph.toColorSpace(IO.getResourceAsImage("assets/profile_overlay.webp"), BufferedImage.TYPE_INT_ARGB);;
		BufferedImage hex = IO.getResourceAsImage("assets/hex_grid.webp");

		AccountSettings settings = account.getSettings();
		BufferedImage bg = IO.getImage(settings.getBackground());
		if (bg == null) {
			settings.setBackground(null);
			bg = IO.getImage(settings.getBackground());
		}
		bg = Graph.toColorSpace(bg, BufferedImage.TYPE_INT_ARGB);

		BufferedImage bi = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		Graph.applyMask(bg, mask, 0);
		g2d.drawImage(bg, 0, 0, null);

		Graphics2D og2d = overlay.createGraphics();
		if (settings.getColor().equals(Color.BLACK)) {
			og2d.setColor(Graph.rotate(Graph.getColor(bg), 180));
		} else {
			og2d.setColor(settings.getColor());
		}
		og2d.setComposite(BlendComposite.Multiply);
		og2d.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
		og2d.dispose();

		Polygon inner = Graph.makePoly(
				6, 75,
				475, 75,
				525, 31,
				897, 31,
				944, 78,
				944, 547,
				897, 594,
				53, 594,
				6, 547
		);

		Graph.applyMask(hex, mask, 0, true);
		g2d.drawImage(hex, 0, 0, null);

		Graph.applyTransformed(g2d, g1 -> {
			Color bgCol = new Color((200 << 24) | (settings.getColor().getRGB() & 0x00FFFFFF), true);

			g1.setClip(inner);
			g1.setColor(bgCol);

			RoundRectangle2D wids = new RoundRectangle2D.Double(-14, 110, 200, 50, 20, 20);

			g1.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 15));
			for (Object o : settings.getWidgets()) {
				String s = String.valueOf(o);
				Rectangle2D bounds = Graph.getStringBounds(g1, s);
				int y = (int) wids.getY();

				g1.setColor(bgCol);
				wids.setFrame(wids.getX(),  y, 43 + bounds.getWidth(), bounds.getHeight() * 3);
				Graph.drawOutlined(g1, wids, 1, Color.BLACK);
				wids.setFrame(wids.getX(),  y + wids.getHeight() + 10, 0, 0);

				g1.setColor(Color.WHITE);
				Graph.drawOutlinedString(g1, s, 15, (int) (y + bounds.getHeight() * 2), 3, Color.BLACK);
			}

			g1.setColor(bgCol);
			Shape desc = new RoundRectangle2D.Double(
					SIZE.width - SIZE.width / 2d - 40, SIZE.height - SIZE.height / 3d - 20,
					SIZE.width / 2d, SIZE.height / 3d,
					20, 20
			);
			Graph.drawOutlined(g1, desc, 1, Color.BLACK);
		});

		Graph.applyMask(overlay, mask, 1);
		g2d.drawImage(overlay, 0, 0, null);

		g2d.dispose();

		return bi;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Profile profile = (Profile) o;
		return Objects.equals(id, profile.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
