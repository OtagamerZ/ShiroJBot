/*
 * This file is part of Shiro J Bot.
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,
either version 3 of the License,
or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not,
see <https://www.gnu.org/licenses/>
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
import com.kuuhaku.command.commands.partner.*;
import com.kuuhaku.command.commands.reactions.*;
import com.kuuhaku.command.commands.reactions.answerable.*;

import java.util.ArrayList;
import java.util.List;

import static com.kuuhaku.command.Category.*;
import static com.kuuhaku.utils.Helper.getString;
import static com.kuuhaku.utils.I18n.PTBR;

public class CommandManager {

	private final List<Command> commands;

	public CommandManager() {
		commands = new ArrayList<Command>() {{
			//DEV
			add(new KillCommand(
					"desligar", new String[]{"kill"}, getString(PTBR, "kill"), DEV));
			add(new RestartCommand(
					"reiniciar", new String[]{"restart"}, getString(PTBR, "restart"), DEV));
			add(new LeaveCommand(
					"sair", new String[]{"leave"}, "<ID do servidor>", getString(PTBR, "leave"), DEV));
			add(new ToxicTagCommand(
					"toxico", new String[]{"toxic"}, "<@usuário>", getString(PTBR, "toxictag"), DEV));
			add(new PartnerTagCommand(
					"parceiro", new String[]{"partner"}, "<@usuário>", getString(PTBR, "partnertag"), DEV));
			add(new VerifiedTagCommand(
					"verificado", new String[]{"verified"}, "<@usuário>", getString(PTBR, "verifiedtag"), DEV));
			add(new RelaysCommand(
					"relays", getString(PTBR, "relaylist"), DEV));
			add(new LogCommand(
					"log", getString(PTBR, "log"), DEV));
			add(new TokenCommand(
					"chave", new String[]{"token"}, "<nome>", getString(PTBR, "token"), DEV));
			add(new BroadcastCommand(
					"transmitir", new String[]{"broadcast", "bc"}, "<tipo> <mensagem>", getString(PTBR, "broadcast"), DEV));
			add(new InviteCommand(
					"convite", new String[]{"invite"}, "<ID do servidor>", getString(PTBR, "invite"), DEV));
			add(new UsageCommand(
					"usos", new String[]{"uses", "usage"}, getString(PTBR, "usage"), DEV));
			add(new SimpleWHMCommand(
					"wh", "<mensagem>", getString(PTBR, "simplewh"), DEV));
			add(new RatingCommand(
					"pedirvoto", new String[]{"requestvote", "howwasi"}, getString(PTBR, "rating"), DEV));

			//SHERIFF
			add(new BlockCommand("bloquear", new String[]{"block"}, "<tipo> <ID> <razão>", getString(PTBR, "block"), SHERIFFS));

			//PARTNER
			add(new JibrilCommand(
					"jibril", getString(PTBR, "jibril"), PARTNER));
			add(new TetCommand(
					"tet", getString(PTBR, "tet"), PARTNER));
			add(new CompileCommand(
					"compilar", new String[]{"compile", "exec"}, "```java/py/js\n<código>\n```", getString(PTBR, "compile"), PARTNER));
			add(new JibrilEmoteListCommand(
					"jemotes", "<nome>", getString(PTBR, "jemotes"), PARTNER));
			add(new PurchaceKGotchiCommand(
					"pkgotchi", new String[]{"buykgotchi", "comprarkgotchi"}, "<escolher/aleatorio> [raça] <nome>", getString(PTBR, "kgotchishop"), PARTNER));
			add(new KGotchiCommand(
					"kgotchi", new String[]{"kg", "kawaig"}, "[alimentar/brincar/treinar/comprar]", getString(PTBR, "kgotchi"), PARTNER));

			//MODERATION
			add(new RemoveAnswerCommand(
					"nãofale", "<id>", getString(PTBR, "dontsay"), Category.MODERACAO));
			add(new SettingsCommand(
					"settings", new String[]{"definicoes", "parametros", "configs"}, "[<parâmetro> <novo valor do parâmetro>]", getString(PTBR, "settings"), Category.MODERACAO));
			add(new AllowCommunityCommand(
					"ouçatodos", getString(PTBR, "allowcommunity"), Category.MODERACAO));
			add(new KickMemberCommand(
					"kick", new String[]{"expulsar", "remover"}, "<membro> <razão>", getString(PTBR, "kick"), Category.MODERACAO));
			add(new BanMemberCommand(
					"ban", new String[]{"banir"}, "<membro> <razão>", getString(PTBR, "ban"), Category.MODERACAO));
			add(new NoLinkCommand(
					"semlink", new String[]{"nolink", "blocklink"}, getString(PTBR, "nolink"), Category.MODERACAO));
			add(new AntispamCommand(
					"semspam", new String[]{"nospam", "antispam"}, "<qtd/soft/hard>", getString(PTBR, "nospam"), Category.MODERACAO));
			add(new AntiraidCommand(
					"semraid", new String[]{"noraid", "antiraid"}, getString(PTBR, "noraid"), Category.MODERACAO));
			add(new MakeLogCommand(
					"logchannel", new String[]{"makelog"}, getString(PTBR, "makelog"), Category.MODERACAO));
			add(new PruneCommand(
					"prune", new String[]{"clean", "limpar"}, "[qtd/all]", getString(PTBR, "prune"), Category.MODERACAO));
			add(new LiteModeCommand(
					"litemode", new String[]{"lite"}, getString(PTBR, "litemode"), Category.MODERACAO));
			add(new AllowImgCommand(
					"allowimg", new String[]{"aimg"}, getString(PTBR, "allowimage"), Category.MODERACAO));
			add(new RoleChooserCommand(
					"botaocargo", new String[]{"rolebutton", "bc", "rb"}, "<reset>/[<ID> <emote> <@cargo>]", getString(PTBR, "rolebutton"), Category.MODERACAO));
			add(new GatekeeperCommand(
					"porteiro", new String[]{"gatekeeper", "gk"}, "<ID> <@cargo>", getString(PTBR, "gatekeeper"), Category.MODERACAO));
			add(new BackupCommand(
					"backup", new String[]{"dados"}, "<salvar/recuperar>", getString(PTBR, "backup"), Category.MODERACAO));
			add(new RegenRulesCommand(
					"rrules", new String[]{"makerules"}, getString(PTBR, "regenrules"), Category.MODERACAO));

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
			add(new LocalEmoteListCommand());
			add(new ShiroEmoteListCommand());
			add(new WalletCommand());

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
			add(new TranslateCommand());
			add(new EightBallCommand());
			add(new ChooseCommand());

			//FUN
			add(new SadButTrueCommand());
			add(new HardDecisionCommand());
			add(new ExpandingBrainCommand());
			add(new PPTCommand());
			add(new ShipCommand());
			add(new MarryCommand());
			add(new StonksCommand());
			add(new StinksCommand());
			add(new DrakeCommand());
			add(new SpiderManCommand());
			add(new PixelCanvasCommand());
			add(new PixelChunkCommand());
			add(new DivorceCommand());
			add(new SlotsCommand());
			add(new HugReaction());
			add(new KissReaction());
			add(new PatReaction());
			add(new StareReaction());
			add(new SlapReaction());
			add(new PunchReaction());
			add(new BiteReaction());
			add(new BlushReaction());
			add(new CryReaction());
			add(new DanceReaction());
			add(new FacedeskReaction());
			add(new LaughReaction());
			add(new NopeReaction());
			add(new RunReaction());

			//MUSICA
			add(new MusicCommand("controle", new String[]{"control", "c"}, getString(PTBR, "control"), Category.MUSICA));
			add(new YoutubeCommand("play", new String[]{"yt", "youtube"}, "<nome>", getString(PTBR, "play"), Category.MUSICA));
			add(new VideoCommand("video", new String[]{"vid"}, "<nome>", getString(PTBR, "video"), Category.MUSICA));

			//EXCEED
			add(new ExceedRankCommand("exceedrank", new String[]{"exrank", "topexceed", "topex"}, getString(PTBR, "exceedrank"), Category.EXCEED));
			add(new ExceedSelectCommand("exceedselect", new String[]{"exselect", "sou"}, getString(PTBR, "exceed"), Category.EXCEED));
		}};
	}

	public List<Command> getCommands() {
		return commands;
	}
}
