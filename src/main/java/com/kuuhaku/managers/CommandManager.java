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
import com.kuuhaku.command.commands.music.ControlCommand;
import com.kuuhaku.command.commands.music.YoutubeCommand;
import com.kuuhaku.command.commands.partner.*;
import com.kuuhaku.command.commands.reactions.*;
import com.kuuhaku.command.commands.reactions.answerable.*;
import com.kuuhaku.command.commands.support.*;

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
	private static final String REQ_TEXT = "req_text";
	private static final String REQ_LINK = "req_link";
	private static final String REQ_QUESTION = "req_question";
	private static final String REQ_TWO_OPTIONS = "req_two-options";
	private final List<Command> commands;

	public CommandManager() {
		commands = new ArrayList<Command>() {{
			//DEV
			add(new KillCommand(
					"desligar", new String[]{"kill"}, getLocale(PT).getString("cmd_kill"), DEV, false
			));
			add(new RestartCommand(
					"reiniciar", new String[]{"restart"}, getLocale(PT).getString("cmd_restart"), DEV, false
			));
			add(new LeaveCommand(
					"sair", new String[]{"leave"}, getLocale(PT).getString(REQ_SERVER_ID), getLocale(PT).getString("cmd_leave"), DEV, true
			));
			add(new ToxicTagCommand(
					"toxico", new String[]{"toxic"}, getLocale(PT).getString(REQ_MENTION), getLocale(PT).getString("cmd_toxic-tag"), DEV, false
			));
			add(new PartnerTagCommand(
					"parceiro", new String[]{"partner"}, getLocale(PT).getString(REQ_MENTION), getLocale(PT).getString("cmd_partner-tag"), DEV, false
			));
			add(new VerifiedTagCommand(
					"verificado", new String[]{"verified"}, getLocale(PT).getString(REQ_MENTION), getLocale(PT).getString("cmd_verified-tag"), DEV, false
			));
			add(new RelaysCommand(
					"relays", getLocale(PT).getString("cmd_relay-list"), DEV, false
			));
			add(new LogCommand(
					"log", getLocale(PT).getString("cmd_log"), DEV, false
			));
			add(new TokenCommand(
					"chave", new String[]{"token"}, getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_token"), DEV, false
			));
			add(new BroadcastCommand(
					"transmitir", new String[]{"broadcast", "bc"}, getLocale(PT).getString("req_type-message"), getLocale(PT).getString("cmd_broadcast"), DEV, false
			));
			add(new UsageCommand(
					"usos", new String[]{"uses", "usage"}, getLocale(PT).getString("cmd_usage"), DEV, true
			));
			add(new SimpleWHMCommand(
					"wh", getLocale(PT).getString(REQ_MESSAGE), getLocale(PT).getString("cmd_simple-wh"), DEV, false
			));
			add(new MMLockCommand(
					"mmlock", getLocale(PT).getString("cmd_mm-lock"), DEV, false
			));
			add(new AddQuizCommand(
					"addquiz", new String[]{"addq", "novoquiz"}, getLocale(PT).getString("req_question-opts-correct-prize"), getLocale(PT).getString("cmd_add-quiz"), DEV, false
			));

			//SUPPORT
			add(new BlockCommand(
					"bloquear", new String[]{"block"}, getLocale(PT).getString("req_type-id-reason"), getLocale(PT).getString("cmd_block"), SUPPORT, false
			));
			add(new IDCommand(
					"id", getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_id"), SUPPORT, false
			));
			add(new InviteCommand(
					"convite", new String[]{"invite"}, getLocale(PT).getString(REQ_SERVER_ID), getLocale(PT).getString("cmd_invite"), SUPPORT, true
			));
			add(new RatingCommand(
					"pedirvoto", new String[]{"requestvote", "howwasi"}, getLocale(PT).getString("cmd_rating"), SUPPORT, false
			));
			add(new MarkTicketCommand(
					"mark", new String[]{"solved", "resolvido"}, getLocale(PT).getString("req_id"), getLocale(PT).getString("cmd_mark-ticket"), SUPPORT, false
			));

			//PARTNER
			add(new JibrilCommand(
					"jibril", getLocale(PT).getString("cmd_jibril"), PARTNER, false
			));
			add(new TetCommand(
					"tet", getLocale(PT).getString("cmd_tet"), PARTNER, false
			));
			add(new CompileCommand(
					"compilar", new String[]{"compile", "exec"}, getLocale(PT).getString("req_code"), getLocale(PT).getString("cmd_compile"), PARTNER, true
			));
			add(new JibrilEmoteListCommand(
					"jemotes", getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_j-emotes"), PARTNER, true
			));
			add(new PurchaceKGotchiCommand(
					"pkgotchi", new String[]{"buykgotchi", "comprarkgotchi"}, getLocale(PT).getString("req_kgotchi"), getLocale(PT).getString("cmd_kgotchi-shop"), PARTNER, false
			));
			add(new KGotchiCommand(
					"kgotchi", new String[]{"kg", "kawaig"}, getLocale(PT).getString("req_action"), getLocale(PT).getString("cmd_kgotchi"), PARTNER, false
			));
			add(new RelayCommand(
					"relay", new String[]{"relinfo", "relcon"}, getLocale(PT).getString("cmd_relay"), PARTNER, false
			));
			add(new TagsCommand(
					"tags", new String[]{"emblemas", "insignias"}, getLocale(PT).getString("cmd_tags"), PARTNER, false
			));
			add(new MyTagsCommand(
					"eu", new String[]{"meusemblemas", "mytags"}, getLocale(PT).getString("cmd_my-tags"), PARTNER, false
			));

			//MODERATION
			add(new RemoveAnswerCommand(
					"nãofale", getLocale(PT).getString("req_id"), getLocale(PT).getString("cmd_dont-say"), MODERACAO, false
			));
			add(new SettingsCommand(
					"settings", new String[]{"definicoes", "parametros", "configs"}, getLocale(PT).getString("req_parameter"), getLocale(PT).getString("cmd_settings"), MODERACAO, false
			));
			add(new AllowCommunityCommand(
					"ouçatodos", getLocale(PT).getString("cmd_allow-community"), MODERACAO, false
			));
			add(new KickMemberCommand(
					"kick", new String[]{"expulsar", "remover"}, getLocale(PT).getString(REQ_MENTION_REASON), getLocale(PT).getString("cmd_kick"), MODERACAO, false
			));
			add(new BanMemberCommand(
					"ban", new String[]{"banir"}, getLocale(PT).getString(REQ_MENTION_REASON), getLocale(PT).getString("cmd_ban"), MODERACAO, false
			));
			add(new NoLinkCommand(
					"semlink", new String[]{"nolink", "blocklink"}, getLocale(PT).getString("cmd_no-link"), MODERACAO, true
			));
			add(new AntispamCommand(
					"semspam", new String[]{"nospam", "antispam"}, getLocale(PT).getString("req_spam-type"), getLocale(PT).getString("cmd_no-spam"), MODERACAO, true
			));
			add(new AntiraidCommand(
					"semraid", new String[]{"noraid", "antiraid"}, getLocale(PT).getString("cmd_no-raid"), MODERACAO, false
			));
			add(new MakeLogCommand(
					"logchannel", new String[]{"makelog"}, getLocale(PT).getString("cmd_make-log"), MODERACAO, false
			));
			add(new PruneCommand(
					"prune", new String[]{"clean", "limpar"}, getLocale(PT).getString("req_qtd-all"), getLocale(PT).getString("cmd_prune"), MODERACAO, true
			));
			add(new LiteModeCommand(
					"litemode", new String[]{"lite"}, getLocale(PT).getString("cmd_lite-mode"), MODERACAO, false
			));
			add(new AllowImgCommand(
					"allowimg", new String[]{"aimg"}, getLocale(PT).getString("cmd_allow-images"), MODERACAO, false
			));
			add(new RoleChooserCommand(
					"botaocargo", new String[]{"rolebutton", "bc", "rb"}, getLocale(PT).getString("req_role-button"), getLocale(PT).getString("cmd_role-button"), MODERACAO, false
			));
			add(new GatekeeperCommand(
					"porteiro", new String[]{"gatekeeper", "gk"}, getLocale(PT).getString("req_id-role"), getLocale(PT).getString("cmd_gatekeeper"), MODERACAO, false
			));
			add(new BackupCommand(
					"backup", new String[]{"dados"}, getLocale(PT).getString("req_save-restore"), getLocale(PT).getString("cmd_backup"), MODERACAO, false
			));
			add(new RegenRulesCommand(
					"rrules", new String[]{"makerules"}, getLocale(PT).getString("cmd_regen-rules"), MODERACAO, true
			));
			add(new PermissionCommand(
					"permissões", new String[]{"perms", "permisions"}, getLocale(PT).getString("cmd_permission"), MODERACAO, false
			));
			add(new AddColorRoleCommand(
					"cargocor", new String[]{"rolecolor"}, getLocale(PT).getString("req_name-color"), getLocale(PT).getString("cmd_add-color-role"), MODERACAO, false
			));
			add(new MuteMemberCommand(
					"mute", new String[]{"mutar", "silenciar", "silence"}, getLocale(PT).getString("req_member"), getLocale(PT).getString("cmd_mute"), MODERACAO, false
			));

			//INFORMATION
			add(new ComandosCommand(
					"comandos", new String[]{"cmds", "cmd", "comando", "ajuda", "help"}, getLocale(PT).getString("req_command"), getLocale(PT).getString("cmd_help"), INFO, false
			));
			add(new ProfileCommand(
					"perfil", new String[]{"xp", "profile", "pf"}, getLocale(PT).getString("cmd_profile"), INFO, false
			));
			add(new ReportBugCommand(
					"bug", new String[]{"sendbug", "feedback"}, getLocale(PT).getString(REQ_MESSAGE), getLocale(PT).getString("cmd_bug"), INFO, false
			));
			add(new ReportUserCommand(
					"report", new String[]{"reportar"}, getLocale(PT).getString("req_user-reason"), getLocale(PT).getString("cmd_report"), INFO, false
			));
			add(new RequestAssistCommand(
					"suporte", new String[]{"support", "assist"}, getLocale(PT).getString("cmd_request-assist"), INFO, false
			));
			add(new BotInfoCommand(
					"info", new String[]{"botinfo", "bot"}, getLocale(PT).getString("cmd_info"), INFO, false
			));
			add(new URankCommand(
					"rank", new String[]{"ranking", "top10"}, getLocale(PT).getString("req_global"), getLocale(PT).getString("cmd_rank"), INFO, true
			));
			add(new ColorTesterCommand(
					"quecor", new String[]{"tcolor", "testcolor"}, getLocale(PT).getString("req_color"), getLocale(PT).getString("cmd_color"), INFO, false
			));
			add(new LocalEmoteListCommand(
					"emotes", getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_emotes"), INFO, true
			));
			add(new ShiroEmoteListCommand(
					"semotes", getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_s-emotes"), INFO, true
			));
			add(new WalletCommand(
					"carteira", new String[]{"banco", "bank", "money", "wallet", "atm"}, getLocale(PT).getString("cmd_wallet"), INFO, false
			));
			add(new PingCommand(
					"ping", getLocale(PT).getString("cmd_ping"), INFO, false
			));
			add(new UptimeCommand(
					"uptime", getLocale(PT).getString("cmd_uptime"), INFO, false
			));
			add(new ListScoreCommand(
					"notas", new String[]{"scores"}, getLocale(PT).getString("cmd_score"), INFO, true
			));

			//MISC
			add(new BackgroundCommand(
					"background", new String[]{"fundo", "bg"}, getLocale(PT).getString(REQ_LINK), getLocale(PT).getString("cmd_background"), MISC, false
			));
			add(new BiographyCommand(
					"bio", new String[]{"story", "desc"}, getLocale(PT).getString(REQ_MESSAGE), getLocale(PT).getString("cmd_biography"), MISC, false
			));
			add(new AsciiCommand(
					"ascii", getLocale(PT).getString(REQ_TEXT), getLocale(PT).getString("cmd_ascii"), MISC, false
			));
			add(new AvatarCommand(
					"avatar", getLocale(PT).getString("req_mention-guild"), getLocale(PT).getString("cmd_avatar"), MISC, false
			));
			add(new FlipCoinCommand(
					"flipcoin", new String[]{"caracoroa", "headstails"}, getLocale(PT).getString("cmd_heads-tails"), MISC, false
			));
			add(new ReverseCommand(
					"reverse", new String[]{"inverter"}, getLocale(PT).getString(REQ_TEXT), getLocale(PT).getString("cmd_reverse"), MISC, false
			));
			add(new SayCommand(
					"say", new String[]{"diga", "repetir"}, getLocale(PT).getString(REQ_MESSAGE), getLocale(PT).getString("cmd_repeat"), MISC, true
			));
			add(new CustomAnswerCommand(
					"fale", getLocale(PT).getString("req_trigger-response"), getLocale(PT).getString("cmd_custom-answer"), MISC, false
			));
			add(new AnimeCommand(
					"anime", new String[]{"desenho", "cartoon"}, getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_anime"), INFO, false
			));
			add(new ValidateGIFCommand(
					"validate", new String[]{"testgif", "tgif"}, getLocale(PT).getString(REQ_LINK), getLocale(PT).getString("cmd_dimension-test"), MISC, false
			));
			add(new EmbedCommand(
					"embed", getLocale(PT).getString("req_json"), getLocale(PT).getString("cmd_embed"), MISC, false
			));
			add(new PollCommand(
					"enquete", new String[]{"poll"}, getLocale(PT).getString(REQ_QUESTION), getLocale(PT).getString("cmd_poll"), MISC, true
			));
			add(new TheAnswerCommand(
					"arespostaé", new String[]{"theansweris", "responder", "answer"}, getLocale(PT).getString("cmd_rules"), MISC, true
			));
			add(new BinaryCommand(
					"bin", getLocale(PT).getString(REQ_TEXT), getLocale(PT).getString("cmd_binary"), MISC, false
			));
			add(new LinkTesterCommand(
					"link", new String[]{"try"}, getLocale(PT).getString(REQ_LINK), getLocale(PT).getString("cmd_link-test"), MISC, false
			));
			add(new VoteCommand(
					"votar", new String[]{"vote"}, getLocale(PT).getString("req_mention-positive-negative"), getLocale(PT).getString("cmd_vote"), MISC, false
			));
			add(new TranslateCommand(
					"traduzir", new String[]{"translate", "traduza", "trad"}, getLocale(PT).getString("req_from-to-text"), getLocale(PT).getString("cmd_translate"), MISC, false
			));
			add(new EightBallCommand(
					"8ball", getLocale(PT).getString(REQ_QUESTION), getLocale(PT).getString("cmd_8ball"), MISC, false
			));
			add(new ChooseCommand(
					"escolha", new String[]{"choose"}, getLocale(PT).getString("req_options"), getLocale(PT).getString("cmd_choose"), MISC, false
			));
			add(new ColorRoleCommand(
					"cor", new String[]{"color"}, getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_color-role"), MISC, false
			));
			add(new ImageCommand(
					"image", new String[]{"imagem", "img"}, getLocale(PT).getString("req_tags"), getLocale(PT).getString("cmd_image"), MISC, false
			));

			//FUN
			add(new SadButTrueCommand(
					"tristemasverdade", new String[]{"tmv", "sadbuttrue", "sbt"}, getLocale(PT).getString("req_truth"), getLocale(PT).getString("cmd_sad-but-true"), FUN, false
			));
			add(new HardDecisionCommand(
					"doisbotoes", new String[]{"tb", "twobuttons", "buttons"}, getLocale(PT).getString(REQ_TWO_OPTIONS), getLocale(PT).getString("cmd_two-buttons"), FUN, false
			));
			add(new ExpandingBrainCommand(
					"menteexpandida", new String[]{"eb", "expandingbrain", "brain"}, getLocale(PT).getString("req_four-options"), getLocale(PT).getString("cmd_expanded-brain"), FUN, false
			));
			add(new PPTCommand(
					"jankenpon", new String[]{"ppt", "rps", "jokenpo", "janken"}, getLocale(PT).getString("req_jakenpon"), getLocale(PT).getString("cmd_jankenpon"), FUN, false
			));
			add(new ShipCommand(
					"ship", new String[]{"shippar"}, getLocale(PT).getString("req_two-mentions"), getLocale(PT).getString("cmd_ship"), FUN, false
			));
			add(new MarryCommand(
					"casar", new String[]{"declarar", "marry"}, getLocale(PT).getString(REQ_MENTION), getLocale(PT).getString("cmd_marry"), FUN, false
			));
			add(new StonksCommand(
					"stonks", new String[]{"stks"}, getLocale(PT).getString(REQ_TEXT), getLocale(PT).getString("cmd_stonks"), FUN, false
			));
			add(new NotStonksCommand(
					"notstonks", new String[]{"notstks", "stinks"}, getLocale(PT).getString(REQ_TEXT), getLocale(PT).getString("cmd_stinks"), FUN, false
			));
			add(new GuessIllDieCommand(
					"guessilldie", new String[]{"gid", "achoquevoumorrer", "meh"}, getLocale(PT).getString(REQ_TEXT), getLocale(PT).getString("cmd_guess-ill-die"), FUN, false
			));
			add(new PatheticCommand(
					"patetico", new String[]{"pathetic"}, getLocale(PT).getString(REQ_TEXT), getLocale(PT).getString("cmd_pathetic"), FUN, false
			));
			add(new DrakeCommand(
					"drake", new String[]{"drk"}, getLocale(PT).getString(REQ_TWO_OPTIONS), getLocale(PT).getString("cmd_drake"), FUN, false
			));
			add(new SpiderManCommand(
					"homemaranha", new String[]{"spiderman", "spoda", "miranha"}, getLocale(PT).getString(REQ_TEXT), getLocale(PT).getString("cmd_spider-man"), FUN, false
			));
			add(new TomCruiseCommand(
					"tomcruise", new String[]{"vainessa", "iludido", "noyoullnot"}, getLocale(PT).getString(REQ_TEXT), getLocale(PT).getString("cmd_tom-cruise"), FUN, false
			));
			add(new PixelCanvasCommand(
					"canvas", new String[]{"pixel", "pixelcanvas"}, getLocale(PT).getString("req_x-y-color"), getLocale(PT).getString("cmd_canvas"), FUN, false
			));
			add(new PixelChunkCommand(
					"chunk", new String[]{"zone", "pixelchunk"}, getLocale(PT).getString("req_zone-x-y-color"), getLocale(PT).getString("cmd_canvas-chunk"), FUN, false
			));
			add(new DivorceCommand(
					"divorciar", new String[]{"separar", "divorce"}, getLocale(PT).getString("cmd_divorce"), FUN, false
			));
			add(new SlotsCommand(
					"slots", new String[]{"roleta"}, getLocale(PT).getString("req_bet"), getLocale(PT).getString("cmd_slots"), FUN, false
			));
			add(new QuizCommand(
					"quiz", new String[]{"qna", "per"}, getLocale(PT).getString("cmd_quiz"), FUN, true
			));
			add(new GuessTheNumberCommand(
					"adivinheonumero", new String[]{"aon", "guessthenumber", "gtn"}, getLocale(PT).getString("cmd_guess-the-number"), FUN, true
			));
			add(new HugReaction(
					"abraçar", new String[]{"abracar", "hug", "vemca"}, getLocale(PT).getString("cmd_hug"), true, "hug"
			));
			add(new KissReaction(
					"beijar", new String[]{"beijo", "kiss", "smac"}, getLocale(PT).getString("cmd_kiss"), true, "kiss"
			));
			add(new PatReaction(
					"cafuné", new String[]{"cafunhé", "pat", "cafu"}, getLocale(PT).getString("cmd_pat"), true, "pat"
			));
			add(new StareReaction(
					"encarar", new String[]{"shiii", "stare", "..."}, getLocale(PT).getString("cmd_stare"), true, "stare"
			));
			add(new SlapReaction(
					"estapear", new String[]{"tapa", "slap", "baka"}, getLocale(PT).getString("cmd_slap"), true, "slap"
			));
			add(new PunchReaction(
					"socar", new String[]{"chega", "tomaessa", "punch"}, getLocale(PT).getString("cmd_punch"), true, "smash"
			));
			add(new BiteReaction(
					"morder", new String[]{"moider", "bite", "moide"}, getLocale(PT).getString("cmd_bite"), true, "bite"
			));
			add(new BlushReaction(
					"vergonha", new String[]{"n-nani", "blush", "pft"}, getLocale(PT).getString("cmd_blush"), false, "blush"
			));
			add(new CryReaction(
					"chorar", new String[]{"buaa", "cry", "sadboy"}, getLocale(PT).getString("cmd_cry"), false, "sad"
			));
			add(new DanceReaction(
					"dançar", new String[]{"dancar", "dance", "tuts"}, getLocale(PT).getString("cmd_dance"), false, "dance"
			));
			add(new FacedeskReaction(
					"facedesk", new String[]{"mds", "ahnão", "nss"}, getLocale(PT).getString("cmd_facedesk"), false, "facedesk"
			));
			add(new LaughReaction(
					"rir", new String[]{"kkk", "laugh", "aiai"}, getLocale(PT).getString("cmd_laugh"), false, "laugh"
			));
			add(new NopeReaction(
					"nope", new String[]{"sqn", "hojenão", "esquiva"}, getLocale(PT).getString("cmd_nope"), false, "nope"
			));
			add(new RunReaction(
					"corre", new String[]{"saisai", "run", "foge"}, getLocale(PT).getString("cmd_run"), false, "run"
			));

			//MUSICA
			add(new ControlCommand(
					"controle", new String[]{"control", "c"}, getLocale(PT).getString("cmd_control"), MUSICA, false
			));
			add(new YoutubeCommand(
					"play", new String[]{"yt", "youtube"}, getLocale(PT).getString(REQ_NAME), getLocale(PT).getString("cmd_play"), MUSICA, false
			));

			//EXCEED
			add(new ExceedRankCommand(
					"exceedrank", new String[]{"exrank", "topexceed", "topex"}, getLocale(PT).getString("cmd_exceed-rank"), EXCEED, false
			));
			add(new ExceedSelectCommand(
					"exceedselect", new String[]{"exselect", "sou"}, getLocale(PT).getString("cmd_exceed"), EXCEED, false
			));

			//NSFW

		}};
	}

	public List<Command> getCommands() {
		return commands;
	}
}
