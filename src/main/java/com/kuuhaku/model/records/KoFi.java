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

package com.kuuhaku.model.records;

import com.squareup.moshi.Json;

import java.time.ZonedDateTime;

public record KoFi(
		ZonedDateTime timestamp,
		String type,
		String message,
		String amount,
		String currency,
		String url,
		String email,

		@Json(name = "message_id")
		String messageID,

		@Json(name = "kofi_transaction_id")
		String kofiTransactionID,

		@Json(name = "from_name")
		String fromName,

		@Json(name = "is_subscription_payment")
		boolean isSubscriptionPayment,

		@Json(name = "is_first_subscription_payment")
		boolean isFirstSubscriptionPayment,

		@Json(name = "is_public")
		boolean isPublic
) {
}