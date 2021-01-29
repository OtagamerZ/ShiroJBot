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

package com.kuuhaku.handlers.games.tabletop.framework;

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.handlers.games.tabletop.framework.enums.BoardSize;
import com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor;
import com.kuuhaku.handlers.games.tabletop.utils.InfiniteList;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.persistent.Account;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Board {
	private final BoardSize size;
	private final InfiniteList<Player> players;
	private final Piece[][] matrix;
	private boolean awarded = false;

	public Board(BoardSize size, long bet, String... players) {
		this.size = size;
		this.players = Arrays.stream(players).map(s -> new Player(s, bet, AccountDAO.getAccount(s).getLoan() > 0)).collect(Collectors.toCollection(InfiniteList::new));
		this.matrix = new Piece[size.getHeight()][size.getWidth()];

		Collections.reverse(this.players);
	}

	public BoardSize getSize() {
		return size;
	}

	public Piece[][] getMatrix() {
		return matrix;
	}

	public void setMatrix(Piece[][] matrix) {
		for (int y = 0; y < this.matrix.length; y++) {
			System.arraycopy(matrix[y], 0, this.matrix[y], 0, this.matrix.length);
		}
	}

	public Piece getPieceAt(Spot s) {
		Piece p = matrix[s.getY()][s.getX()];
		return p instanceof Decoy ? null : p;
	}

	public Piece getPieceOrDecoyAt(Spot s) {
		return matrix[s.getY()][s.getX()];
	}

	public void setPieceAt(Spot s, Piece p) {
		matrix[s.getY()][s.getX()] = p;
	}

	public Piece[] getLine(Spot from, Neighbor vector, boolean inclusive) {
		List<Piece> pieces = new ArrayList<>();

		int[] currentCoords = inclusive ? from.getCoords() : new int[]{from.getX() + vector.getX(), from.getY() + vector.getY()};
		while (true) {
			try {
				pieces.add(matrix[currentCoords[1]][currentCoords[0]]);

				currentCoords[0] += vector.getX();
				currentCoords[1] += vector.getY();
			} catch (ArrayIndexOutOfBoundsException e) {
				break;
			}
		}

		return pieces.toArray(Piece[]::new);
	}

	public void fillLine(Spot from, int distance, Piece with, Neighbor vector, boolean inclusive) {
		int[] currentCoords = inclusive ? from.getCoords() : new int[]{from.getX() + vector.getX(), from.getY() + vector.getY()};
		for (int i = 0; i <= distance; i++) {
			try {
				matrix[currentCoords[1]][currentCoords[0]] = with;

				currentCoords[0] += vector.getX();
				currentCoords[1] += vector.getY();
			} catch (ArrayIndexOutOfBoundsException e) {
				break;
			}
		}
	}

	public Piece[] getColumn(int index) {
		Piece[] column = new Piece[matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			column[i] = matrix[i][index];
		}
		return column;
	}

	public Piece[] getRow(int row) {
		return matrix[row];
	}

	public Piece[] filter(Predicate<Piece> condition) {
		List<Piece> pieces = new ArrayList<>();

		for (Piece[] row : matrix) {
			for (Piece p : row) {
				if (condition.test(p)) pieces.add(p);
			}
		}

		return pieces.toArray(Piece[]::new);
	}

	public InfiniteList<Player> getPlayers() {
		return players;
	}

	public InfiniteList<Player> getInGamePlayers() {
		return players.stream().filter(Player::isInGame).collect(Collectors.toCollection(InfiniteList::new));
	}

	public void leaveGame() {
		Objects.requireNonNull(players.pollFirst()).setInGame(false);
	}

	public void awardWinner(Game game, String id) {
		List<Player> losers = players.stream().filter(p -> !p.getId().equals(id)).collect(Collectors.toList());

		Account wacc = AccountDAO.getAccount(id);
		wacc.addCredit(losers.stream().mapToLong(Player::getBet).sum(), this.getClass());
		AccountDAO.saveAccount(wacc);

		if (ExceedDAO.hasExceed(id)) {
			String wex = ExceedDAO.getExceed(id);
			PoliticalState wps = PStateDAO.getPoliticalState(ExceedEnum.getByName(wex));
			wps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(wex)).count() * 5));
			PStateDAO.savePoliticalState(wps);
		}

		for (Player l : losers) {
			Account lacc = AccountDAO.getAccount(l.getId());
			lacc.removeCredit(l.hasLoan() ? l.getBet() * 2 : l.getBet(), this.getClass());
			AccountDAO.saveAccount(lacc);

			if (ExceedDAO.hasExceed(l.getId())) {
				String lex = ExceedDAO.getExceed(l.getId());
				PoliticalState lps = PStateDAO.getPoliticalState(ExceedEnum.getByName(lex));
				lps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(lex)).count() * 5));
				PStateDAO.savePoliticalState(lps);
			}
		}

		game.close();
		awarded = true;
	}

	public void awardWinner(Game game, boolean daily, String id) {
		List<Player> losers = players.stream().filter(p -> !p.getId().equals(id)).collect(Collectors.toList());

		Account wacc = AccountDAO.getAccount(id);
		wacc.addCredit(losers.stream().mapToLong(Player::getBet).sum(), this.getClass());

		if (ExceedDAO.hasExceed(id)) {
			String wex = ExceedDAO.getExceed(id);
			PoliticalState wps = PStateDAO.getPoliticalState(ExceedEnum.getByName(wex));
			wps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(wex)).count() * (daily && wacc.hasDailyAvailable() ? 25 : 5)));
			PStateDAO.savePoliticalState(wps);
		}

		wacc.playedDaily();
		AccountDAO.saveAccount(wacc);

		for (Player l : losers) {
			Account lacc = AccountDAO.getAccount(l.getId());
			lacc.removeCredit(l.hasLoan() ? l.getBet() * 2 : l.getBet(), this.getClass());

			if (ExceedDAO.hasExceed(l.getId())) {
				String lex = ExceedDAO.getExceed(l.getId());
				PoliticalState lps = PStateDAO.getPoliticalState(ExceedEnum.getByName(lex));
				lps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(lex)).count() * (daily && lacc.hasDailyAvailable() ? 25 : 5)));
				PStateDAO.savePoliticalState(lps);
			}

			lacc.playedDaily();
			AccountDAO.saveAccount(lacc);
		}

		game.close();
		awarded = true;
	}

	public void awardWinners(Game game, String... ids) {
		List<Player> losers = players.stream().filter(p -> ArrayUtils.contains(ids, p.getId())).collect(Collectors.toList());

		for (String id : ids) {
			Account wacc = AccountDAO.getAccount(id);
			wacc.addCredit(losers.stream().mapToLong(Player::getBet).sum() / ids.length, this.getClass());
			AccountDAO.saveAccount(wacc);

			if (ExceedDAO.hasExceed(id)) {
				String wex = ExceedDAO.getExceed(id);
				PoliticalState wps = PStateDAO.getPoliticalState(ExceedEnum.getByName(wex));
				wps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(wex)).count() * 5));
				PStateDAO.savePoliticalState(wps);
			}
		}

		for (Player l : losers) {
			Account lacc = AccountDAO.getAccount(l.getId());
			lacc.removeCredit(l.hasLoan() ? l.getBet() * 2 : l.getBet(), this.getClass());
			AccountDAO.saveAccount(lacc);

			if (ExceedDAO.hasExceed(l.getId())) {
				String lex = ExceedDAO.getExceed(l.getId());
				PoliticalState lps = PStateDAO.getPoliticalState(ExceedEnum.getByName(lex));
				lps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(lex)).count() * 5));
				PStateDAO.savePoliticalState(lps);
			}
		}

		game.close();
		awarded = true;
	}

	public void awardWinners(Game game, boolean daily, String... ids) {
		List<Player> losers = players.stream().filter(p -> ArrayUtils.contains(ids, p.getId())).collect(Collectors.toList());

		for (String id : ids) {
			Account wacc = AccountDAO.getAccount(id);
			wacc.addCredit(losers.stream().mapToLong(Player::getBet).sum() / ids.length, this.getClass());

			if (ExceedDAO.hasExceed(id)) {
				String wex = ExceedDAO.getExceed(id);
				PoliticalState wps = PStateDAO.getPoliticalState(ExceedEnum.getByName(wex));
				wps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(wex)).count() * (daily && wacc.hasDailyAvailable() ? 25 : 5)));
				PStateDAO.savePoliticalState(wps);
			}

			wacc.playedDaily();
			AccountDAO.saveAccount(wacc);
		}

		for (Player l : losers) {
			Account lacc = AccountDAO.getAccount(l.getId());
			lacc.removeCredit(l.hasLoan() ? l.getBet() * 2 : l.getBet(), this.getClass());

			if (ExceedDAO.hasExceed(l.getId())) {
				String lex = ExceedDAO.getExceed(l.getId());
				PoliticalState lps = PStateDAO.getPoliticalState(ExceedEnum.getByName(lex));
				lps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(lex)).count() * (daily && lacc.hasDailyAvailable() ? 25 : 5)));
				PStateDAO.savePoliticalState(lps);
			}

			lacc.playedDaily();
			AccountDAO.saveAccount(lacc);
		}

		game.close();
		awarded = true;
	}

	public void awardWinner(GlobalGame game, String id) {
		List<Player> losers = players.stream().filter(p -> !p.getId().equals(id)).collect(Collectors.toList());

		Account wacc = AccountDAO.getAccount(id);
		wacc.addCredit(losers.stream().mapToLong(Player::getBet).sum(), this.getClass());
		AccountDAO.saveAccount(wacc);

		if (ExceedDAO.hasExceed(id)) {
			String wex = ExceedDAO.getExceed(id);
			PoliticalState wps = PStateDAO.getPoliticalState(ExceedEnum.getByName(wex));
			wps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(wex)).count() * 5));
			PStateDAO.savePoliticalState(wps);
		}

		for (Player l : losers) {
			Account lacc = AccountDAO.getAccount(l.getId());
			lacc.removeCredit(l.hasLoan() ? l.getBet() * 2 : l.getBet(), this.getClass());
			AccountDAO.saveAccount(lacc);

			if (ExceedDAO.hasExceed(l.getId())) {
				String lex = ExceedDAO.getExceed(l.getId());
				PoliticalState lps = PStateDAO.getPoliticalState(ExceedEnum.getByName(lex));
				lps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(lex)).count() * 5));
				PStateDAO.savePoliticalState(lps);
			}
		}

		game.close();
		awarded = true;
	}

	public void awardWinner(GlobalGame game, boolean daily, String id) {
		List<Player> losers = players.stream().filter(p -> !p.getId().equals(id)).collect(Collectors.toList());

		Account wacc = AccountDAO.getAccount(id);
		wacc.addCredit(losers.stream().mapToLong(Player::getBet).sum(), this.getClass());

		if (ExceedDAO.hasExceed(id)) {
			String wex = ExceedDAO.getExceed(id);
			PoliticalState wps = PStateDAO.getPoliticalState(ExceedEnum.getByName(wex));
			wps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(wex)).count() * (daily && wacc.hasDailyAvailable() ? 25 : 5)));
			PStateDAO.savePoliticalState(wps);
		}

		wacc.playedDaily();
		AccountDAO.saveAccount(wacc);

		for (Player l : losers) {
			Account lacc = AccountDAO.getAccount(l.getId());
			lacc.removeCredit(l.hasLoan() ? l.getBet() * 2 : l.getBet(), this.getClass());

			if (ExceedDAO.hasExceed(l.getId())) {
				String lex = ExceedDAO.getExceed(l.getId());
				PoliticalState lps = PStateDAO.getPoliticalState(ExceedEnum.getByName(lex));
				lps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(lex)).count() * (daily && lacc.hasDailyAvailable() ? 25 : 5)));
				PStateDAO.savePoliticalState(lps);
			}

			lacc.playedDaily();
			AccountDAO.saveAccount(lacc);
		}

		game.close();
		awarded = true;
	}

	public void awardWinners(GlobalGame game, String... ids) {
		List<Player> losers = players.stream().filter(p -> ArrayUtils.contains(ids, p.getId())).collect(Collectors.toList());

		for (String id : ids) {
			Account wacc = AccountDAO.getAccount(id);
			wacc.addCredit(losers.stream().mapToLong(Player::getBet).sum() / ids.length, this.getClass());
			AccountDAO.saveAccount(wacc);

			if (ExceedDAO.hasExceed(id)) {
				String wex = ExceedDAO.getExceed(id);
				PoliticalState wps = PStateDAO.getPoliticalState(ExceedEnum.getByName(wex));
				wps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(wex)).count() * 5));
				PStateDAO.savePoliticalState(wps);
			}
		}

		for (Player l : losers) {
			Account lacc = AccountDAO.getAccount(l.getId());
			lacc.removeCredit(l.hasLoan() ? l.getBet() * 2 : l.getBet(), this.getClass());
			AccountDAO.saveAccount(lacc);

			if (ExceedDAO.hasExceed(l.getId())) {
				String lex = ExceedDAO.getExceed(l.getId());
				PoliticalState lps = PStateDAO.getPoliticalState(ExceedEnum.getByName(lex));
				lps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(lex)).count() * 5));
				PStateDAO.savePoliticalState(lps);
			}
		}

		game.close();
		awarded = true;
	}

	public void awardWinners(GlobalGame game, boolean daily, String... ids) {
		List<Player> losers = players.stream().filter(p -> ArrayUtils.contains(ids, p.getId())).collect(Collectors.toList());

		for (String id : ids) {
			Account wacc = AccountDAO.getAccount(id);
			wacc.addCredit(losers.stream().mapToLong(Player::getBet).sum() / ids.length, this.getClass());

			if (ExceedDAO.hasExceed(id)) {
				String wex = ExceedDAO.getExceed(id);
				PoliticalState wps = PStateDAO.getPoliticalState(ExceedEnum.getByName(wex));
				wps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(wex)).count() * (daily && wacc.hasDailyAvailable() ? 25 : 5)));
				PStateDAO.savePoliticalState(wps);
			}

			wacc.playedDaily();
			AccountDAO.saveAccount(wacc);
		}

		for (Player l : losers) {
			Account lacc = AccountDAO.getAccount(l.getId());
			lacc.removeCredit(l.hasLoan() ? l.getBet() * 2 : l.getBet(), this.getClass());

			if (ExceedDAO.hasExceed(l.getId())) {
				String lex = ExceedDAO.getExceed(l.getId());
				PoliticalState lps = PStateDAO.getPoliticalState(ExceedEnum.getByName(lex));
				lps.modifyInfluence((int) (losers.stream().filter(p -> !ExceedDAO.getExceed(p.getId()).equalsIgnoreCase(lex)).count() * (daily && lacc.hasDailyAvailable() ? 25 : 5)));
				PStateDAO.savePoliticalState(lps);
			}

			lacc.playedDaily();
			AccountDAO.saveAccount(lacc);
		}

		game.close();
		awarded = true;
	}

	public BufferedImage render() {
		BufferedImage bi = new BufferedImage(64 * size.getWidth(), 64 * size.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 32));

		for (int y = 0; y < size.getHeight(); y++) {
			for (int x = 0; x < size.getWidth(); x++) {
				g2d.setColor((y + x) % 2 == 0 ? Color.decode("#dcc0b4") : Color.decode("#472d22"));
				g2d.fillRect(64 * x, 64 * y, 64, 64);
				g2d.setColor((y + x) % 2 == 0 ? Color.decode("#472d22") : Color.decode("#dcc0b4"));

				Piece p = matrix[y][x];
				if (p != null && !(p instanceof Decoy)) {
					g2d.drawImage(p.getIcon(), 64 * x, 64 * y, null);
				} else {
					g2d.drawString(String.valueOf(Spot.getAlphabet().toUpperCase().charAt(x)) + (y + 1), 64 * x + 13, 64 * y + 44);
				}
			}
		}
		g2d.dispose();
		return bi;
	}

	public boolean isAwarded() {
		return awarded;
	}
}
