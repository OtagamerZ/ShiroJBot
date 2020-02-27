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
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;

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
					"sair", new String[]{"leave"}, getString(PTBR, "req_server_id"), getString(PTBR, "leave"), DEV));
			add(new ToxicTagCommand(
					"toxico", new String[]{"toxic"}, getString(PTBR, "req_mention"), getString(PTBR, "toxic_tag"), DEV));
			add(new PartnerTagCommand(
					"parceiro", new String[]{"partner"}, getString(PTBR, "req_mention"), getString(PTBR, "partner_tag"), DEV));
			add(new VerifiedTagCommand(
					"verificado", new String[]{"verified"}, getString(PTBR, "req_mention"), getString(PTBR, "verified_tag"), DEV));
			add(new RelaysCommand(
					"relays", getString(PTBR, "relaylist"), DEV));
			add(new LogCommand(
					"log", getString(PTBR, "log"), DEV));
			add(new TokenCommand(
					"chave", new String[]{"token"}, getString(PTBR, "req_name"), getString(PTBR, "token"), DEV));
			add(new BroadcastCommand(
					"transmitir", new String[]{"broadcast", "bc"}, getString(PTBR, "req_type_message"), getString(PTBR, "broadcast"), DEV));
			add(new InviteCommand(
					"convite", new String[]{"invite"}, getString(PTBR, "req_server_id"), getString(PTBR, "invite"), DEV));
			add(new UsageCommand(
					"usos", new String[]{"uses", "usage"}, getString(PTBR, "usage"), DEV));
			add(new SimpleWHMCommand(
					"wh", getString(PTBR, "req_message"), getString(PTBR, "simple_wh"), DEV));
			add(new RatingCommand(
					"pedirvoto", new String[]{"requestvote", "howwasi"}, getString(PTBR, "rating"), DEV));

			//SHERIFF
			add(new BlockCommand("bloquear", new String[]{"block"}, getString(PTBR, "req_type_id_reason"), getString(PTBR, "block"), SHERIFFS));

			//PARTNER
			add(new JibrilCommand(
					"jibril", getString(PTBR, "jibril"), PARTNER));
			add(new TetCommand(
					"tet", getString(PTBR, "tet"), PARTNER));
			add(new CompileCommand(
					"compilar", new String[]{"compile", "exec"}, getString(PTBR, "req_code"), getString(PTBR, "compile"), PARTNER));
			add(new JibrilEmoteListCommand(
					"jemotes", getString(PTBR, "req_name"), getString(PTBR, "jemotes"), PARTNER));
			add(new PurchaceKGotchiCommand(
					"pkgotchi", new String[]{"buykgotchi", "comprarkgotchi"}, getString(PTBR, "req_kgotchi"), getString(PTBR, "kgotchi_shop"), PARTNER));
			add(new KGotchiCommand(
					"kgotchi", new String[]{"kg", "kawaig"}, getString(PTBR, "req_action"), getString(PTBR, "kgotchi"), PARTNER));
			add(new RelayCommand(
					"relay", new String[]{"relinfo", "relcon"}, getString(PTBR, "relay"), PARTNER));
			add(new TagsCommand(
					"tags", new String[]{"emblemas", "insignias"}, getString(PTBR, "tags"), PARTNER));
			add(new MyTagsCommand(
					"eu", new String[]{"meusemblemas", "mytags"}, getString(PTBR, "my_tags"), PARTNER));

			//MODERATION
			add(new RemoveAnswerCommand(
					"nãofale", "<id>", getString(PTBR, "dont_say"), MODERACAO));
			add(new SettingsCommand(
					"settings", new String[]{"definicoes", "parametros", "configs"}, getString(PTBR, "req_parameter"), getString(PTBR, "settings"), MODERACAO));
			add(new AllowCommunityCommand(
					"ouçatodos", getString(PTBR, "allow_community"), MODERACAO));
			add(new KickMemberCommand(
					"kick", new String[]{"expulsar", "remover"}, getString(PTBR, "req_mention_reason"), getString(PTBR, "kick"), MODERACAO));
			add(new BanMemberCommand(
					"ban", new String[]{"banir"}, getString(PTBR, "req_mention_reason"), getString(PTBR, "ban"), MODERACAO));
			add(new NoLinkCommand(
					"semlink", new String[]{"nolink", "blocklink"}, getString(PTBR, "no_link"), MODERACAO));
			add(new AntispamCommand(
					"semspam", new String[]{"nospam", "antispam"}, getString(PTBR, "req_spam_type"), getString(PTBR, "no_spam"), MODERACAO));
			add(new AntiraidCommand(
					"semraid", new String[]{"noraid", "antiraid"}, getString(PTBR, "no_raid"), MODERACAO));
			add(new MakeLogCommand(
					"logchannel", new String[]{"makelog"}, getString(PTBR, "make_log"), MODERACAO));
			add(new PruneCommand(
					"prune", new String[]{"clean", "limpar"}, getString(PTBR, "req_qtd_all"), getString(PTBR, "prune"), MODERACAO));
			add(new LiteModeCommand(
					"litemode", new String[]{"lite"}, getString(PTBR, "lite_mode"), MODERACAO));
			add(new AllowImgCommand(
					"allowimg", new String[]{"aimg"}, getString(PTBR, "allow_image"), MODERACAO));
			add(new RoleChooserCommand(
					"botaocargo", new String[]{"rolebutton", "bc", "rb"}, getString(PTBR, "req_role_button"), getString(PTBR, "role_button"), MODERACAO));
			add(new GatekeeperCommand(
					"porteiro", new String[]{"gatekeeper", "gk"}, getString(PTBR, "req_id_role"), getString(PTBR, "gatekeeper"), MODERACAO));
			add(new BackupCommand(
					"backup", new String[]{"dados"}, getString(PTBR, "req_save_restore"), getString(PTBR, "backup"), MODERACAO));
			add(new RegenRulesCommand(
					"rrules", new String[]{"makerules"}, getString(PTBR, "regen_rules"), MODERACAO));
			add(new PermissionCommand(
					"permissões", new String[]{"perms", "permisions"}, getString(PTBR, "permission"), MODERACAO));

			//INFORMATION
			add(new ComandosCommand(
					"comandos", new String[]{"cmds", "cmd", "comando", "ajuda", "help"}, getString(PTBR, "req_command"), getString(PTBR, "help"), INFO));
			add(new ProfileCommand(
					"perfil", new String[]{"xp", "profile", "pf"}, getString(PTBR, "profile"), INFO));
			add(new ReportBugCommand(
					"bug", new String[]{"sendbug", "feedback"}, getString(PTBR, "req_message"), getString(PTBR, "bug"), INFO));
			add(new ReportUserCommand(
					"report", new String[]{"reportar"}, getString(PTBR, "req_user_reason"), getString(PTBR, "report"), INFO));
			add(new BotInfoCommand(
					"info", new String[]{"botinfo", "bot"}, getString(PTBR, "info"), INFO));
			add(new URankCommand(
					"rank", new String[]{"ranking", "top10"}, getString(PTBR, "req_global"), getString(PTBR, "rank"), INFO));
			add(new IDCommand(
					"id", getString(PTBR, "req_name"), getString(PTBR, "id"), INFO));
			add(new ColorTesterCommand(
					"cor", new String[]{"color"}, getString(PTBR, "req_color"), getString(PTBR, "color"), INFO));
			add(new LocalEmoteListCommand(
					"emotes", getString(PTBR, "req_name"), getString(PTBR, "emotes"), INFO));
			add(new ShiroEmoteListCommand(
					"semotes", getString(PTBR, "req_name"), getString(PTBR, "s_emotes"), INFO));
			add(new WalletCommand(
					"carteira", new String[]{"banco", "bank", "money", "wallet", "atm"}, getString(PTBR, "wallet"), INFO));
			add(new PingCommand(
					"ping", Helper.getString(I18n.PTBR, "ping"), INFO));
			add(new UptimeCommand(
					"uptime", Helper.getString(I18n.PTBR, "uptime"), INFO));
			add(new ListScoreCommand(
					"notas", new String[]{"scores"}, Helper.getString(I18n.PTBR, "score"), INFO));

			//MISC
			add(new BackgroundCommand(
					"background", new String[]{"fundo", "bg"}, getString(PTBR, "req_link"), getString(PTBR, "background"), MISC));
			add(new BiographyCommand(
					"bio", new String[]{"story", "desc"}, getString(PTBR, "req_message"), getString(PTBR, "biography"), MISC));
			add(new AsciiCommand());
			add(new AvatarCommand());
			add(new FlipCoinCommand());
			add(new ReverseCommand());
			add(new SayCommand());
			add(new CustomAnswerCommand());
			add(new AnimeCommand());
			add(new ImageCommand());
			add(new ValidateGIFCommand());
			add(new EmbedCommand());
			add(new PollCommand());
			add(new TheAnswerCommand());
			add(new BinaryCommand());
			add(new LinkTesterCommand());
			add(new VoteCommand());
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
			add(new MusicCommand(
					"controle", new String[]{"control", "c"}, getString(PTBR, "control"), MUSICA));
			add(new YoutubeCommand(
					"play", new String[]{"yt", "youtube"}, getString(PTBR, "req_name"), getString(PTBR, "play"), MUSICA));
			add(new VideoCommand(
					"video", new String[]{"vid"}, getString(PTBR, "req_name"), getString(PTBR, "video"), MUSICA));

			//EXCEED
			add(new ExceedRankCommand(
					"exceedrank", new String[]{"exrank", "topexceed", "topex"}, getString(PTBR, "exceed_rank"), EXCEED));
			add(new ExceedSelectCommand(
					"exceedselect", new String[]{"exselect", "sou"}, getString(PTBR, "exceed"), EXCEED));
		}};
	}

	public List<Command> getCommands() {
		return commands;
	}
}
