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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Blacklistable;
import com.kuuhaku.interfaces.annotations.WhenNull;
import com.kuuhaku.model.common.Checkpoint;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.RuleAction;
import com.kuuhaku.model.persistent.guild.AutoRule;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.id.ProfileId;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jdesktop.swingx.graphics.BlendComposite;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "profile", indexes = @Index(columnList = "xp DESC"))
public class Profile extends DAO<Profile> implements Blacklistable {
	private static final Dimension SIZE = new Dimension(950, 600);

	@EmbeddedId
	private ProfileId id;

	@Column(name = "xp", nullable = false)
	private long xp;

	@Column(name = "last_xp", nullable = false)
	private long lastXp;

	@OneToMany(mappedBy = "profile", cascade = ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private final List<Warn> warns = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "uid", nullable = false, updatable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("uid")
	private Account account;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "gid", nullable = false, updatable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("gid")
	private GuildConfig guild;

	@OneToOne(fetch = FetchType.LAZY, cascade = ALL, orphanRemoval = true)
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
	}

	public ProfileId getId() {
		return id;
	}

	public long getXp() {
		return xp;
	}

	public void addXp(long value) {
		if (System.currentTimeMillis() - lastXp >= 1000) {
			xp += value;
			lastXp = System.currentTimeMillis();
		}
	}

	public int getLevel() {
		return (int) Math.max(1, Math.floor(Math.sqrt(xp / 100d)) + 1);
	}

	public long getXpToLevel(int level) {
		return (int) (Math.pow(level - 1, 2) * 100);
	}

	public long getXpToLevel(int from, int to) {
		return Math.max(0, getXpToLevel(to) - getXpToLevel(from));
	}

	public List<Warn> getWarns() {
		return warns;
	}

	public int getWarnCount() {
		return (int) warns.parallelStream()
				.filter(w -> w.getPardoner() == null)
				.count();
	}

	public void warn(User issuer, String reason) {
		apply(Profile.class, id, p -> p.getWarns().add(new Warn(p, issuer, reason)));

		AutoRule rule = null;
		int mult = 0;
		int total = getWarnCount();
		for (AutoRule r : guild.getSettings().getAutoRules()) {
			if (r.getThreshold() <= total) {
				if (r.getAction() != RuleAction.AGGRAVATE) {
					rule = r;
					mult = 0;
				} else {
					mult = Math.min(mult + 1, 4);
				}
			}
		}

		if (rule != null) {
			I18N locale = guild.getLocale();
			String cause = locale.get("str/autorule_desc",
					locale.get("str/autorule_" + rule.getAction()),
					rule.getThreshold()
			);

			Member m = getMember();
			int finalMult = mult;
			m.getUser().openPrivateChannel()
					.flatMap(c -> c.sendMessage(locale.get("alert/autorule_trigger", cause, finalMult + 1)))
					.queue(null, Utils::doNothing);

			switch (rule.getAction()) {
				case MUTE -> m.timeoutFor((long) (1 * Math.pow(2, mult)), TimeUnit.MINUTES)
						.reason(cause + " (A" + (mult + 1) + ")")
						.queue(null, Utils::doNothing);
				case LOSE_XP -> {
					long range = getXpToLevel(getLevel(), getLevel() + 1);
					xp = (long) (xp - (range * (0.05 * Math.pow(2, mult))));
				}
				case DELEVEL -> {
					long range = getXpToLevel((int) (getLevel() - Math.pow(2, mult)), getLevel());
					xp = Math.max(0, xp - range);
				}
				case KICK -> m.kick()
						.reason(cause + " (x" + (mult + 1) + ")")
						.queue(null, Utils::doNothing);
				case BAN -> m.ban(7, TimeUnit.DAYS)
						.reason(cause + " (x" + (mult + 1) + ")")
						.queue(null, Utils::doNothing);
			}
		}
	}

	public Account getAccount() {
		return account;
	}

	public GuildConfig getGuild() {
		return guild;
	}

	public ProfileSettings getSettings() {
		if (settings == null) {
			this.settings = new ProfileSettings(id);
		}

		return settings;
	}

