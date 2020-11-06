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

package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.utils.Helper;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
public class PatreonHandler {

	@RequestMapping(value = "/webhook/patreon", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
	public void handleVote(@RequestHeader(value = "X-Patreon-Signature") String signature, @RequestBody String payload) {
		if (!System.getenv().containsKey("PATREON_SECRET")) return;
		Helper.logger(this.getClass()).info("Signature: " + signature);
		Helper.logger(this.getClass()).info("Body MD5: " + Helper.hmac(payload.getBytes(StandardCharsets.UTF_8), System.getenv("PATREON_SECRET").getBytes(StandardCharsets.UTF_8), "MD5"));
		Helper.logger(this.getClass()).info("Body: " + payload);
	}
}
