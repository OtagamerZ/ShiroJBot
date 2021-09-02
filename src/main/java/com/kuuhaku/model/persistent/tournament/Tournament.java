/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.tournament;

import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.jdesktop.swingx.graphics.BlendComposite;

import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

@Entity
@Table(name = "tournament")
public class Tournament {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String name;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int seed = new Random().nextInt();

	@LazyCollection(LazyCollectionOption.FALSE)
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "tournament")
	private Set<Participant> ranking = new TreeSet<>(
			Comparator.comparingInt(Participant::getPoints).reversed()
					.thenComparingInt(Participant::getIndex)
					.thenComparing(Participant::isThird).reversed()
	);

	@LazyCollection(LazyCollectionOption.FALSE)
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "tournament")
	private List<Participant> thirdPlace = Arrays.asList(null, null);

	@LazyCollection(LazyCollectionOption.FALSE)
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "tournament")
	private List<Participant> participants = new ArrayList<>();

	@OneToOne(fetch = FetchType.EAGER)
	private Bracket bracket;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean closed = false;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int size = 0;

	private static final int H_MARGIN = 150;
	private static final int V_MARGIN = 25;
	private static final int WIDTH = 400;
	private static final int HEIGHT = 100;

	private static final Color BG_COLOR = new Color(0x2f3136);
	private static final Color PRIMARY_COLOR = new Color(0x8e9297);
	private static final Color SECONDARY_COLOR = new Color(0x40444b);

	public Tournament() {
	}

	public Tournament(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<Participant> getParticipants() {
		return participants;
	}

	public Bracket getBracket() {
		return bracket;
	}

	public boolean isClosed() {
		return closed;
	}

	public void register(String id) {
		participants.add(new Participant(id, this.id));
	}

	public void close() {
		closed = true;
		size = Helper.roundToBit(participants.size());
		bracket = new Bracket(size);
		bracket.populate(this, participants);
	}

	public void setResult(int phase, int index, Participant winner) {
		if (phase == bracket.getPhases().size()) {
			winner.setThird();
			ranking.addAll(participants);
		} else {
			Pair<Participant, Participant> mtch = bracket.getPhases().get(phase).getMatch(index);
			Participant other = Objects.equals(mtch.getLeft(), winner) ? mtch.getRight() : mtch.getLeft();

			boolean top = winner.getIndex() % 2 == 0;
			winner.won(phase);
			Phase next = bracket.getPhases().get(phase + 1);
			next.setMatch(index / 2, winner);

			if (phase == bracket.getPhases().size() - 3) {
				thirdPlace.set(top ? 1 : 0, other);
			}

			if (next.isLast()) {
				ranking.add(winner);
				ranking.add(other);
			}
		}
	}

	public BufferedImage view() {
		int phases = bracket.getPhases().size();

		BufferedImage bi = new BufferedImage((WIDTH + H_MARGIN) * phases + WIDTH - H_MARGIN + 10, (HEIGHT + V_MARGIN) * size - V_MARGIN + 10, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setBackground(BG_COLOR);
		g2d.clearRect(0, 0, bi.getWidth(), bi.getHeight());
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(new BasicStroke(8, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

		g2d.setColor(Color.white);
		g2d.setFont(Fonts.DOREKING.deriveFont(Font.BOLD, 200));
		Helper.drawCenteredString(g2d, name, (WIDTH + H_MARGIN) * 3 + 5, 5, bi.getWidth() - (WIDTH + H_MARGIN) * 3, 400);

		for (int i = 0; i < phases; i++) {
			int x = (WIDTH + H_MARGIN) * i + 5;

			Phase p = bracket.getPhases().get(i);
			int mult = p.isLast() ? 2 : 1;

			List<Participant> ps = p.getParticipants();
			int pSize = p.getSize();
			for (int k = 0; k < pSize; k++) {
				Participant part = ps.size() > k ? ps.get(k) : null;

				int y = (bi.getHeight() - 10) / pSize * k + 5;
				int offset = (bi.getHeight() - 10) / pSize / 2 - HEIGHT * mult / 2;
				boolean winner = part != null && part.isWinner(i) && !part.isBye();

				g2d.setColor(SECONDARY_COLOR);
				g2d.fillRoundRect(x, y + offset, WIDTH * mult, HEIGHT * mult, 50, 50);

				if (i < phases - 1) {
					int y2 = (bi.getHeight() - 10) / (pSize / 2) * (k / 2);
					int offset2 = (bi.getHeight() - 10) / (pSize / 2) / 2 - HEIGHT * mult / 2;

					g2d.setColor(winner ? Color.white : SECONDARY_COLOR);
					Composite comp = g2d.getComposite();
					g2d.setComposite(BlendComposite.Lighten);
					Helper.drawSquareLine(g2d, x + WIDTH, y + offset + HEIGHT / 2, (WIDTH + H_MARGIN) * (i + 1), y2 + offset2 + HEIGHT / 2);
					g2d.setComposite(comp);
				}

				if (part != null) {
					if (part.isBye()) {
						g2d.setColor(PRIMARY_COLOR);
						g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.BOLD | Font.ITALIC, 50));
						Helper.drawCenteredString(g2d, "BYE", x, y + offset, WIDTH, (HEIGHT - 20));
					} else {
						g2d.setColor(winner ? Color.white : PRIMARY_COLOR);
						g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.BOLD, 50 * mult));
						Helper.drawCenteredString(g2d, StringUtils.abbreviate(part.toString(), 13), x, y + offset, WIDTH * mult, (HEIGHT - 20) * mult);
					}
				}

				if (mult == 2) {
					g2d.setColor(Color.orange);
					g2d.drawRoundRect(x, y + offset, WIDTH * mult, HEIGHT * mult, 50, 50);

					//SECOND PLACE
					g2d.setColor(SECONDARY_COLOR);
					g2d.fillRoundRect(x, y + offset + (HEIGHT + V_MARGIN) * mult, WIDTH * mult, HEIGHT * mult, 50, 50);

					Participant second = ranking.isEmpty() ? null : ((TreeSet<Participant>) ranking).lower(((TreeSet<Participant>) ranking).first());
					if (second != null) {
						g2d.setColor(Color.white);
						g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.BOLD, 50 * mult));
						Helper.drawCenteredString(g2d, second.toString(), x, y + offset + (HEIGHT + V_MARGIN) * mult, WIDTH * mult, (HEIGHT - 20) * mult);
					}

					g2d.setColor(Color.lightGray);
					g2d.drawRoundRect(x, y + offset + (HEIGHT + V_MARGIN) * mult, WIDTH * mult, HEIGHT * mult, 50, 50);
					//SECOND PLACE

					//THIRD PLACE
					g2d.setColor(SECONDARY_COLOR);
					g2d.fillRoundRect(x, y + offset + (HEIGHT + V_MARGIN) * mult * 2, WIDTH * mult, HEIGHT * mult, 50, 50);

					Participant third = second == null ? null : ((TreeSet<Participant>) ranking).lower(second);
					if (third != null) {
						g2d.setColor(Color.white);
						g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.BOLD, 50 * mult));
						Helper.drawCenteredString(g2d, second.toString(), x, y + offset + (HEIGHT + V_MARGIN) * mult * 2, WIDTH * mult, (HEIGHT - 20) * mult);
					}

					g2d.setColor(new Color(0x7C3600));
					g2d.drawRoundRect(x, y + offset + (HEIGHT + V_MARGIN) * mult * 2, WIDTH * mult, HEIGHT * mult, 50, 50);
					//THIRD PLACE
				} else {
					g2d.setColor(winner ? Color.white : PRIMARY_COLOR);
					g2d.drawRoundRect(x, y + offset, WIDTH * mult, HEIGHT * mult, 50, 50);
				}
			}
		}

		g2d.dispose();
		return bi;
	}
}
