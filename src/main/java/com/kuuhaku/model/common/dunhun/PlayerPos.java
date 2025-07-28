package com.kuuhaku.model.common.dunhun;

public class PlayerPos {
	private int floor;
	private int sublevel;
	private int path;

	public int getFloor() {
		return floor;
	}

	public void setFloor(int floor) {
		this.floor = floor;
	}

	public void moveFloor(int step) {
		this.floor += step;
	}

	public int getSublevel() {
		return sublevel;
	}

	public void setSublevel(int sublevel) {
		this.sublevel = sublevel;
	}

	public void moveSublevel(int step) {
		this.sublevel += step;
	}

	public int getPath() {
		return path;
	}

	public void setPath(int path) {
		this.path = path;
	}

	public void movePath(int step) {
		this.path += step;
	}
}
