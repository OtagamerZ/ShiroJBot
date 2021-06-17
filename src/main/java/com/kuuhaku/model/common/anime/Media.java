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

package com.kuuhaku.model.common.anime;

import java.util.List;

public class Media {
	private long idMal;
	private Title title;
	private String status;
	private StartDate startDate;
	private long episodes;
	private CoverImage coverImage;
	private List<String> genres;
	private long averageScore;
	private long popularity;
	private Studios studios;
	private Staff staff;
	private NextAiringEpisode nextAiringEpisode;
	private Trailer trailer;
	private String description;

	public long getIdMal() {
		return idMal;
	}

	public void setIdMal(long value) {
		this.idMal = value;
	}

	public Title getTitle() {
		return title;
	}

	public void setTitle(Title value) {
		this.title = value;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String value) {
		this.status = value;
	}

	public StartDate getStartDate() {
		return startDate;
	}

	public void setStartDate(StartDate value) {
		this.startDate = value;
	}

	public long getEpisodes() {
		if (nextAiringEpisode != null)
			return Math.max(episodes, nextAiringEpisode.getEpisode() - 1);
		else return episodes;
	}

	public void setEpisodes(long value) {
		this.episodes = value;
	}

	public CoverImage getCoverImage() {
		return coverImage;
	}

	public void setCoverImage(CoverImage value) {
		this.coverImage = value;
	}

	public List<String> getGenres() {
		return genres;
	}

	public void setGenres(List<String> value) {
		this.genres = value;
	}

	public long getAverageScore() {
		return averageScore;
	}

	public void setAverageScore(long value) {
		this.averageScore = value;
	}

	public long getPopularity() {
		return popularity;
	}

	public void setPopularity(long value) {
		this.popularity = value;
	}

	public Studios getStudios() {
		return studios;
	}

	public void setStudios(Studios value) {
		this.studios = value;
	}

	public Staff getStaff() {
		return staff;
	}

	public void setStaff(Staff value) {
		this.staff = value;
	}

	public NextAiringEpisode getNextAiringEpisode() {
		return nextAiringEpisode;
	}

	public void setNextAiringEpisode(NextAiringEpisode value) {
		this.nextAiringEpisode = value;
	}

	public Trailer getTrailer() {
		return trailer;
	}

	public void setTrailer(Trailer value) {
		this.trailer = value;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String value) {
		this.description = value;
	}
}
