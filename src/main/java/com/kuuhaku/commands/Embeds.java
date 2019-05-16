package com.kuuhaku.commands;

import com.kuuhaku.controller.Tradutor;
import com.kuuhaku.model.Anime;
import com.kuuhaku.model.Badges;
import com.kuuhaku.model.Member;
import com.kuuhaku.model.guildConfig;
import de.androidpit.colorthief.ColorThief;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Embeds {
    public static MessageEmbed bugReport(MessageReceivedEvent message, String prefix) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setAuthor("Novo bug enviado de " + message.getGuild().getName());
        eb.addField("Enviado de:", message.getAuthor().getAsMention(), true);
        eb.addField("Data:", message.getMessage().getCreationTime().minusHours(3).format(DateTimeFormatter.ofPattern("dd/MM/yyyy (HH:mm)")), true);
        eb.addField("Relatório do bug:", String.join(" ", message.getMessage().getContentRaw().split(prefix + "bug ")), false);

        return eb.build();
    }

    static String helpEmbed(String prefix) {
        return ":closed_lock_with_key: **Administrativo**```" +
                prefix + "definir prefixo [prefixo] - Define o prefixo para o servidor.\n\n" +
                prefix + "definir canalbv [canal] - Define o canal de boas-vindas para o servidor.\n\n" +
                prefix + "definir canalav [canal] - Define o canal de avisos para o servidor.\n\n" +
                prefix + "definir msgbv [\"mensagem\"] - Define a mensagem de boas-vindas para o servidor.\n\n" +
                prefix + "definir msgadeus [\"mensagem\"] - Define a mensagem de adeus para o servidor.\n\n" +
                prefix + "definir cargolvl [level] [cargo] - Define um novo cargo como recompensa para o level especificado.\n\n" +
                prefix + "remover cargolvl [level] - Remove a recompensa do level especificado.\n\n" +
                prefix + "configs - Mostra as configurações do servidor.\n\n" +
                prefix + "alertar [membro] [razão] - Registra um alerta no perfil do membro especificado.\n\n" +
                prefix + "punir [membro] - Reseta o XP de um membro.\n\n" +
                prefix + "perdoar [membro] [Nº] - Perdoa um alerta do membro.```\n" +

                ":speech_balloon: **Utilitário**\n```" +
                prefix + "ajuda - Mostra essa mensagem no seu canal privado.\n\n" +
                prefix + "bug [mensagem] - Envia um bug para meu Nii-chan corrigir.\n\n" +
                prefix + "ping - Confere se estou online e funcionando direitinho.\n\n" +
                prefix + "uptime - Descobre a quanto tempo estou acordada.\n\n" +
                prefix + "imagem [tags] [página] - Busca uma imagem no Safebooru, as tags não podem conter espaços (substitua-os por _).\n\n" +
                prefix + "anime [nome] - Pesquisa informações sobre um anime.\n\n" +
                prefix + "embed [título;descrição;imagem] - Cria um novo embed com os dados passados.\n\n" +
                prefix + "xp - Mostra dados sobre o seu perfil.```\n";
    }

    static String helpEmbed2(String prefix) {
        return ":juggling: **Diversão**\n```" +
                prefix + "pergunta [pergunta] - Me pergunte algo, mas só vou responder com sim ou não!\n\n" +
                prefix + "escolha [opção1;opção2;opção3;...] - Quer que eu escolha entre essas opções? Facil!\n\n" +
                prefix + "vemca [membro] - Dá um abraço exagerado em alguém.\n\n" +
                prefix + "meee - Bate a cara.\n\n" +
                prefix + "abraçar [membro] - Dá um abraço em alguém.\n\n" +
                prefix + "sqn - Esquiva de uma tentativa.\n\n" +
                prefix + "corre - Sai correndo.\n\n" +
                prefix + "tapa [membro] - Dá um tapa em alguém.\n\n" +
                prefix + "vemca [membro] - Dá um abraço exagerado em alguém.\n\n" +
                prefix + "chega [membro] - Retalha alguém.\n\n" +
                prefix + "encara [membro] - Encara alguém.```\n" +

                ":gem: **OtagamerZ**\n```" +
                prefix + "conquistas - Mosta as conquistas que você completou.\n\n" +
                prefix + "conquista [Nº] - Mostra informações detalhadas de uma conquista.```";
    }

    static MessageEmbed imageEmbed(String[] tag, String index) throws IOException {
        URL link = new URL("https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1&limit=1&rating=safe&tags=" +
                String.join("+", tag) + "&pid=" + index);
        HttpURLConnection con = (HttpURLConnection) link.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.addRequestProperty("Accept", "application/json");
        System.out.println("Requisição 'GET' para o URL: " + link);
        System.out.println("Resposta: " + con.getResponseCode());
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String input;
        StringBuilder resposta = new StringBuilder();
        while ((input = br.readLine()) != null) {
            resposta.append(input);
        }
        br.close();
        con.disconnect();

        System.out.println(resposta.toString());
        JSONObject jo = new JSONObject(resposta.toString().replace("[", "").replace("]", ""));

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.MAGENTA);
        eb.setAuthor("Aqui está!", "https://safebooru.org//images/" + jo.getString("directory") + "/" + jo.getString("image"));
        eb.addField("Largura:", Integer.toString(jo.getInt("width")), true);
        eb.addField("Altura:", Integer.toString(jo.getInt("height")), true);
        eb.addField("Tags:", "`" + String.join("` `", jo.getString("tags").split(" ")) + "`", false);
        eb.setImage("https://safebooru.org//images/" + jo.getString("directory") + "/" + jo.getString("image"));

        return eb.build();
    }

    static MessageEmbed imageEmbed(String[] tag) throws IOException {
        URL link = new URL("https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1&limit=1&rating=safe&tags=" +
                String.join("+", tag) + "&pid=" + (int) (Math.random() * 101));
        HttpURLConnection con = (HttpURLConnection) link.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        System.out.println("Requisição 'GET' para o URL: " + link);
        System.out.println("Resposta: " + con.getResponseCode());
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String input;
        StringBuilder resposta = new StringBuilder();
        while ((input = br.readLine()) != null) {
            resposta.append(input);
        }
        br.close();

        System.out.println(resposta.toString());
        JSONObject jo = new JSONObject(resposta.toString().replace("[", "").replace("]", ""));

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.MAGENTA);
        eb.setAuthor("Aqui está!", "https://safebooru.org//images/" + jo.getString("directory") + "/" + jo.getString("image"));
        eb.addField("Largura:", Integer.toString(jo.getInt("width")), true);
        eb.addField("Altura:", Integer.toString(jo.getInt("height")), true);
        eb.addField("Tags:", "`" + String.join("` `", jo.getString("tags").split(" ")) + "`", true);
        eb.setImage("https://safebooru.org//images/" + jo.getString("directory") + "/" + jo.getString("image"));

        return eb.build();
    }

    public static void configsEmbed(MessageReceivedEvent message, guildConfig gc) {
        EmbedBuilder eb = new EmbedBuilder();
        Map<String, String> roles = new HashMap<>();
        gc.getCargoslvl().forEach((k, v) -> roles.put(k, message.getGuild().getRoleById(v.toString()).getName()));

        Map<String, String> nc = new HashMap<>();
        gc.getCargoNew().forEach((k, v) -> nc.put(k, message.getGuild().getRoleById(v.toString()).getName()));

        eb.setColor(Color.MAGENTA);
        eb.setAuthor("Eis as configurações deste servidor");
        eb.setDescription("Prefixo: __**" + gc.getPrefix() + "**__");
        eb.addField("Canal de boas-vindas:", gc.getCanalbv() != null ? message.getGuild().getTextChannelById(gc.getCanalbv()).getAsMention() : "Não definido", true);
        eb.addField("Canal de avisos:", gc.getCanalav() != null ? message.getGuild().getTextChannelById(gc.getCanalav()).getAsMention() : "Não definido", true);
        eb.addField("Mensagem de boas-vindas:", gc.getMsgBoasVindas(), true);
        eb.addField("Mensagem de adeus:", gc.getMsgAdeus(), true);
        eb.addField("Notificações de level up:", gc.getLvlNotif() ? "Ativadas" : "Desativadas", true);
        eb.addField("Cargo de punição:", gc.getCargowarn() != null ? message.getGuild().getRoleById(gc.getCargowarn()).getAsMention() : "Não definido", true);
        eb.addField("Recompensas de level:", gc.getCargoslvl().size() != 0 ? roles.toString().replace(",", "\n").replace("{", "").replace("}", "").replace("=", " = ") : "Não definidos", true);
        eb.addField("Cargos para novos membros:", gc.getCargoNew().size() != 0 ? nc.values().toString().replace(",", "\n").replace("{", "").replace("}", "").replace("=", " = ") : "Não definidos", true);

        message.getChannel().sendMessage(eb.build()).queue();
    }

    public static void animeEmbed(MessageReceivedEvent message, String cmd) throws IOException {
        String query = "{\n" +
                "Media(search: \\\"" + message.getMessage().getContentRaw().replace(cmd, "").trim() + "\\\", type: ANIME) {\n" +
                "title {\n" +
                "romaji\n" +
                "english\n" +
                "}\n" +
                "status\n" +
                "startDate {\n" +
                "year\n" +
                "month\n" +
                "day\n" +
                "}\n" +
                "duration\n" +
                "coverImage {\n" +
                "extraLarge\n" +
                "large\n" +
                "medium\n" +
                "color\n" +
                "}\n" +
                "genres\n" +
                "averageScore\n" +
                "popularity\n" +
                "studios(isMain: true) {\n" +
                "edges {\n" +
                "node {\n" +
                "name\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "staff {\n" +
                "edges {\n" +
                "role\n" +
                "node {\n" +
                "name {\n" +
                "first\n" +
                "last\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}" +
                "nextAiringEpisode {\n" +
                "episode\n" +
                "airingAt\n" +
                "}\n" +
                "trailer {\n" +
                "site\n" +
                "}\n" +
                "description\n" +
                "}\n" +
                "}\n";
        query = query.replace("\n", " ");
        JSONObject data = new JSONObject(com.kuuhaku.controller.Anime.getData(query));
        Anime anime = new Anime(data);

        EmbedBuilder eb = new EmbedBuilder();
        if (anime.getGenres().toLowerCase().contains("hentai") && !message.getTextChannel().isNSFW()) {
            message.getChannel().sendMessage("Humm safadinho, não vou buscar dados sobre um Hentai né!").queue();
        }

        eb.setColor(anime.getcColor());
        eb.setAuthor("Bem, aqui está um novo anime para você assistir!\n");
        eb.setTitle(anime.gettRomaji() + (!anime.gettRomaji().equals(anime.gettEnglish()) ? " (" + anime.gettEnglish() + ")" : ""));
        eb.setDescription(Tradutor.translate("en", "pt", anime.getDescription()));
        eb.setImage(anime.getcImage());
        eb.addField("Estúdio:", anime.getStudio(), true);
        eb.addField("Criado por:", anime.getCreator(), true);
        eb.addField("Ano:", anime.getsDate(), true);
        eb.addField("Estado:", anime.getStatus(), true);
        eb.addField("Episódios:", anime.getDuration(), true);
        if (anime.getNaeAiringAt() != null)
            eb.addField("Próximo episódio:", anime.getNaeEpisode() + " -> " + anime.getNaeAiringAt(), true);
        eb.addField("Nota:", Float.toString(anime.getScore() / 10), true);
        eb.addField("Popularidade:", Integer.toString(anime.getPopularity()), true);
        eb.addField("Gêneros:", anime.getGenres(), false);
        eb.setFooter("Descrição traduzida por Yandex | http://translate.yandex.com.", "https://cdn6.aptoide.com/imgs/6/3/5/635bc7fad9a6329e0efbe9502f472dc5_icon.png");

        try {
            message.getChannel().sendMessage(eb.build()).queue();
        } catch (Exception e) {
            message.getChannel().sendMessage("Humm...não achei nenhum anime com esse nome, talvez você tenha escrito algo errado?").queue();
            e.printStackTrace();
        }
    }

    public static void levelEmbed(MessageReceivedEvent message, Member m, String prefix) throws IOException {
        int conqs = 0;
        for (int i = 0; i < m.getBadges().length; i++) {
            if (m.getBadges()[i]) conqs++;
        }
        URL url = new URL(message.getAuthor().getAvatarUrl());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedImage image = ImageIO.read(con.getInputStream());

        EmbedBuilder eb = new EmbedBuilder();

        assert image != null;
        eb.setColor(new Color(ColorThief.getColor(image)[0], ColorThief.getColor(image)[1], ColorThief.getColor(image)[2]));
        eb.setTitle(":pencil: Perfil de " + message.getGuild().getMemberById(m.getId().replace(message.getGuild().getId(), "")).getEffectiveName() + " | " + message.getGuild().getName());
        eb.setThumbnail(message.getGuild().getMemberById(m.getId().replace(message.getGuild().getId(), "")).getUser().getAvatarUrl());
        eb.addField(":tada: Level: " + m.getLevel(), "Xp: " + m.getXp() + " | " + ((int) Math.pow(m.getLevel(), 2) * 100), true);
        eb.addField(":warning: Alertas:", Integer.toString(m.getWarns().length - 1), true);
        if (m.getId().contains("421495229594730496")) {
            eb.addField(":beginner: Conquistas:", "**" + conqs + "**", true);
            eb.setFooter("Digite " + prefix + "conquistas para ver as conquistas que você completou.", "https://discordapp.com/assets/a3fc335f559f462df3e5d6cdbb9178e8.svg");
        }

        message.getChannel().sendMessage(eb.build()).queue();
    }

    public static void myBadgesEmbed(MessageReceivedEvent message, Member m) throws IOException {
        URL url = new URL(message.getAuthor().getAvatarUrl());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedImage image = ImageIO.read(con.getInputStream());

        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(new Color(ColorThief.getColor(image)[0], ColorThief.getColor(image)[1], ColorThief.getColor(image)[2]));
        eb.setTitle(":beginner: Conquistas de " + message.getMember().getEffectiveName());
        eb.setThumbnail(message.getGuild().getMemberById(m.getId().replace(message.getGuild().getId(), "")).getUser().getAvatarUrl());
        eb.addField("", Badges.getBadges(m.getBadges()), false);

        message.getChannel().sendMessage(eb.build()).queue();
    }

    public static void welcomeEmbed(GuildMemberJoinEvent event, String msg, TextChannel canalbv) throws IOException {
        if (!msg.equals("")) {
            URL url = new URL(event.getUser().getAvatarUrl());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedImage image = ImageIO.read(con.getInputStream());

            EmbedBuilder eb = new EmbedBuilder();

            eb.setColor(new Color(ColorThief.getColor(image)[0], ColorThief.getColor(image)[1], ColorThief.getColor(image)[2]));
            eb.setDescription(msg.replace("%user%", event.getUser().getAsMention()).replace("%guild%", event.getGuild().getName()).replace("%user-id%", event.getUser().getId()));
            eb.setThumbnail(event.getUser().getAvatarUrl());
            eb.setFooter("Servidor gerenciado por " + event.getGuild().getOwner().getEffectiveName(), event.getGuild().getOwner().getUser().getAvatarUrl());
            switch ((int) (Math.random() * 5)) {
                case 0:
                    eb.setTitle("Opa, parece que temos um novo membro?");
                    break;
                case 1:
                    eb.setTitle("Mais um membro para nosso lindo servidor!");
                    break;
                case 2:
                    eb.setTitle("Um novo jogador entrou na partida, pressione start 2P!");
                    break;
                case 3:
                    eb.setTitle("Agora podemos iniciar a teamfight, um novo membro veio nos ajudar!");
                    break;
                case 4:
                    eb.setTitle("Bem-vindo ao nosso servidor, puxe uma cadeira e fique à vontade!");
                    break;
            }

            canalbv.sendMessage(eb.build()).queue();
        }
    }

    public static void byeEmbed(GuildMemberLeaveEvent event, String msg, TextChannel canalbv) throws IOException {
        if (!msg.equals("")) {
            URL url = new URL(event.getUser().getAvatarUrl());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedImage image = ImageIO.read(con.getInputStream());

            int rmsg = (int) (Math.random() * 5);

            EmbedBuilder eb = new EmbedBuilder();

            eb.setColor(new Color(ColorThief.getColor(image)[0], ColorThief.getColor(image)[1], ColorThief.getColor(image)[2]));
            eb.setThumbnail(event.getUser().getAvatarUrl());
            eb.setDescription(msg.replace("%user%", event.getUser().getName()).replace("%guild%", event.getGuild().getName()).replace("%user-id%", event.getUser().getId()));
            eb.setFooter("Servidor gerenciado por " + event.getGuild().getOwner().getEffectiveName(), event.getGuild().getOwner().getUser().getAvatarUrl());
            switch (rmsg) {
                case 0:
                    eb.setTitle("Nãããoo...um membro deixou este servidor!");
                    break;
                case 1:
                    eb.setTitle("O quê? Temos um membro a menos neste servidor!");
                    break;
                case 2:
                    eb.setTitle("Alguém saiu do servidor, deve ter acabado a pilha, só pode!");
                    break;
                case 3:
                    eb.setTitle("Bem, alguém não está mais neste servidor, que pena!");
                    break;
                case 4:
                    eb.setTitle("Saíram do servidor bem no meio de uma teamfight, da pra acreditar?");
                    break;
            }

            canalbv.sendMessage(eb.build()).queue();
        }
    }

    public static void makeEmbed(MessageReceivedEvent message, String msg) throws IOException {
        String[] args = msg.split(";");
        URL url = new URL(args[2]);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedImage image = ImageIO.read(con.getInputStream());

        EmbedBuilder eb = new EmbedBuilder();

        try {
            eb.setTitle(args[0]);
            eb.setDescription(args[1]);
            eb.setThumbnail(args[2]);
            eb.setColor(new Color(ColorThief.getColor(image)[0], ColorThief.getColor(image)[1], ColorThief.getColor(image)[2]));

            message.getChannel().sendMessage(eb.build()).queue();
        } catch (Exception e) {
            message.getChannel().sendMessage("Opa, algo deu errado, tenha certeza de ter escrito neste formato:\n" +
                    "**Título;Descrição;Link da foto**").queue();
        }
    }
}
