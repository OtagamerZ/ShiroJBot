package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class AsciiCommand extends Command {
	
	public AsciiCommand() {
		super("ascii", "<texto>", "Converte o texto fornecido em ascii.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		
		if(args.length == 0) { channel.sendMessage(":x: | Você necessita de fornecer um texto para converter em ascii..").queue(); return; }
		
        String query = "";
        for(String arg : args) {
            query += arg + "+ ";
            query = query.substring(0, query.length()-1);
        }
        
        OkHttpClient caller = new OkHttpClient();
        Request request = new Request.Builder().url("http://artii.herokuapp.com/make?text=" + query).build();
        try {
            Response response = caller.newCall(request).execute();
            channel.sendMessage(":warning: | O texto ascii pode parecer deformado devido ao tamanho do seu ecrã!\n```\n" + response.body().string() + "\n```").queue();
        } catch (IOException e) {
        	channel.sendMessage(":x: | Ocorreu um erro ao contactar a API.").queue();
        }
    }

		
}