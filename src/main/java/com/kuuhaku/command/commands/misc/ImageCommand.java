package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class ImageCommand extends Command {

    public ImageCommand() {
        super("image", new String[]{"imagem", "img"}, "Busca uma imagem na internet.", Category.INFO);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (args.length < 1) {
            channel.sendMessage(":x: | Você precisa de indicar uma ou ou mais tags separadas por `;`.").queue();
            return;
        }

        String[] tag = String.join(" ", args).split(";");

        channel.sendMessage("<a:Loading:598500653215645697> Buscando imagem...").queue(m -> {
            try {
                URL link = new URL("https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1&limit=1&rating=safe&tags=" +
                        String.join("+", tag).replace(" ", "_"));
                HttpURLConnection con = (HttpURLConnection) link.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                Helper.log(this.getClass(), LogLevel.DEBUG, "Requisição 'GET' para o URL: " + link);
                Helper.log(this.getClass(), LogLevel.DEBUG, "Resposta: " + con.getResponseCode());
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

                String input;
                StringBuilder resposta = new StringBuilder();
                while ((input = br.readLine()) != null) {
                    resposta.append(input);
                }
                br.close();

                Helper.log(this.getClass(), LogLevel.DEBUG, resposta.toString());
                JSONObject jo = new JSONObject(resposta.toString().replace("[", "").replace("]", ""));
                String url = "https://safebooru.org//images/" + jo.getString("directory") + "/" + jo.getString("image");

                if (Arrays.asList(jo.getString("tags").split(" ")).contains("hentai")) {
                    m.editMessage("Humm safadinho, eu não posso postar sobre Hentais neste canal!").queue();
                    return;
                }

                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Helper.colorThief(url));
                eb.setAuthor("Aqui está!", "https://safebooru.org//images/" + jo.getString("directory") + "/" + jo.getString("image"));
                eb.addField("Largura:", Integer.toString(jo.getInt("width")), true);
                eb.addField("Altura:", Integer.toString(jo.getInt("height")), true);
                eb.addField("Tags:", "`" + String.join("` `", jo.getString("tags").split(" ")) + "`", true);
                eb.setImage(url);

                m.delete().queue();
                channel.sendMessage(eb.build()).queue();
            } catch (IOException | JSONException e) {
                m.editMessage(":x: | Humm...não achei nenhuma imagem com essas tags, talvez você tenha escrito algo errado?").queue();
                Helper.log(this.getClass(), LogLevel.ERROR, e.toString());
            }
        });
    }
}
