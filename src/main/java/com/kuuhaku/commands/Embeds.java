package com.kuuhaku.commands;

import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.json.JSONObject;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.format.DateTimeFormatter;

public class Embeds {
    public static MessageEmbed bugReport(MessageReceivedEvent message, String prefix) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setAuthor("Novo bug enviado de " + message.getGuild().getName());
        eb.addField("Enviado de:", message.getAuthor().getAsMention(), true);
        eb.addField("Data:", message.getMessage().getCreationTime().minusHours(3).format(DateTimeFormatter.ofPattern("dd/MM/yyyy (HH:mm)")), true);
        eb.addField("Relatório do bug:", String.join(" ", message.getMessage().getContentRaw().split(prefix + "bug ")), false);

        return eb.build();
    }

    static MessageEmbed helpEmbed(String prefix) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(Color.MAGENTA);
        eb.addField("Administrativo", "```" +
                prefix + "definir prefixo [prefixo] - Define o prefixo para o servidor.\n\n" +
                prefix + "definir canalbv [canal] - Define o canal de boas-vindas para o servidor.\n\n" +
                prefix + "definir canalav [canal] - Define o canal de avisos para o servidor.\n\n" +
                prefix + "definir msgbv [\"mensagem\"] - Define a mensagem de boas-vindas para o servidor.\n\n" +
                prefix + "definir msgadeus [\"mensagem\"] - Define a mensagem de adeus para o servidor.\n\n" +
                prefix + "configs - Mostra as configurações do servidor.\n" +
                "```", false);
        eb.addField("Utilitários", "```\n\n" +
                prefix + "ajuda - Mostra essa mensagem no seu canal privado.\n\n" +
                prefix + "bug [mensagem] - Envia um bug para meu Nii-chan corrigir.\n\n" +
                prefix + "ping - Confere se estou online e funcionando direitinho.\n\n" +
                prefix + "uptime - Descobre a quanto tempo estou acordada.\n\n" +
                prefix + "imagem [tags] [página] - Busca uma imagem no Safebooru, as tags não podem conter espaços (substitua-os por _).\n" +
                "```", false);

        return eb.build();
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
        eb.addField("Tags:", "`" + String.join("` `", jo.getString("tags").split(" ")) + "`", false);
        eb.setImage("https://safebooru.org//images/" + jo.getString("directory") + "/" + jo.getString("image"));

        return eb.build();
    }

    public static MessageEmbed configsEmbed(guildConfig gc, MessageReceivedEvent message) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(Color.MAGENTA);
        eb.setAuthor("Eis as configurações deste servidor");
        eb.setDescription("Prefixo: __**" + gc.getPrefix() + "**__");
        eb.addField("Canal de boas-vindas:", gc.getCanalbv() != null ? message.getGuild().getTextChannelById(gc.getCanalbv()).getAsMention() : "não definido", true);
        eb.addField("Canal de avisos:", gc.getCanalav() != null ? message.getGuild().getTextChannelById(gc.getCanalav()).getAsMention() : "não definido", true);
        eb.addField("Mensagem de boas-vindas:", gc.getMsgBoasVindas(null), false);
        eb.addField("Mensagem de adeus:", gc.getMsgAdeus(null), false);

        return eb.build();
    }

    public static MessageEmbed animeEmbed(String name) throws IOException {
        String query = "{\n" +
                "  Media(search: " + name + ", type: ANIME) {\n" +
                "    title {\n" +
                "      romaji\n" +
                "      english\n" +
                "    }\n" +
                "    status\n" +
                "    startDate {\n" +
                "      year\n" +
                "      month\n" +
                "      day\n" +
                "    }\n" +
                "    endDate {\n" +
                "      year\n" +
                "      month\n" +
                "      day\n" +
                "    }\n" +
                "    duration\n" +
                "    coverImage {\n" +
                "      large\n" +
                "      color\n" +
                "    }\n" +
                "    genres\n" +
                "    averageScore\n" +
                "    popularity\n" +
                "    staff {\n" +
                "      edges {\n" +
                "        role\n" +
                "        node {\n" +
                "          name {\n" +
                "            first\n" +
                "            last\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    studios(isMain: true) {\n" +
                "      edges {\n" +
                "        node {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    nextAiringEpisode {\n" +
                "      episode\n" +
                "      airingAt\n" +
                "    }\n" +
                "    trailer {\n" +
                "      site\n" +
                "    }\n" +
                "    description\n" +
                "  }\n" +
                "}\n";
        query = query.replace("\n", " ").replace("  ", " ");
        System.out.println(Anime.getData(query));
        JSONObject data = new JSONObject(Anime.getData(query));

        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(data.getString("coverImage"))
    }
}
