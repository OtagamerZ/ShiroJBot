/*
 * Copyright (C) 2019 Yago Garcia Sanches Gimenez / KuuHaKu
 *
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see https://www.gnu.org/licenses/
 */

package com.kuuhaku.model;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

public class Badges {
    public static String getBadges(boolean[] conquistas) {
        String conq = "";
        conq += (conquistas[0] ? ":white_check_mark:" : ":x:") + " 0 - Moderador!\n\n";
        conq += (conquistas[1] ? ":white_check_mark:" : ":x:") + " 1 - Desafiando quem?\n\n";
        conq += (conquistas[2] ? ":white_check_mark:" : ":x:") + " 2 - Isso é lootável?\n\n";
        conq += (conquistas[3] ? ":white_check_mark:" : ":x:") + " 3 - Humpf, achei fácil!\n\n";
        conq += (conquistas[4] ? ":white_check_mark:" : ":x:") + " 4 - Temos um Sherok Homer aqui!\n\n";
        conq += (conquistas[5] ? ":white_check_mark:" : ":x:") + " 5 - Salvo....por pouco!\n\n";
        conq += (conquistas[6] ? ":white_check_mark:" : ":x:") + " 6 - Ummm, então é isso, né?\n\n";
        conq += (conquistas[7] ? ":white_check_mark:" : ":x:") + " 7 - Treinador iniciante.\n\n";
        conq += (conquistas[8] ? ":white_check_mark:" : ":x:") + " 8 - Mestre treinador.\n\n";
        conq += (conquistas[9] ? ":white_check_mark:" : ":x:") + " 9 - Amante de nekos.\n\n";
        conq += (conquistas[10] ? ":white_check_mark:" : ":x:") + " 10 - Escudeiro nota 10!\n\n";
        conq += (conquistas[11] ? ":white_check_mark:" : ":x:") + " 11 - Recrutador de elite!\n\n";
        conq += (conquistas[12] ? ":white_check_mark:" : ":x:") + " 12 - Viajante das estrelas!\n\n";

        return conq;
    }

    public static MessageEmbed getBadgeDesc(String index) throws Exception {
        EmbedBuilder eb = new EmbedBuilder();

        switch (Integer.parseInt(index)) {
            case 0:
                eb.setTitle("Moderador!");
                eb.setDescription("Vença o evento de promoção.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/563719635565019146/cho-moderator-badge-2.png");
                eb.addField("Dificuldade:", "8/10", true);
                return eb.build();
            case 1:
                eb.setTitle("Desafiando quem?");
                eb.setDescription("Alcance o cargo Desafiante.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/563721202040963083/challenger.png");
                eb.addField("Dificuldade:", "6/10", true);
                return eb.build();
            case 2:
                eb.setTitle("Isso é lootável?");
                eb.setDescription("Participe de uma expedição no evento RPG.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/563721800408629248/Icon_Heroes_Chest.png");
                eb.addField("Dificuldade:", "3/10", true);
                return eb.build();
            case 3:
                eb.setTitle("Humpf, achei fácil!");
                eb.setDescription("Vença uma campanha no evento RPG.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/563722369156382741/unnamed.png");
                eb.addField("Dificuldade:", "5/10", true);
                return eb.build();
            case 4:
                eb.setTitle("Temos um Sherok Homer aqui!");
                eb.setDescription("Colha provas e descubra o culpado de uma infração.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/563723291567587333/images.png");
                eb.addField("Dificuldade:", "1/10", true);
                return eb.build();
            case 5:
                eb.setTitle("Salvo....por pouco!");
                eb.setDescription("Chegue à 9 alertas e consiga reduzir a 0.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/563723686021169171/RcAyodd4i.png");
                eb.addField("Dificuldade:", "9/10", true);
                return eb.build();
            case 6:
                eb.setTitle("Ummm, então é isso, né?");
                eb.setDescription("Consiga o cargo secreto.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/563725863523319811/1200x630bb.png");
                eb.addField("Dificuldade:", "10/10", true);
                return eb.build();
            case 7:
                eb.setTitle("Treinador iniciante.");
                eb.setDescription("Vença um líder de ginásio.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/577652669070704640/unnamed.png");
                eb.addField("Dificuldade:", "4/10", true);
                return eb.build();
            case 8:
                eb.setTitle("Mestre treinador.");
                eb.setDescription("Vença todos os líderes de ginásio.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/577652652121260062/master_ball_by_peetzaahhh2010_d93nt8r-fullview.png");
                eb.addField("Dificuldade:", "7/10", true);
                return eb.build();
            case 9:
                eb.setTitle("Amante de nekos.");
                eb.setDescription("Use o comando ->cat ou ->catgirl 1000 vezes.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/563742638407155742/1471089838_Q74iLzn.png");
                eb.addField("Dificuldade:", "5/10", true);
                return eb.build();
            case 10:
                eb.setTitle("Escudeiro nota 10!");
                eb.setDescription("Ajude um administrador ou o Overlord em um teste.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/565313072441327618/blacktoftbeacon-scouts_ex-cs-cspa.png");
                eb.addField("Dificuldade:", "1/10", true);
                return eb.build();
            case 11:
                eb.setTitle("Recrutador de elite!");
                eb.setDescription("Convide 25 pessoas para este servidor, e consiga mantê-las por 1 mês.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/563875508077461515/cc7f9ed5b382b35efdb9555a5f6ebdba_15.png");
                eb.addField("Dificuldade:", "6/10", true);
                return eb.build();
            case 12:
                eb.setTitle("Viajante das estrelas!");
                eb.setDescription("Tenha um comentário seu fixado no starboard.");
                eb.setThumbnail("https://cdn.discordapp.com/attachments/421517121411874816/566397167741173780/kisspng-computer-icons-flat-design-clip-art-gold-stars-5ad199c9befbd6.3779426015236858337823.png");
                eb.addField("Dificuldade:", "3/10", true);
                return eb.build();
        }
        throw new Exception();
    }
}