	public Member getMember() {
		Guild guild = Main.getApp().getShiro().getGuildById(getId().getGid());
		assert guild != null;

		return Pages.subGet(guild.retrieveMemberById(getId().getUid()));
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

	public RichCustomEmoji getLevelEmote() {
		return Main.getApp().getShiro()
				.getEmojisByName("lvl_" + (getLevel() - getLevel() % 5), false)
				.parallelStream()
				.findAny().orElseThrow();
	}

	public BufferedImage render(I18N locale) {
		try (Checkpoint cp = new Checkpoint()) {
			BufferedImage mask = IO.getResourceAsImage("assets/masks/profile_mask.png");
			BufferedImage overlay = IO.getResourceAsImage("assets/profile_overlay.png");

			cp.lap("1");
			AccountSettings settings = account.getSettings();
			BufferedImage bg = IO.getImage(settings.getBackground());
			if (bg == null) {
				settings.setBackground(null);
				bg = IO.getImage(settings.getBackground());
			}
			bg = Graph.scaleAndCenterImage(Graph.toColorSpace(bg, BufferedImage.TYPE_INT_ARGB), SIZE.width, SIZE.height);

			cp.lap("2");
			BufferedImage bi = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_ARGB);

			cp.lap("3");
			Graphics2D g2d = bi.createGraphics();
			g2d.setRenderingHints(Constants.HD_HINTS);

			cp.lap("4");
			BufferedImage avatar = null;
			Guild g = Main.getApp().getShiro().getGuildById(id.getGid());
			if (g != null) {
				Member m = g.getMemberById(id.getUid());
				if (m != null) {
					avatar = IO.getImage(m.getEffectiveAvatarUrl());
				}
			}

			cp.lap("5");
			g2d.drawImage(Utils.getOr(avatar, IO.getImage(account.getUser().getEffectiveAvatarUrl())), -23, 56, 150, 150, null);

			cp.lap("6");
			Graph.applyMask(bg, mask, 0);
			g2d.drawImage(bg, 0, 0, null);

			cp.lap("7");
			Color color;
			if (settings.getColor().equals(Color.BLACK)) {
				color = Graph.rotate(Graph.getColor(bg), 180);
			} else {
				color = settings.getColor();
			}

			cp.lap("8");
			Graphics2D og2d = overlay.createGraphics();
			og2d.setComposite(BlendComposite.Multiply);
			og2d.setColor(color);
			og2d.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
			og2d.dispose();

			cp.lap("9");
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

			cp.lap("10");
			BufferedImage effect = settings.getEffect().getImage();
			Graph.applyMask(effect, mask, 0, true);
			g2d.drawImage(effect, 0, 0, null);

			cp.lap("11");
			Map<String, Object> replaces = new HashMap<>();
			replaces.put("guild", getGuild().getName());
			replaces.put("g_rank", Utils.separate(account.getRanking()));
			replaces.put("l_rank", Utils.separate(getRanking()));
			replaces.put("xp", Utils.shorten(xp));
			replaces.put("level", getLevel());

			cp.lap("12");
			for (AccountTitle title : account.getTitles()) {
				Title t = title.getTitle();
				if (t.getId().startsWith("SS_")) continue;

				replaces.put(
						t.getId().toLowerCase(),
						"#%06X,%s".formatted(
								t.getRarity().getColor(false).getRGB() & 0xFFFFFF,
								t.getInfo(locale).getName().replace(" ", "_")
						)
				);
			}

			cp.lap("13");
			Graph.applyTransformed(g2d, g1 -> {
				Color bgCol = new Color((200 << 24) | (color.getRGB() & 0xFFFFFF), true);

				g1.setClip(inner);
				g1.setColor(bgCol);

				RoundRectangle2D wids = new RoundRectangle2D.Double(-14, 210, 200, 50, 20, 20);

				cp.lap("14");
				int em = g1.getFontMetrics().getHeight();
				g1.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(20));
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

				cp.lap("15");
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

			cp.lap("16");
			Graph.applyMask(overlay, mask, 1);
			g2d.drawImage(overlay, 0, 0, null);

			cp.lap("17");
			BufferedImage emote = IO.getImage(getLevelEmote().getImageUrl());
			g2d.drawImage(emote, 6, -3, 81, 81, null);

			cp.lap("18");
			g2d.setColor(Color.GRAY);
			Graph.drawOutlined(g2d, new Rectangle(88, 59, 384, 10), 2, Color.BLACK);

			cp.lap("19");
			int lvl = getLevel();
			long lvlXp = getXpToLevel(lvl);
			long toNext = getXpToLevel(lvl + 1);
			int pad = 4;
			double prcnt = Math.max(0, Calc.prcnt(xp - lvlXp, toNext - lvlXp));
			int[] colors = {0x5b2d11, 0xb5b5b5, 0xd49800, 0x00d4d4, 0x9716ff, 0x0ed700, 0xe40000};

			cp.lap("20");
			g2d.setColor(new Color(colors[Math.max(0, (lvl % 210) / 30)]));
			g2d.fillRect(88 + pad / 2, 59 + pad / 2, (int) ((384 - pad) * prcnt), 10 - pad);

			cp.lap("21");
			g2d.setFont(Fonts.DOREKING.derivePlain(55));
			Graph.drawOutlinedString(g2d, String.valueOf(lvl), 88, 51, 3, Color.BLACK);

			cp.lap("22");
			int offset = (int) (Graph.getStringBounds(g2d, String.valueOf(lvl)).getWidth() + 10);
			g2d.setColor(Color.WHITE);
			g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(25));
			Graph.drawOutlinedString(g2d, account.getName(), 88 + offset, 25, 3, Color.BLACK);

			cp.lap("23");
			String details = "XP: %s/%s I Rank: ".formatted(
					Utils.shorten(xp - lvlXp), Utils.shorten(toNext - lvlXp)
			);
			g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(20));
			Graph.drawOutlinedString(g2d, details, 88 + offset, 51, 3, Color.BLACK);

			cp.lap("24");
			offset += (int) Graph.getStringBounds(g2d, details).getWidth() + 5;
			g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(12));
			Graph.drawOutlinedString(g2d, "#", 88 + offset, 45, 3, Color.BLACK);

			cp.lap("25");
			offset += (int) Graph.getStringBounds(g2d, "#").getWidth();
			g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(20));
			Graph.drawOutlinedString(g2d, String.valueOf(account.getRanking()), 88 + offset, 51, 3, Color.BLACK);

			cp.lap("26");
			AccountTitle title = account.getTitle();
			if (title != null) {
				g2d.setColor(title.getTitle().getRarity().getColor(false));
				g2d.setFont(Fonts.DOREKING.deriveBold(35));

				String str = title.getTitle().getInfo(locale).getName();
				Graph.drawShadowedString(g2d, str, 524 + (374 - g2d.getFontMetrics().stringWidth(str)) / 2, 70, 15, 2, Color.BLACK);
			}

			g2d.dispose();

			return bi;
		}
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
		return Objects.hashCode(id);
	}
}
