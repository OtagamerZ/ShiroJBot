package com.kuuhaku.managers;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.persistent.RaidInfo;
import com.kuuhaku.model.persistent.RaidMember;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.records.RaidData;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.awt.*;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class RaidManager {
	private final ConcurrentMap<String, RaidData> raids = ExpiringMap.builder()
			.<String, RaidData>expirationListener((gid, data) -> {
				Guild guild = Main.getShiro().getGuildById(gid);
				if (guild == null) return;

				GuildConfig gc = GuildDAO.getGuildById(guild.getId());
				TextChannel chn = gc.getGeneralChannel();

				long duration = System.currentTimeMillis() - data.start();
				Set<String> ids = data.ids();
				if (chn != null) {
					EmbedBuilder eb = new EmbedBuilder()
							.setColor(Color.green)
							.setTitle("**RELATÓRIO DO SISTEMA R.A.ID**")
							.setDescription("""
									Detectado fim da raid, usuários podem voltar à rotina normal.
									          
									Duração da raid: %s
									Usuários banidos: %s
																		
									O relatório completo pode ser encontrado no comando `raids`.
									""".formatted(StringHelper.toStringDuration(duration), ids.size())
							);

					chn.sendMessageEmbeds(eb.build()).queue(null, MiscHelper::doNothing);
				}

				for (TextChannel tc : guild.getTextChannels()) {
					try {
						if (guild.getPublicRole().hasPermission(tc, Permission.MESSAGE_WRITE)) {
							tc.getManager().setSlowmode(0).queue(null, MiscHelper::doNothing);
						}
					} catch (Exception ignore) {
					}
				}

				if (!ids.isEmpty()) {
					RaidInfo info = new RaidInfo(guild.getId(), duration);
					for (String id : ids) {
						info.getMembers().add(new RaidMember(id, guild.getId()));
					}
					info.save();
				}
			})
			.expirationPolicy(ExpirationPolicy.ACCESSED)
			.expiration(10, TimeUnit.SECONDS)
			.build();

	public ConcurrentMap<String, RaidData> getRaids() {
		return raids;
	}
}
