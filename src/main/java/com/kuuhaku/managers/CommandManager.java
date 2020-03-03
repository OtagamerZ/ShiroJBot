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

import java.util.ArrayList;
import java.util.List;

import static com.kuuhaku.command.Category.*;
import static com.kuuhaku.utils.I18n.PT;
import static com.kuuhaku.utils.ShiroInfo.getLocale;

public class CommandManager {

	private static final String REQ_MENTION = "req_mention";
	private static final String REQ_MESSAGE = "req_message";
	private static final String REQ_NAME = "req_name";
	private static final String REQ_SERVER_ID = "req_server-id";
	private static final String REQ_MENTION_REASON = "req_mention-reason";
	private final List<Command> commands;

	public CommandManager() {
		commands = new ArrayList<Command>() {{
			//DEV
			add(new KillCommand(
					"desligar", new String[]{"kill"}, getLocale(PT).getString("cmd_kill"), DEV));
			add(new RestartCommand(
					"reiniciar", new String[]{"restart"}, getLocale(PT).getString("cmd_restart"), DEV));
			add(new LeaveCommand(
					"sair", new String[]{"leave"}, getLocale(PT).getString(REQ_SERVER_ID), getLocale(PT).getString("cmd_leave"), DEV));
			add(new ToxicTagCommand(
					"toxico", new String[]{"toxic"}, getLocale(PT).getString(REQ_MENTION), getLocale(PT).getString("cmd_toxic-tag"), DEV));
			add(new PartnerTagCommand(
					"parceiro", new String[]{"partner"}, getLocale(PT).getString(REQ_MENTION), getLocale(PT).getString("cmd_partner-tag"), DEV));
			add(new VerifiedTagCommand(
					"verificado", new String[]{"verified"}, getLocale(PT).getString(REQ_MENTION), getLocale(PT).getString("cmd_verified-tag"), DEV));
			add(new RelaysCommand(
					"relays", getLocale(PT).getString("cmd_relay-list"), DEV));
			add(new LogCommand(
					"log", getLocale(PT).getString("cmd_log"), DEV));
			add(new TokenCommand(
					"chave", new String[]{"token"}, getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_token"), DEV));
			add(new BroadcastCommand(
					"transmitir", new String[]{"broadcast", "bc"}, getLocale(PT).getString("req_type-message"), getLocale(PT).getString("cmd_broadcast"), DEV));
			add(new InviteCommand(
					"convite", new String[]{"invite"}, getLocale(PT).getString(REQ_SERVER_ID), getLocale(PT).getString("cmd_invite"), DEV));
			add(new UsageCommand(
					"usos", new String[]{"uses", "usage"}, getLocale(PT).getString("cmd_usage"), DEV));
			add(new SimpleWHMCommand(
					"wh", getLocale(PT).getString(REQ_MESSAGE), getLocale(PT).getString("cmd_simple-wh"), DEV));
			add(new RatingCommand(
					"pedirvoto", new String[]{"requestvote", "howwasi"}, getLocale(PT).getString("cmd_rating"), DEV));

			//SHERIFF
			add(new BlockCommand("bloquear", new String[]{"block"}, getLocale(PT).getString("req_type-id-reason"), getLocale(PT).getString("cmd_block"), SHERIFFS));

			//PARTNER
			add(new JibrilCommand(
					"jibril", getLocale(PT).getString("cmd_jibril"), PARTNER));
			add(new TetCommand(
					"tet", getLocale(PT).getString("cmd_tet"), PARTNER));
			add(new CompileCommand(
					"compilar", new String[]{"compile", "exec"}, getLocale(PT).getString("req_code"), getLocale(PT).getString("cmd_compile"), PARTNER));
			add(new JibrilEmoteListCommand(
					"jemotes", getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_j-emotes"), PARTNER));
			add(new PurchaceKGotchiCommand(
					"pkgotchi", new String[]{"buykgotchi", "comprarkgotchi"}, getLocale(PT).getString("req_kgotchi"), getLocale(PT).getString("cmd_kgotchi-shop"), PARTNER));
			add(new KGotchiCommand(
					"kgotchi", new String[]{"kg", "kawaig"}, getLocale(PT).getString("req_action"), getLocale(PT).getString("cmd_kgotchi"), PARTNER));
			add(new RelayCommand(
					"relay", new String[]{"relinfo", "relcon"}, getLocale(PT).getString("cmd_relay"), PARTNER));
			add(new TagsCommand(
					"tags", new String[]{"emblemas", "insignias"}, getLocale(PT).getString("cmd_tags"), PARTNER));
			add(new MyTagsCommand(
					"eu", new String[]{"meusemblemas", "mytags"}, getLocale(PT).getString("cmd_my-tags"), PARTNER));

			//MODERATION
			add(new RemoveAnswerCommand(
					"nãofale", getLocale(PT).getString("req_id"), getLocale(PT).getString("cmd_dont-say"), MODERACAO));
			add(new SettingsCommand(
					"settings", new String[]{"definicoes", "parametros", "configs"}, getLocale(PT).getString("req_parameter"), getLocale(PT).getString("cmd_settings"), MODERACAO));
			add(new AllowCommunityCommand(
					"ouçatodos", getLocale(PT).getString("cmd_allow-community"), MODERACAO));
			add(new KickMemberCommand(
					"kick", new String[]{"expulsar", "remover"}, getLocale(PT).getString(REQ_MENTION_REASON), getLocale(PT).getString("cmd_kick"), MODERACAO));
			add(new BanMemberCommand(
					"ban", new String[]{"banir"}, getLocale(PT).getString(REQ_MENTION_REASON), getLocale(PT).getString("cmd_ban"), MODERACAO));
			add(new NoLinkCommand(
					"semlink", new String[]{"nolink", "blocklink"}, getLocale(PT).getString("cmd_no-link"), MODERACAO));
			add(new AntispamCommand(
					"semspam", new String[]{"nospam", "antispam"}, getLocale(PT).getString("req_spam-type"), getLocale(PT).getString("cmd_no-spam"), MODERACAO));
			add(new AntiraidCommand(
					"semraid", new String[]{"noraid", "antiraid"}, getLocale(PT).getString("cmd_no-raid"), MODERACAO));
			add(new MakeLogCommand(
					"logchannel", new String[]{"makelog"}, getLocale(PT).getString("cmd_make-log"), MODERACAO));
			add(new PruneCommand(
					"prune", new String[]{"clean", "limpar"}, getLocale(PT).getString("req_qtd-all"), getLocale(PT).getString("cmd_prune"), MODERACAO));
			add(new LiteModeCommand(
					"litemode", new String[]{"lite"}, getLocale(PT).getString("cmd_lite-mode"), MODERACAO));
			add(new AllowImgCommand(
					"allowimg", new String[]{"aimg"}, getLocale(PT).getString("cmd_allow-images"), MODERACAO));
			add(new RoleChooserCommand(
					"botaocargo", new String[]{"rolebutton", "bc", "rb"}, getLocale(PT).getString("req_role-button"), getLocale(PT).getString("cmd_role-button"), MODERACAO));
			add(new GatekeeperCommand(
					"porteiro", new String[]{"gatekeeper", "gk"}, getLocale(PT).getString("req_id-role"), getLocale(PT).getString("cmd_gatekeeper"), MODERACAO));
			add(new BackupCommand(
					"backup", new String[]{"dados"}, getLocale(PT).getString("req_save-restore"), getLocale(PT).getString("cmd_backup"), MODERACAO));
			add(new RegenRulesCommand(
					"rrules", new String[]{"makerules"}, getLocale(PT).getString("cmd_regen-rules"), MODERACAO));
			add(new PermissionCommand(
					"permissões", new String[]{"perms", "permisions"}, getLocale(PT).getString("cmd_permission"), MODERACAO));
			add(new AddColorRoleCommand(
					"cargocor", new String[]{"rolecolor"}, getLocale(PT).getString("req_name-color"), getLocale(PT).getString("cmd_add-color-role"), MODERACAO));

			//INFORMATION
			add(new ComandosCommand(
					"comandos", new String[]{"cmds", "cmd", "comando", "ajuda", "help"}, getLocale(PT).getString("req_command"), getLocale(PT).getString("cmd_help"), INFO));
			add(new ProfileCommand(
					"perfil", new String[]{"xp", "profile", "pf"}, getLocale(PT).getString("cmd_profile"), INFO));
			add(new ReportBugCommand(
					"bug", new String[]{"sendbug", "feedback"}, getLocale(PT).getString(REQ_MESSAGE), getLocale(PT).getString("cmd_bug"), INFO));
			add(new ReportUserCommand(
					"report", new String[]{"reportar"}, getLocale(PT).getString("req_user-reason"), getLocale(PT).getString("cmd_report"), INFO));
			add(new BotInfoCommand(
					"info", new String[]{"botinfo", "bot"}, getLocale(PT).getString("cmd_info"), INFO));
			add(new URankCommand(
					"rank", new String[]{"ranking", "top10"}, getLocale(PT).getString("req_global"), getLocale(PT).getString("cmd_rank"), INFO));
			add(new IDCommand(
					"id", getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_id"), INFO));
			add(new ColorTesterCommand(
					"quecor", new String[]{"tcolor", "testcolor"}, getLocale(PT).getString("req_color"), getLocale(PT).getString("cmd_color"), INFO));
			add(new LocalEmoteListCommand(
					"emotes", getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_emotes"), INFO));
			add(new ShiroEmoteListCommand(
					"semotes", getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_s-emotes"), INFO));
			add(new WalletCommand(
					"carteira", new String[]{"banco", "bank", "money", "wallet", "atm"}, getLocale(PT).getString("cmd_wallet"), INFO));
			add(new PingCommand(
					"ping", getLocale(PT).getString("cmd_ping"), INFO));
			add(new UptimeCommand(
					"uptime", getLocale(PT).getString("cmd_uptime"), INFO));
			add(new ListScoreCommand(
					"notas", new String[]{"scores"}, getLocale(PT).getString("cmd_score"), INFO));

			//MISC
			add(new BackgroundCommand(
					"background", new String[]{"fundo", "bg"}, getLocale(PT).getString("req_link"), getLocale(PT).getString("cmd_background"), MISC));
			add(new BiographyCommand(
					"bio", new String[]{"story", "desc"}, getLocale(PT).getString(REQ_MESSAGE), getLocale(PT).getString("cmd_biography"), MISC));
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
			add(new ColorRoleCommand(
					"cor", new String[]{"color"}, getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_color-role"), MISC));

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
					"controle", new String[]{"control", "c"}, getLocale(PT).getString("cmd_control"), MUSICA));
			add(new YoutubeCommand(
					"play", new String[]{"yt", "youtube"}, getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_play"), MUSICA));
			add(new VideoCommand(
					"video", new String[]{"vid"}, getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_video"), MUSICA));

			//EXCEED
			add(new ExceedRankCommand(
					"exceedrank", new String[]{"exrank", "topexceed", "topex"}, getLocale(PT).getString("cmd_exceed-rank"), EXCEED));
			add(new ExceedSelectCommand(
					"exceedselect", new String[]{"exselect", "sou"}, getLocale(PT).getString("cmd_exceed"), EXCEED));
		}};
	}

	public List<Command> getCommands() {
		return commands;
	}
}
