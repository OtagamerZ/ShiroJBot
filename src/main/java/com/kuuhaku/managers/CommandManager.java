/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.managers;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.command.commands.dev.*;
import com.kuuhaku.command.commands.exceed.ExceedRankCommand;
import com.kuuhaku.command.commands.exceed.ExceedSelectCommand;
import com.kuuhaku.command.commands.fun.*;
import com.kuuhaku.command.commands.information.*;
import com.kuuhaku.command.commands.misc.*;
import com.kuuhaku.command.commands.moderation.*;
import com.kuuhaku.command.commands.music.MusicCommand;
import com.kuuhaku.command.commands.music.VideoCommand;
import com.kuuhaku.command.commands.music.YoutubeCommand;
import com.kuuhaku.command.commands.partner.CompileCommand;
import com.kuuhaku.command.commands.partner.JibrilCommand;
import com.kuuhaku.command.commands.partner.JibrilEmoteListCommand;
import com.kuuhaku.command.commands.partner.TetCommand;
import com.kuuhaku.command.commands.reactions.*;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {

	private final List<Command> commands;

	public CommandManager() {
		commands = new ArrayList<Command>() {{
			//DEV
			add(new KillCommand("desligar", new String[]{"kill"}, "Desliga a Shiro.", Category.DEVS));
			add(new RestartCommand("reiniciar", new String[]{"restart"}, "Reinicia a Shiro.", Category.DEVS));
			add(new LeaveCommand("sair", new String[]{"leave"}, "<ID do servidor>", "Sai do servidor com o ID informado.", Category.DEVS));
			add(new ToxicTagCommand("toxico", new String[]{"toxic"}, "<@usuário>", "Define um usuário como tóxico ou não.", Category.DEVS));
			add(new PartnerTagCommand("parceiro", new String[]{"partner"}, "<@usuário>", "Define um usuário como parceiro ou não.", Category.DEVS));
			add(new VerifiedTagCommand("verificado", new String[]{"verified"}, "<@usuário>", "Define um usuário como verificado ou não.", Category.DEVS));
			add(new RelaysCommand("relays", "Mostra os IDs dos clientes do relay.", Category.DEVS));
			add(new LogCommand("log", "Recupera o arquivo de logs", Category.DEVS));
			add(new TokenCommand("chave", new String[]{"token"}, "<nome>", "Gera um token aleatório de 64 caractéres.", Category.DEVS));
			add(new BroadcastCommand("transmitir", new String[]{"broadcast", "bc"}, "<tipo> <mensagem>", "Envia um aviso a todos os donos de servidor que possuem a Shiro, ou a todos o parceiros.", Category.DEVS));
			add(new InviteCommand("convite", new String[]{"invite"}, "<ID do servidor>", "Obtém um convite de uso único do servidor informado.", Category.DEVS));
			add(new UsageCommand("usos", new String[]{"uses", "usage"}, "Vê a quantidade de comandos usados por servidor.", Category.DEVS));

			//SHERIFF
			add(new BlockCommand("bloquear", new String[]{"block"}, "<tipo> <ID> <razão>", "Bloqueia alguém de usar o chat global.", Category.SHERIFFS));

			//PARTNER
			add(new JibrilCommand("jibril", "Chama a Jibril para usar o chat global em seu servidor.", Category.PARTNER));
			add(new CompileCommand("compilar", new String[]{"compile"}, "```java\n<código>\n```", "Executa um código Java.", Category.PARTNER));
			add(new TetCommand("tet", "Chama o Tet para usar o módulo de RPG em seu servidor.", Category.PARTNER));
			add(new JibrilEmoteListCommand("jemotes", "<nome>", "Mostra a lista de emotes disponíveis para uso através da Jibril.", Category.PARTNER));

			//MODERATION
			add(new RemoveAnswerCommand());
			add(new SettingsCommand());
			add(new AllowCommunityCommand());
			add(new KickMemberCommand());
			add(new BanMemberCommand());
			add(new NoLinkCommand());
			add(new AntispamCommand());
			add(new AntiraidCommand());
			add(new MakeLogCommand());
			add(new PruneCommand());
			add(new LiteModeCommand());
			add(new AllowImgCommand());
			add(new RoleChooserCommand());
			add(new GatekeeperCommand());
			add(new BackupCommand());

			//INFORMATION
			add(new ComandosCommand());
			add(new ProfileCommand());
			add(new ReportBugCommand());
			add(new ReportUserCommand());
			add(new BackgroundCommand());
			add(new BiographyCommand());
			add(new RelayCommand());
			add(new TagsCommand());
			add(new MyTagsCommand());
			add(new BotInfoCommand());
			add(new URankCommand());
			add(new IDCommand());
			add(new ColorTesterCommand());
			add(new RegenRulesCommand());
			add(new LocalEmoteListCommand());
			add(new ShiroEmoteListCommand());

			//MISC
			add(new AsciiCommand());
			add(new AvatarCommand());
			add(new FlipCoinCommand());
			add(new PingCommand());
			add(new ReverseCommand());
			add(new SayCommand());
			add(new UptimeCommand());
			add(new CustomAnswerCommand());
			add(new AnimeCommand());
			add(new ImageCommand());
			add(new ValidateGIFCommand());
			add(new EmbedCommand());
			add(new PollCommand());
			add(new TheAnswerCommand());
			add(new BinaryCommand());
			add(new PermissionCommand());
			add(new LinkTesterCommand());
			add(new VoteCommand());
			add(new ListScoreCommand());

			//FUN
			add(new SadButTrueCommand());
			add(new HardDecisionCommand());
			add(new ExpandingBrainCommand());
			add(new PPTCommand());
			add(new ShipCommand());
			add(new MarryCommand());
			add(new StonksCommand());
			add(new PixelCanvasCommand());
			add(new PixelChunkCommand());
			add(new DivorceCommand());
			//REAÇÕES
			//RECIPROCOS
			add(new HugReaction(false));
			add(new KissReaction(false));
			add(new PatReaction(false));
			add(new StareReaction(false));
			add(new SlapReaction(false));
			add(new PunchReaction(false));
			add(new BiteReaction(false));
			//NÃO RECIPROCOS
			add(new BlushReaction());
			add(new CryReaction());
			add(new DanceReaction());
			add(new FacedeskReaction());
			add(new LaughReaction());
			add(new NopeReaction());
			add(new RunReaction());

			//MUSICA
			add(new MusicCommand());
			add(new YoutubeCommand());
			add(new VideoCommand());

			//EXCEED
			add(new ExceedRankCommand());
			add(new ExceedSelectCommand());
		}};
	}

	public List<Command> getCommands() {
		return commands;
	}
}
