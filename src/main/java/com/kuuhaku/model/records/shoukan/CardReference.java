package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.shiro.Card;
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
}
