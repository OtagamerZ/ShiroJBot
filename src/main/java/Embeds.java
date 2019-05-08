import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

class Embeds {
    static MessageEmbed helpEmbed(String prefix) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(Color.MAGENTA);
        eb.addField("Administrativo", "```...```", false);
        eb.addField("Utilit\u00e0rios", "```\n" +
                prefix + "ajuda - Mostra essa mensagem no seu canal privado.\n" +
                prefix + "bug [mensagem] - Envia um bug para meu Nii-chan corrigir.\n" +
                prefix + "ping - Confere se estou online e funcionando direitinho.\n" +
                prefix + "uptime - Descobre a quanto tempo estou acordada.\n" +
                "```", false);

        return eb.build();
    }

    static MessageEmbed imageEmbed(String[] tag, String index) throws MalformedURLException, IOException {
        URL link = new URL("https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1&limit=1&rating=safe&tags=" +
                String.join("+", tag) + "&pid=" + index);
        HttpURLConnection con =(HttpURLConnection) link.openConnection();
        con.setRequestMethod("POST");
    }
}
