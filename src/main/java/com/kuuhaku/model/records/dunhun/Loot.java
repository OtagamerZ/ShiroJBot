package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.util.Utils;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.TreeBag;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public record Loot(I18N locale, List<Gear> gear, Bag<UserItem> items, AtomicInteger xp) {
	public Loot(I18N locale) {
		this(locale,
				new ArrayList<>(),
				new TreeBag<>(Comparator.comparing(UserItem::getId)),
				new AtomicInteger()
		);
	}

	public void add(Loot lt) {
		gear.addAll(lt.gear());
		items.addAll(lt.items());
		xp.addAndGet(lt.xp.get());
	}

	public void remove(Loot lt) {
		lt.gear().forEach(gear::remove);
		lt.items().forEach(items::remove);
		xp.addAndGet(-lt.xp.get());
	}

	public List<String> names() {
		List<String> out = new ArrayList<>();
		for (Gear g : gear) {
			out.add(g.getName(locale));
		}
		for (UserItem i : items.uniqueSet()) {
			out.add(items.getCount(i) + "x " + i.getName(locale));
		}

		return out;
	}

	@Override
	public @NonNull String toString() {
		return Utils.properlyJoin(locale, names());
	}
}
