package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.shiro.Card;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Embeddable
public record CardReference(
		@Enumerated(EnumType.STRING)
		@Column(name = "owner")
		Side owner,
		@ManyToOne(optional = false)
		@JoinColumn(name = "card_id")
		@Fetch(FetchMode.JOIN)
		Card card
) {
	public CardReference(Drawable<?> card) {
		this(card.getSide(), card.getCard());
	}

	public CardReference(JSONObject json) {
		this(json.getEnum(Side.class, "owner"), DAO.find(Card.class, json.getString("card")));
	}

	public JSONObject toJSON() {
		JSONObject out = new JSONObject();

		if (owner != null) out.put("owner", owner);
		out.put("card", card.getId());

		return out;
	}
}
