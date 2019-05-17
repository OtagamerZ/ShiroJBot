package com.kuuhaku.commands;

import com.kuuhaku.model.ReactionsList;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Random;

public class Reactions {
    public static void hug(JDA bot, Message message, User target, TextChannel channel, boolean answer, User mbr) throws IOException {
        URL url = new URL(ReactionsList.hug()[new Random().nextInt(ReactionsList.hug().length)]);
        HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        String msg = "";
        switch (new Random().nextInt(3)) {
            case 0:
                msg = "Awnn, que abraço fofo! :blush:";
                break;
            case 1:
                msg = "Já shippo os dois ein!";
                break;
            case 2:
                msg = "Ai sim ein, tem que ir pra cima mesmo!";
                break;
        }

        if (target == bot.getSelfUser()) {
            if (message.getAuthor().getId().equals("350836145921327115")) {
                switch (new Random().nextInt(3)) {
                    case 0:
                        msg = ("Arigatou, Nii-chan!");
                        break;
                    case 1:
                        msg = ("N-N-Não precisava, Nii-chan!");
                        break;
                    case 2:
                        msg = ("N-N-Nii-chan no baka!");
                        break;
                }
                channel.sendMessage(msg).addFile(con.getInputStream(), "hug.gif").queue();
            } else {
                switch (new Random().nextInt(3)) {
                    case 0:
                        msg = ("Moshi moshi, FBI-sama!");
                        break;
                    case 1:
                        msg = ("B-B-Baka!");
                        break;
                    case 2:
                        msg = ("Paraaa, to ocupada jogando agora!");
                        break;
                }
                channel.sendMessage(msg).addFile(con.getInputStream(), "hug.gif").queue();
            }
        } else {
            if (!answer)
                msg = (mbr.getAsMention() + " abraçou " + target.getAsMention() + " - " + msg);
            else
                msg = (target.getAsMention() + " retribuiu o abraço " + mbr.getAsMention() + " - " + msg);
            channel.sendMessage(msg).addFile(con.getInputStream(), "hug.gif").queue();
        }
    }

    public static void slap(JDA bot, Message message, User target, TextChannel channel, boolean answer, User mbr) throws IOException {
        URL url = new URL(ReactionsList.slap()[new Random().nextInt(ReactionsList.slap().length)]);
        HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        String msg = "";
        switch (new Random().nextInt(3)) {
            case 0:
                msg = "Kono BAKAAAA!!";
                break;
            case 1:
                msg = "Calma, deixa ele(a) respirar!";
                break;
            case 2:
                msg = "Toma essa!";
                break;
        }

        if (target == bot.getSelfUser()) {
            if (message.getAuthor().getId().equals("350836145921327115")) {
                switch (new Random().nextInt(3)) {
                    case 0:
                        msg = ("Nii-chan no BAKA!");
                        break;
                    case 1:
                        msg = ("Ai, vai mesmo bater na sua Nee-chan?");
                        break;
                    case 2:
                        msg = ("EU TO JOGANDO!");
                        break;
                }
                channel.sendMessage(msg).addFile(con.getInputStream(), "slap.gif").queue();
            } else {
                channel.sendMessage(msg).addFile(con.getInputStream(), "slap.gif").queue();
                nope(message, bot.getSelfUser());
            }
        } else {
            if (!answer)
                msg = (mbr.getAsMention() + " deu um tapa em " + target.getAsMention() + " - " + msg);
            else
                msg = (target.getAsMention() + " respondeu o tapa de " + mbr.getAsMention() + " - " + msg);
            channel.sendMessage(msg).addFile(con.getInputStream(), "slap.gif").queue();
        }
    }

