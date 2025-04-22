package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.persistent.shiro.Card;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Embeddable
public record CardReference(
		@Column(name = "owner")
		String owner,
		@ManyToOne(optional = false)
		@JoinColumn(name = "card_id")
		@Fetch(FetchMode.JOIN)
		Card card
) {
	public CardReference(Drawable<?> card) {
		this(card.getHand().getUid(), card.getCard());
	}
}
