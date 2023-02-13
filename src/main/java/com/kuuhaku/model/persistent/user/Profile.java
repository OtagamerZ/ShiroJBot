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
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Blacklistable;
import com.kuuhaku.interfaces.annotations.WhenNull;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.id.ProfileId;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;
import org.jdesktop.swingx.graphics.BlendComposite;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

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
		return (int) Math.max(1, Math.floor(Math.sqrt(xp / 100d)) + 1);
	}

	public long getXpToLevel(int level) {
		return (int) (Math.pow(level - 1, 2) * 100);
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

	public int getRanking() {
		return DAO.queryNative(Integer.class, """
				SELECT x.rank
				FROM (
				     SELECT p.uid
				          , rank() OVER (ORDER BY p.xp DESC)
				     FROM profile p
				     WHERE p.gid = ?2
				     ) x
				WHERE x.uid = ?1
				""", id.getUid(), id.getGid());
	}

	public Emote getLevelEmote() {
		return Main.getApp().getShiro()
				.getEmotesByName("lvl_" + (getLevel() - getLevel() % 5), false)
				.stream()
				.findFirst()
				.orElseThrow();
	}

	public BufferedImage render(I18N locale) {
		BufferedImage mask = IO.getResourceAsImage("assets/masks/profile_mask.png");
		BufferedImage overlay = IO.getResourceAsImage("assets/profile_overlay.png");

		AccountSettings settings = account.getSettings();
		BufferedImage bg = IO.getImage(settings.getBackground());
		if (bg == null) {
			settings.setBackground(null);
			bg = IO.getImage(settings.getBackground());
		}
		bg = Graph.scaleAndCenterImage(Graph.toColorSpace(bg, BufferedImage.TYPE_INT_ARGB), SIZE.width, SIZE.height);

		BufferedImage bi = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		BufferedImage avatar = null;
		Guild g = Main.getApp().getShiro().getGuildById(id.getGid());
		if (g != null) {
			Member m = g.getMemberById(id.getUid());
			if (m != null) {
				avatar = IO.getImage(m.getEffectiveAvatarUrl());
			}
		}

		g2d.drawImage(Utils.getOr(avatar, IO.getImage(account.getUser().getEffectiveAvatarUrl())), -23, 56, 150, 150, null);

		Graph.applyMask(bg, mask, 0);
		g2d.drawImage(bg, 0, 0, null);

		Color color;
		if (settings.getColor().equals(Color.BLACK)) {
			color = Graph.rotate(Graph.getColor(bg), 180);
		} else {
			color = settings.getColor();
		}

		Graphics2D og2d = overlay.createGraphics();
		og2d.setComposite(BlendComposite.Multiply);
		og2d.setColor(color);
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

		BufferedImage effect = settings.getEffect().getImage();
		Graph.applyMask(effect, mask, 0, true);
		g2d.drawImage(effect, 0, 0, null);

		Map<String, Object> replaces = new HashMap<>();
		replaces.put("waifu", Utils.getOr(() -> account.getCouple().getOther(id.getUid()).getName(), locale.get("str/none")));
		replaces.put("guild", getGuild().getName());
		replaces.put("g_rank", Utils.separate(account.getRanking()));
		replaces.put("l_rank", Utils.separate(getRanking()));
		replaces.put("xp", Utils.shorten(xp));
		replaces.put("level", getLevel());

		for (AccountTitle title : account.getTitles()) {
			Title t = title.getTitle();

			replaces.put(
					t.getId().toLowerCase(),
					"#%06X,%s".formatted(
							t.getRarity().getColor(false).getRGB() & 0xFFFFFF,
							t.getInfo(locale).getName().replace(" ", "_")
					)
			);
		}

		Graph.applyTransformed(g2d, g1 -> {
			Color bgCol = new Color((200 << 24) | (color.getRGB() & 0xFFFFFF), true);

			g1.setClip(inner);
			g1.setColor(bgCol);

			RoundRectangle2D wids = new RoundRectangle2D.Double(-14, 210, 200, 50, 20, 20);

			int em = g1.getFontMetrics().getHeight();
			g1.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 20));
			for (Object o : settings.getWidgets()) {
				String s = Utils.replaceTags(String.valueOf(o), '%', replaces);
				Rectangle bounds;
				if (s.startsWith("#")) {
					bounds = Graph.getStringBounds(g1, s.substring(s.indexOf(",") + 1).replace("_", " ")).getBounds();
				} else {
					bounds = Graph.getStringBounds(g1, s).getBounds();
				}
				int y = (int) wids.getY();

				g1.setColor(bgCol);
				wids.setFrame(wids.getX(), y, 43 + bounds.getWidth(), em * 2);
				Graph.drawOutlined(g1, wids, 1, Color.BLACK);
				wids.setFrame(wids.getX(), y + wids.getHeight() + 10, 0, 0);

				g1.setColor(Color.WHITE);
				Graph.drawMultilineString(g1, s, 15, (int) (y + em * 1.5), SIZE.width, (str, px, py) -> {
					if (str.startsWith("#")) {
						String[] frags = str.split(",");

						g1.setColor(Color.decode(frags[0]));
						Graph.drawOutlinedString(g1, frags[1].replace("_", " "), px, py, 3, Color.BLACK);
					} else {
						g1.setColor(Color.WHITE);
						Graph.drawOutlinedString(g1, str, px, py, 3, Color.BLACK);
					}
				});
			}

			String bio = Utils.replaceTags(settings.getBio(), '%', replaces);
			if (!bio.isBlank()) {
				int x = (int) (SIZE.width - SIZE.width / 2d - 40);
				int lines = Graph.getLineCount(g1, bio, (int) (SIZE.width / 2d - 20));
				int h = em * 2 * lines - 3 * lines;
				int w = (int) (SIZE.width / 2d);

				Graph.applyTransformed(g1, x, SIZE.height - h - 20, g2 -> {
					g2.setColor(bgCol);
					Shape desc = new RoundRectangle2D.Double(0, 0, w, h, 20, 20);
					Graph.drawOutlined(g2, desc, 1, Color.BLACK);

					g2.setColor(Color.WHITE);
					Graph.drawMultilineString(g2, bio, 10, (int) (em * 1.5), w - 20, 3, (str, px, py) -> {
						if (str.startsWith("#")) {
							String[] frags = str.split(",");

							g2.setColor(Color.decode(frags[0]));
							Graph.drawOutlinedString(g2, frags[1].replace("_", " "), px, py, 3, Color.BLACK);
						} else {
							g2.setColor(Color.WHITE);
							Graph.drawOutlinedString(g2, str, px, py, 3, Color.BLACK);
						}
					});
				});
			}
		});

		Graph.applyMask(overlay, mask, 1);
		g2d.drawImage(overlay, 0, 0, null);

		BufferedImage emote = IO.getImage(getLevelEmote().getImageUrl());
		g2d.drawImage(emote, 6, -3, 81, 81, null);

		g2d.setColor(Color.GRAY);
		Graph.drawOutlined(g2d, new Rectangle(88, 59, 384, 10), 2, Color.BLACK);

		int lvl = getLevel();
		long lvlXp = getXpToLevel(lvl);
		long toNext = getXpToLevel(lvl + 1);
		int pad = 4;
		double prcnt = Math.max(0, Calc.prcnt(xp - lvlXp, toNext - lvlXp));
		int[] colors = {0x5b2d11, 0xb5b5b5, 0xd49800, 0x00d4d4, 0x9716ff, 0x0ed700, 0xe40000};
		g2d.setColor(new Color(colors[Math.max(0, (lvl % 215 - 1) / 30)]));
		g2d.fillRect(88 + pad / 2, 59 + pad / 2, (int) ((384 - pad) * prcnt), 10 - pad);

		g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 55));
		Graph.drawOutlinedString(g2d, String.valueOf(lvl), 88, 51, 3, Color.BLACK);

		int offset = (int) (Graph.getStringBounds(g2d, String.valueOf(lvl)).getWidth() + 10);
		g2d.setColor(Color.WHITE);
		g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 25));
		Graph.drawOutlinedString(g2d, account.getName(), 88 + offset, 25, 3, Color.BLACK);

		String details = "XP: %s/%s I Rank: ".formatted(
				Utils.shorten(xp - lvlXp), Utils.shorten(toNext - lvlXp)
		);
		g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 20));
		Graph.drawOutlinedString(g2d, details, 88 + offset, 51, 3, Color.BLACK);

		offset += Graph.getStringBounds(g2d, details).getWidth();
		g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 12));
		Graph.drawOutlinedString(g2d, "#", 88 + offset, 45, 3, Color.BLACK);

		offset += Graph.getStringBounds(g2d, "#").getWidth();
		g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 20));
		Graph.drawOutlinedString(g2d, String.valueOf(account.getRanking()), 88 + offset, 51, 3, Color.BLACK);

		AccountTitle title = account.getTitle();
		if (title != null) {
			g2d.setColor(title.getTitle().getRarity().getColor(false));
			g2d.setFont(Fonts.DOREKING.deriveFont(Font.BOLD, 35));

			String str = title.getTitle().getInfo(locale).getName();
			Graph.drawShadowedString(g2d, str, 524 + (374 - g2d.getFontMetrics().stringWidth(str)) / 2, 70, 15, 2, Color.BLACK);
		}

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