    public static void smash(JDA bot, Message message, User target, TextChannel channel, boolean answer, User mbr) throws IOException {
        URL url = new URL(ReactionsList.smash()[new Random().nextInt(ReactionsList.smash().length)]);
        HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        String msg = "";
        switch (new Random().nextInt(3)) {
            case 0:
                msg = "Omae wa mou...SHINDEIRU!!";
                break;
            case 1:
                msg = "Morreee!";
                break;
            case 2:
                msg = "Agora deu!";
                break;
        }

        if (target == bot.getSelfUser()) {
            if (message.getAuthor().getId().equals("350836145921327115")) {
                switch (new Random().nextInt(3)) {
                    case 0:
                        msg = ("O que eu fiz??");
                        break;
                    case 1:
                        msg = ("Vai ter volta!");
                        break;
                    case 2:
                        msg = ("Calma, eu só caí uns elozinhos com a sua conta!");
                        break;
                }
                channel.sendMessage(msg).addFile(con.getInputStream(), "smash.gif").queue();
            } else {
                channel.sendMessage(msg).addFile(con.getInputStream(), "smash.gif").queue();
                nope(message, bot.getSelfUser());
            }
        } else {
            if (!answer)
                msg = (mbr.getAsMention() + " destruiu " + target.getAsMention() + " - " + msg);
            else
                msg = ("Porém, " + target.getAsMention() + " se levantou e atacou " + mbr.getAsMention() + " de volta - " + msg);
            channel.sendMessage(msg).addFile(con.getInputStream(), "smash.gif").queue();
        }
    }

    public static void kiss(JDA bot, Message message, User target, TextChannel channel, boolean answer, User mbr) throws IOException {
        URL url = new URL(ReactionsList.kiss()[new Random().nextInt(ReactionsList.kiss().length)]);
        HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        String msg = "";

        if (target == bot.getSelfUser()) {
            if (message.getAuthor().getId().equals("350836145921327115")) {
                switch (new Random().nextInt(3)) {
                    case 0:
                        msg = ("E-Ei!");
                        break;
                    case 1:
                        msg = ("N-Nii-chan?!");
                        break;
                    case 2:
                        msg = ("P-Pera!");
                        break;
                }
                channel.sendMessage(msg).addFile(con.getInputStream(), "kiss.gif").queue();
            } else {
                channel.sendMessage(msg).addFile(con.getInputStream(), "kiss.gif").queue();
                nope(message, bot.getSelfUser());
            }
        } else {
            if (!answer)
                msg = (mbr.getAsMention() + " beijou " + target.getAsMention() + " - " + msg);
            else
                msg = (target.getAsMention() + " também deu um beijo em " + mbr.getAsMention() + " - " + msg);
            channel.sendMessage(msg).addFile(con.getInputStream(), "kiss.gif").queue();
        }
    }

    public static void stare(JDA bot, User target, TextChannel channel, boolean answer, User mbr) throws IOException {
        URL url = new URL(ReactionsList.stare()[new Random().nextInt(ReactionsList.stare().length)]);
        HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        String msg = "";
        switch (new Random().nextInt(3)) {
            case 0:
                msg = "Shiii~~";
                break;
            case 1:
                msg = "Observa~~";
                break;
            case 2:
                msg = "...";
                break;
        }

        if (target == bot.getSelfUser()) {
            switch (new Random().nextInt(3)) {
                case 0:
                    msg = ("Que foi?");
                    break;
                case 1:
                    msg = ("Hum?");
                    break;
                case 2:
                    msg = ("Nani?");
                    break;
            }
            channel.sendMessage(msg).addFile(con.getInputStream(), "stare.gif").queue();
        } else {
            if (!answer)
                msg = (mbr.getAsMention() + " encarou " + target.getAsMention() + " - " + msg);
            else
                msg = (target.getAsMention() + " também está encarando " + mbr.getAsMention() + " - " + msg);
            channel.sendMessage(msg).addFile(con.getInputStream(), "stare.gif").queue();
        }
    }

    public static void facedesk(Message message) throws IOException {
        User mbr = message.getAuthor();
        URL url = new URL(ReactionsList.facedesk()[new Random().nextInt(ReactionsList.facedesk().length)]);
        HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        String msg = "";
        switch (new Random().nextInt(3)) {
            case 0:
                msg = "Eita, calmaaa!";
                break;
            case 1:
                msg = "Vish, para que vai dar dor de cabeça!";
                break;
            case 2:
                msg = "Calma, a culpa não foi sua!";
                break;
        }
        msg = (mbr.getAsMention() + " bateu a cara - " + msg);
        message.getChannel().sendMessage(msg).addFile(con.getInputStream(), "facedesk.gif").queue();
    }

