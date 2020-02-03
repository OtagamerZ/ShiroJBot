/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.handlers.games.rpg.deserializers;

import com.google.gson.*;
import com.kuuhaku.handlers.games.rpg.entities.Item;
import com.kuuhaku.handlers.games.rpg.enums.Equipment;

import java.lang.reflect.Type;

public class ItemDeserializer implements JsonDeserializer<Item> {
	@Override
	public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		Equipment type = Equipment.byName(String.valueOf(json.getAsJsonObject().get("type")));
		switch (type) {
			case HEAD:
				return new Gson().fromJson(json, Item.Head.class);
			case CHEST:
				return new Gson().fromJson(json, Item.Chest.class);
			case LEG:
				return new Gson().fromJson(json, Item.Leg.class);
			case FOOT:
				return new Gson().fromJson(json, Item.Foot.class);
			case ARM:
				return new Gson().fromJson(json, Item.Arm.class);
			case NECK:
				return new Gson().fromJson(json, Item.Neck.class);
			case BAG:
				return new Gson().fromJson(json, Item.Bag.class);
			case RING:
				return new Gson().fromJson(json, Item.Ring.class);
			case WEAPON:
				return new Gson().fromJson(json, Item.Weapon.class);
			case MISC:
				return new Gson().fromJson(json, Item.Misc.class);
		}
		return null;
	}
}