    public static void nope(Message message) throws IOException {
        User mbr = message.getAuthor();
        URL url = new URL(ReactionsList.nope()[new Random().nextInt(ReactionsList.nope().length)]);
        HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        String msg = "";
        switch (new Random().nextInt(3)) {
            case 0:
                msg = "Não tão rápido!";
                break;
            case 1:
                msg = "Só que não!";
                break;
            case 2:
                msg = "Hoje não!";
                break;
        }
        msg = (mbr.getAsMention() + " esquivou-se - " + msg);
        message.getChannel().sendMessage(msg).addFile(con.getInputStream(), "nope.gif").queue();
    }

    private static void nope(Message message, User author) throws IOException {
        URL url = new URL(ReactionsList.nope()[new Random().nextInt(ReactionsList.nope().length)]);
        HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        String msg = "";
        switch (new Random().nextInt(3)) {
            case 0:
                msg = "Não tão rápido!";
                break;
            case 1:
                msg = "Só que não!";
                break;
            case 2:
                msg = "Hoje não!";
                break;
        }
        msg = (author.getAsMention() + " esquivou-se - " + msg);
        message.getChannel().sendMessage(msg).addFile(con.getInputStream(), "nope.gif").queue();
    }

    public static void run(Message message) throws IOException {
        User mbr = message.getAuthor();
        URL url = new URL(ReactionsList.run()[new Random().nextInt(ReactionsList.run().length)]);
        HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        String msg = "";
        switch (new Random().nextInt(3)) {
            case 0:
                msg = "Corre maluco!";
                break;
            case 1:
                msg = "Sai da frenteee!";
                break;
            case 2:
                msg = "Sebo nas canelas!";
                break;
        }
        msg = (mbr.getAsMention() + " saiu correndo - " + msg);
        message.getChannel().sendMessage(msg).addFile(con.getInputStream(), "run.gif").queue();
    }

    public static void blush(Message message) throws IOException {
        User mbr = message.getAuthor();
        URL url = new URL(ReactionsList.blush()[new Random().nextInt(ReactionsList.blush().length)]);
        HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        String msg = "";
        switch (new Random().nextInt(3)) {
            case 0:
                msg = "B-Baka!";
                break;
            case 1:
                msg = "N-Não é como se eu gostasse disso nem nada do tipo!";
                break;
            case 2:
                msg = "NA-";
                break;
        }
        msg = (mbr.getAsMention() + " está com vergonha - " + msg);
        message.getChannel().sendMessage(msg).addFile(con.getInputStream(), "blush.gif").queue();
    }

    public static void laugh(Message message) throws IOException {
        User mbr = message.getAuthor();
        URL url = new URL(ReactionsList.laugh()[new Random().nextInt(ReactionsList.laugh().length)]);
        HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        String msg = "";
        switch (new Random().nextInt(3)) {
            case 0:
                msg = "Muahahahahaha!";
                break;
            case 1:
                msg = "Kkkkkkkk!";
                break;
            case 2:
                msg = "Rsrsrsrsrs!";
                break;
        }
        msg = (mbr.getAsMention() + " está rindo - " + msg);
        message.getChannel().sendMessage(msg).addFile(con.getInputStream(), "laugh.gif").queue();
    }

    public static void sad(Message message) throws IOException {
        User mbr = message.getAuthor();
        URL url = new URL(ReactionsList.sad()[new Random().nextInt(ReactionsList.sad().length)]);
        HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        String msg = "";
        switch (new Random().nextInt(3)) {
            case 0:
                msg = "Eu...eu...";
                break;
            case 1:
                msg = "Snif...";
                break;
            case 2:
                msg = "Faça isso parar!";
                break;
        }
        msg = (mbr.getAsMention() + " está chorando - " + msg);
        message.getChannel().sendMessage(msg).addFile(con.getInputStream(), "sad.gif").queue();
    }
}
