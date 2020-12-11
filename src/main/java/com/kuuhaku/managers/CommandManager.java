/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.command.commands.discord.beta.*;
import com.kuuhaku.command.commands.discord.dev.*;
import com.kuuhaku.command.commands.discord.exceed.*;
import com.kuuhaku.command.commands.discord.fun.*;
import com.kuuhaku.command.commands.discord.information.*;
import com.kuuhaku.command.commands.discord.misc.*;
import com.kuuhaku.command.commands.discord.moderation.*;
import com.kuuhaku.command.commands.discord.music.ControlCommand;
import com.kuuhaku.command.commands.discord.music.YoutubeCommand;
import com.kuuhaku.command.commands.discord.reactions.*;
import com.kuuhaku.command.commands.discord.reactions.answerable.*;
import com.kuuhaku.command.commands.discord.support.BlockCommand;
import com.kuuhaku.command.commands.discord.support.InviteCommand;
import com.kuuhaku.command.commands.discord.support.MarkTicketCommand;
import com.kuuhaku.command.commands.discord.support.RatingCommand;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static com.kuuhaku.command.Category.*;

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
	private static final String REQ_ID = "req_id";
	private static final String REQ_KEY_FILE = "req_key-file";
	private static final String REQ_CHANNEL = "req_channel";
	private static final String REQ_MENTION_BET = "req_mention-bet";
	private static final String REQ_COLOR = "req_color";
	private static final String REQ_ITEM = "req_item";
	private final HashMap<Class<? extends Command>, Argument> commands = new HashMap<>() {
		{
			//DEV
			put(KillCommand.class, new Argument(
					"desligar", new String[]{"kill"}, "cmd_kill", DEV, false
			));
			put(LeaveCommand.class, new Argument(
					"sair", new String[]{"leave"}, REQ_SERVER_ID, "cmd_leave", DEV, true
			));
			put(ToxicTagCommand.class, new Argument(
					"toxico", new String[]{"toxic"}, REQ_MENTION, "cmd_toxic-tag", DEV, false
			));
			put(BetaTagCommand.class, new Argument(
					"beta", REQ_MENTION, "cmd_beta-tag", DEV, false
			));
			put(VerifiedTagCommand.class, new Argument(
					"verificado", new String[]{"verified"}, REQ_MENTION, "cmd_verified-tag", DEV, false
			));
			put(RelaysCommand.class, new Argument(
					"relays", "cmd_relay-list", DEV, false
			));
			put(LogCommand.class, new Argument(
					"log", "cmd_log", DEV, false
			));
			put(TokenCommand.class, new Argument(
					"chave", new String[]{"token"}, REQ_NAME, "cmd_token", DEV, false
			));
			put(BroadcastCommand.class, new Argument(
					"transmitir", new String[]{"broadcast"}, "req_type-message", "cmd_broadcast", DEV, false
			));
			put(UsageCommand.class, new Argument(
					"usos", new String[]{"uses", "usage"}, "cmd_usage", DEV, true
			));
			put(SimpleWHMCommand.class, new Argument(
					"wh", REQ_MESSAGE, "cmd_simple-wh", DEV, false
			));
			put(MMLockCommand.class, new Argument(
					"mmlock", "cmd_mm-lock", DEV, false
			));
			put(DrawRaffleCommand.class, new Argument(
					"rifa", new String[]{"raffle"}, "req_period", "cmd_draw-raffle", DEV, false
			));
			put(GarbageCollectorCommand.class, new Argument(
					"coletarlixo", new String[]{"gc"}, "cmd_garbage-collector", DEV, false
			));
			put(BlacklistCommand.class, new Argument(
					"listanegra", new String[]{"bl", "blacklist"}, "cmd_blacklist", DEV, false
			));
			put(CompileCommand.class, new Argument(
					"compilar", new String[]{"compile", "exec"}, "req_code", "cmd_compile", DEV, true
			));
			put(SweepCommand.class, new Argument(
					"sweep", new String[]{"limpar", "cleanse"}, "cmd_sweep", DEV, true
			));
			put(BugHuntCommand.class, new Argument(
					"bughunt", new String[]{"hunter", "debugador"}, REQ_MENTION, "cmd_bug-hunter", DEV, false
			));
			put(ShardStatusCommand.class, new Argument(
					"shards", "cmd_shard-status", DEV, true
			));
			put(ShardRestartCommand.class, new Argument(
					"rshard", REQ_ID, "cmd_shard-restart", DEV, false
			));
			put(AuditCommand.class, new Argument(
					"audit", "req_type-id", "cmd_audit", DEV, true
			));

			//SUPPORT
			put(BlockCommand.class, new Argument(
					"bloquear", new String[]{"block"}, "req_type-id-reason", "cmd_block", SUPPORT, false
			));
			put(InviteCommand.class, new Argument(
					"convite", new String[]{"invite"}, REQ_SERVER_ID, "cmd_invite", SUPPORT, true
			));
			put(RatingCommand.class, new Argument(
					"pedirvoto", new String[]{"requestvote", "howwasi"}, "cmd_rating", SUPPORT, false
			));
			put(MarkTicketCommand.class, new Argument(
					"mark", new String[]{"solved", "resolvido"}, REQ_ID, "cmd_mark-ticket", SUPPORT, false
			));

			//BETA
			put(JibrilCommand.class, new Argument(
					"jibril", "cmd_jibril", BETA, false
			));
			put(TetCommand.class, new Argument(
					"tet", "cmd_tet", BETA, false
			));
			put(JibrilEmoteListCommand.class, new Argument(
					"jemotes", REQ_NAME, "cmd_j-emotes", BETA, true
			));
			put(PurchaceKGotchiCommand.class, new Argument(
					"pkgotchi", new String[]{"buykgotchi", "comprarkgotchi"}, "req_kgotchi", "cmd_kgotchi-shop", BETA, false
			));
			put(KGotchiCommand.class, new Argument(
					"kgotchi", new String[]{"kg", "kawaig"}, "req_action", "cmd_kgotchi", BETA, false
			));
			put(RelayCommand.class, new Argument(
					"relay", new String[]{"relinfo", "relcon"}, "cmd_relay", BETA, false
			));
			put(EncryptCommand.class, new Argument(
					"criptografar", new String[]{"crypt", "crpt"}, REQ_KEY_FILE, "cmd_encrypt", BETA, false
			));
			put(DecryptCommand.class, new Argument(
					"descriptografar", new String[]{"decrypt", "dcrpt"}, REQ_KEY_FILE, "cmd_decrypt", BETA, false
			));

			//MODERATION
			put(RemoveAnswerCommand.class, new Argument(
					"nãofale", "req_id-nothing", "cmd_remove-answer", MODERACAO, false
			));
			put(SettingsCommand.class, new Argument(
					"settings", new String[]{"definicoes", "parametros", "configs"}, "req_parameter", "cmd_settings", MODERACAO, false
			));
			put(AllowCommunityCommand.class, new Argument(
					"ouçatodos", "cmd_allow-community", MODERACAO, false
			));
			put(KickMemberCommand.class, new Argument(
					"kick", new String[]{"expulsar", "remover"}, REQ_MENTION_REASON, "cmd_kick", MODERACAO, false
			));
			put(BanMemberCommand.class, new Argument(
					"ban", new String[]{"banir"}, REQ_MENTION_REASON, "cmd_ban", MODERACAO, false
			));
			put(NoLinkCommand.class, new Argument(
					"semlink", new String[]{"nolink", "blocklink"}, "cmd_no-link", MODERACAO, true
			));
			put(AntispamCommand.class, new Argument(
					"semspam", new String[]{"nospam", "antispam"}, "req_spam-type-amount", "cmd_no-spam", MODERACAO, true
			));
			put(AntiraidCommand.class, new Argument(
					"semraid", new String[]{"noraid", "antiraid"}, "cmd_no-raid", MODERACAO, false
			));
			put(MakeLogCommand.class, new Argument(
					"logchannel", new String[]{"makelog"}, "cmd_make-log", MODERACAO, false
			));
			put(PruneCommand.class, new Argument(
					"prune", new String[]{"clean", "limpar"}, "req_qtd-all", "cmd_prune", MODERACAO, true
			));
			put(LiteModeCommand.class, new Argument(
					"litemode", new String[]{"lite"}, "cmd_lite-mode", MODERACAO, false
			));
			put(AllowImgCommand.class, new Argument(
					"allowimg", new String[]{"aimg"}, "cmd_allow-images", MODERACAO, false
			));
			put(RoleChooserCommand.class, new Argument(
					"botaocargo", new String[]{"rolebutton", "bc", "rb"}, "req_role-button", "cmd_role-button", MODERACAO, false
			));
			put(GatekeeperCommand.class, new Argument(
					"porteiro", new String[]{"gatekeeper", "gk"}, "req_id-role", "cmd_gatekeeper", MODERACAO, false
			));
			put(BackupCommand.class, new Argument(
					"backup", new String[]{"dados"}, "req_save-restore", "cmd_backup", MODERACAO, false
			));
			put(RegenRulesCommand.class, new Argument(
					"rrules", new String[]{"makerules"}, "cmd_regen-rules", MODERACAO, true
			));
			put(PermissionCommand.class, new Argument(
					"permissões", new String[]{"perms", "permisions"}, "cmd_permission", MODERACAO, false
			));
			put(AddColorRoleCommand.class, new Argument(
					"cargocor", new String[]{"rolecolor"}, "req_name-color", "cmd_add-color-role", MODERACAO, false
			));
			put(MuteMemberCommand.class, new Argument(
					"mute", new String[]{"mutar", "silenciar", "silence"}, "req_member-time-reason", "cmd_mute", MODERACAO, false
			));
			put(AllowKawaiponCommand.class, new Argument(
					"habilitarkp", new String[]{"enablekp", "hkp", "ekp"}, REQ_CHANNEL, "cmd_allow-kawaipon", MODERACAO, false
			));
			put(AllowDropsCommand.class, new Argument(
					"habilitardp", new String[]{"enabledp", "hdp", "edp"}, REQ_CHANNEL, "cmd_allow-drops", MODERACAO, false
			));
			put(PurchaseBuffCommand.class, new Argument(
					"melhorar", new String[]{"upgrade", "up"}, "req_type-tier", "cmd_purchase-buff", MODERACAO, false
			));
			put(ModifyRulesCommand.class, new Argument(
					"regra", new String[]{"rule", "r"}, "req_rule-index", "cmd_modify-rule", MODERACAO, false
			));
			put(UnmuteMemberCommand.class, new Argument(
					"unmute", new String[]{"desmutar", "dessilenciar", "unsilence"}, "req_mention", "cmd_unmute", MODERACAO, false
			));
			put(ToggleExceedRolesCommand.class, new Argument(
					"cargosexceed", new String[]{"exceedroles", "exroles", "cargosex"}, "cmd_toggle-exceed-roles", MODERACAO, false
			));
			put(NQNModeCommand.class, new Argument(
					"modonqn", new String[]{"nqnmode", "nqn", "autoemotes"}, "cmd_nqn-mode", MODERACAO, true
			));

			//INFORMATION
			put(HelpCommand.class, new Argument(
					"comandos", new String[]{"cmds", "cmd", "comando", "ajuda", "help"}, "req_command", "cmd_help", INFO, false
			));
			put(ProfileCommand.class, new Argument(
					"perfil", new String[]{"xp", "profile", "pf"}, "cmd_profile", INFO, false
			));
			put(ReportBugCommand.class, new Argument(
					"bug", new String[]{"sendbug", "feedback"}, REQ_MESSAGE, "cmd_bug", INFO, false
			));
			put(ReportUserCommand.class, new Argument(
					"report", new String[]{"reportar"}, "req_user-reason", "cmd_report", INFO, false
			));
			put(RequestAssistCommand.class, new Argument(
					"suporte", new String[]{"support", "assist"}, "cmd_request-assist", INFO, false
			));
			put(BotInfoCommand.class, new Argument(
					"info", new String[]{"botinfo", "bot"}, "cmd_info", INFO, false
			));
			put(URankCommand.class, new Argument(
					"rank", new String[]{"ranking", "top10"}, "req_global-credit-card", "cmd_rank", INFO, true
			));
			put(ColorTesterCommand.class, new Argument(
					"quecor", new String[]{"tcolor", "testcolor"}, REQ_COLOR, "cmd_color", INFO, false
			));
			put(LocalEmoteListCommand.class, new Argument(
					"emotes", REQ_NAME, "cmd_emotes", INFO, true
			));
			put(ShiroEmoteListCommand.class, new Argument(
					"semotes", REQ_NAME, "cmd_s-emotes", INFO, true
			));
			put(WalletCommand.class, new Argument(
					"carteira", new String[]{"banco", "bank", "money", "wallet", "atm"}, "cmd_wallet", INFO, false
			));
			put(PingCommand.class, new Argument(
					"ping", "cmd_ping", INFO, false
			));
			put(UptimeCommand.class, new Argument(
					"uptime", "cmd_uptime", INFO, false
			));
			put(ListScoreCommand.class, new Argument(
					"notas", new String[]{"scores"}, "cmd_score", INFO, true
			));
			put(TagsCommand.class, new Argument(
					"tags", new String[]{"emblemas", "insignias"}, "cmd_tags", INFO, false
			));
			put(MyStatsCommand.class, new Argument(
					"eu", new String[]{"meustatus", "mystats"}, "cmd_my-stats", INFO, false
			));
			put(MyBuffsCommand.class, new Argument(
					"buffs", new String[]{"meusbuffs", "xpmodifiers", "xpmodifs"}, "cmd_my-buffs", INFO, false
			));
			put(HttpCatCommand.class, new Argument(
					"catnet", new String[]{"httpcat"}, "cmd_http-cat", INFO, false
			));
			put(KawaiponsCommand.class, new Argument(
					"kawaipons", new String[]{"kps"}, "req_anime-rarity-type", "cmd_kawaipons", INFO, false
			));
			put(RemainingCardsCommand.class, new Argument(
					"cartasrestantes", new String[]{"restante", "remaining"}, "req_anime", "cmd_remaining-cards", INFO, false
			));
			put(RemindMeCommand.class, new Argument(
					"melembre", new String[]{"remindme", "notifyvote", "meavise"}, "cmd_remind-me", INFO, false
			));
			put(CardValueCommand.class, new Argument(
					"valor", new String[]{"value"}, "req_card", "cmd_card-value", INFO, false
			));
			put(ShoukanDeckCommand.class, new Argument(
					"deck", "req_daily-deck-p", "cmd_shoukan-deck", INFO, false
			));
			put(VoteCommand.class, new Argument(
					"votar", new String[]{"vote", "upvote"}, "cmd_vote", INFO, false
			));
			put(DonateCommand.class, new Argument(
					"doar", new String[]{"donate"}, "cmd_donate", INFO, false
			));
			put(MyTicketsCommand.class, new Argument(
					"bilhetes", new String[]{"tickets"}, "cmd_tickets", INFO, false
			));
			put(DeckEvalCommand.class, new Argument(
					"avaliardeck", new String[]{"deckeval"}, "cmd_deck-eval", INFO, false
			));

			//MISC
			put(BackgroundCommand.class, new Argument(
					"background", new String[]{"fundo", "bg"}, REQ_LINK, "cmd_background", MISC, false
			));
			put(BiographyCommand.class, new Argument(
					"bio", new String[]{"story", "desc"}, REQ_MESSAGE, "cmd_biography", MISC, false
			));
			put(ProfileColorCommand.class, new Argument(
					"cordoperfil", new String[]{"profilecolor", "pc", "cp"}, REQ_COLOR, "cmd_profile-color", MISC, false
			));
			put(AsciiCommand.class, new Argument(
					"ascii", "req_text-image", "cmd_ascii", MISC, false
			));
			put(AvatarCommand.class, new Argument(
					"avatar", "req_mention-guild", "cmd_avatar", MISC, false
			));
			put(FlipCoinCommand.class, new Argument(
					"flipcoin", new String[]{"caracoroa", "headstails"}, "cmd_heads-tails", MISC, false
			));
			put(ReverseCommand.class, new Argument(
					"reverse", new String[]{"inverter"}, REQ_TEXT, "cmd_reverse", MISC, false
			));
			put(SayCommand.class, new Argument(
					"say", new String[]{"diga", "repetir"}, REQ_MESSAGE, "cmd_repeat", MISC, true
			));
			put(CustomAnswerCommand.class, new Argument(
					"fale", "req_trigger-response", "cmd_custom-answer", MISC, false
			));
			put(AnimeCommand.class, new Argument(
					"anime", new String[]{"desenho", "cartoon"}, REQ_NAME, "cmd_anime", INFO, false
			));
			put(ValidateGIFCommand.class, new Argument(
					"validate", new String[]{"testgif", "tgif"}, REQ_LINK, "cmd_dimension-test", MISC, false
			));
			put(EmbedCommand.class, new Argument(
					"embed", "req_json", "cmd_embed", MISC, false
			));
			put(PollCommand.class, new Argument(
					"enquete", new String[]{"poll"}, REQ_QUESTION, "cmd_poll", MISC, true
			));
			put(TheAnswerCommand.class, new Argument(
					"arespostaé", new String[]{"theansweris", "responder", "answer"}, "cmd_rules", MISC, true
			));
			put(BinaryCommand.class, new Argument(
					"bin", REQ_TEXT, "cmd_binary", MISC, false
			));
			put(LinkTesterCommand.class, new Argument(
					"link", new String[]{"try"}, REQ_LINK, "cmd_link-test", MISC, false
			));
			put(RateCommand.class, new Argument(
					"avaliar", new String[]{"rate"}, "req_mention-positive-negative", "cmd_rate", MISC, false
			));
			/*put(TranslateCommand.class, new Argument(
					"traduzir", new String[]{"translate", "traduza", "trad"}, "req_from-to-text", "cmd_translate", MISC, false
			));*/
			put(EightBallCommand.class, new Argument(
					"8ball", REQ_QUESTION, "cmd_8ball", MISC, false
			));
			put(ChooseCommand.class, new Argument(
					"escolha", new String[]{"choose"}, "req_options", "cmd_choose", MISC, false
			));
			put(ColorRoleCommand.class, new Argument(
					"cor", new String[]{"color"}, REQ_NAME, "cmd_color-role", MISC, false
			));
			//put(ImageCommand.class, new Argument(
			//		"image", new String[]{"imagem", "img"}, "req_tags", "cmd_image", MISC, false
			//));
			put(TransferCommand.class, new Argument(
					"transferir", new String[]{"transfer", "tr"}, "req_user-amount", "cmd_transfer", MISC, false
			));
			put(TradeCardCommand.class, new Argument(
					"trocar", new String[]{"trade"}, "req_user-card-amount", "cmd_trade-card", MISC, true
			));
			put(BuyCardCommand.class, new Argument(
					"comprar", new String[]{"buy"}, REQ_ID, "cmd_buy-card", MISC, true
			));
			put(SellCardCommand.class, new Argument(
					"anunciar", new String[]{"sell"}, "req_card-type-price", "cmd_sell-card", MISC, true
			));
			put(PseudoNameCommand.class, new Argument(
					"pseudonimo", new String[]{"pnome", "pname"}, REQ_NAME, "cmd_pseudo-name", MISC, true
			));
			put(PseudoAvatarCommand.class, new Argument(
					"pseudoavatar", new String[]{"pavatar"}, REQ_LINK, "cmd_pseudo-avatar", MISC, true
			));
			put(SeeCardCommand.class, new Argument(
					"carta", new String[]{"card", "see", "olhar"}, "req_card-type", "cmd_see-card", MISC, false
			));
			put(LoanCommand.class, new Argument(
					"emprestimo", new String[]{"emp", "loan"}, REQ_ID, "cmd_loan", MISC, false
			));
			put(RedeemCommand.class, new Argument(
					"trocargema", new String[]{"resgatar", "redeem"}, "cmd_redeem", MISC, true
			));
			put(VIPShopCommand.class, new Argument(
					"vip", new String[]{"lojavip", "gemshop"}, REQ_ID, "cmd_vip", MISC, false
			));
			put(PayLoanCommand.class, new Argument(
					"pagar", new String[]{"pay", "payloan"}, "cmd_pay-loan", MISC, false
			));
			put(BindCommand.class, new Argument(
					"vincular", new String[]{"bind"}, "cmd_bind", MISC, false
			));
			put(UnbindCommand.class, new Argument(
					"desvincular", new String[]{"unbind"}, "cmd_unbind", MISC, false
			));
			put(LotteryCommand.class, new Argument(
					"loteria", new String[]{"lottery"}, "req_dozens", "cmd_lottery", MISC, true
			));
			put(BuyConsumableCommand.class, new Argument(
					"loja", new String[]{"shop"}, REQ_ITEM, "cmd_item-shop", MISC, false
			));
			put(UseConsumableCommand.class, new Argument(
					"usar", new String[]{"use"}, REQ_ITEM, "cmd_use-item", MISC, false
			));
			put(FrameColorCommand.class, new Argument(
					"cordaborda", new String[]{"borda", "framecolor", "frame"}, "req_frame-color", "cmd_frame-color", MISC, true
			));
			put(FrameBackgroundCommand.class, new Argument(
					"fundododeck", new String[]{"deckbg"}, "req_ultimate", "cmd_frame-background", MISC, false
			));
			put(ConvertCardCommand.class, new Argument(
					"converter", new String[]{"convert"}, "req_card", "cmd_convert-card", MISC, true
			));
			put(RevertCardCommand.class, new Argument(
					"reverter", new String[]{"revert"}, "req_card", "cmd_revert-card", MISC, true
			));
			put(SynthesizeCardCommand.class, new Argument(
					"sintetizar", new String[]{"synthesize", "synth"}, "req_cards-type", "cmd_synthesize-card", MISC, true
			));
			put(AuctionCommand.class, new Argument(
					"leilão", new String[]{"auction", "leilao", "auct"}, "req_card-type-price", "cmd_auction", MISC, true
			));
			put(ProfileTrophyCommand.class, new Argument(
					"trofeu", new String[]{"trophy"}, "req_id-reset", "cmd_trophy", MISC, true
			));

			//FUN
			put(SadButTrueCommand.class, new Argument(
					"tristemasverdade", new String[]{"tmv", "sadbuttrue", "sbt"}, "req_truth", "cmd_sad-but-true", FUN, false
			));
			put(HardDecisionCommand.class, new Argument(
					"doisbotoes", new String[]{"tb", "twobuttons", "buttons"}, REQ_TWO_OPTIONS, "cmd_two-buttons", FUN, false
			));
			put(ExpandingBrainCommand.class, new Argument(
					"menteexpandida", new String[]{"eb", "expandingbrain", "brain"}, "req_four-options", "cmd_expanded-brain", FUN, false
			));
			put(JojoCommand.class, new Argument(
					"jojo", new String[]{"jj", "kickhim", "chutaele"}, "req_two-mentions-message", "cmd_jojo", FUN, false
			));
			put(RPSCommand.class, new Argument(
					"jankenpon", new String[]{"ppt", "rps", "jokenpo", "janken"}, "req_jakenpon", "cmd_jankenpon", FUN, false
			));
			put(ShipCommand.class, new Argument(
					"ship", new String[]{"shippar"}, "req_two-mentions", "cmd_ship", FUN, false
			));
			put(MarryCommand.class, new Argument(
					"casar", new String[]{"declarar", "marry"}, REQ_MENTION, "cmd_marry", FUN, false
			));
			put(StonksCommand.class, new Argument(
					"stonks", new String[]{"stks"}, REQ_TEXT, "cmd_stonks", FUN, false
			));
			put(NotStonksCommand.class, new Argument(
					"notstonks", new String[]{"notstks", "stinks"}, REQ_TEXT, "cmd_stinks", FUN, false
			));
			put(GuessIllDieCommand.class, new Argument(
					"guessilldie", new String[]{"gid", "achoquevoumorrer", "meh"}, REQ_TEXT, "cmd_guess-ill-die", FUN, false
			));
			put(PatheticCommand.class, new Argument(
					"patetico", new String[]{"pathetic"}, REQ_TEXT, "cmd_pathetic", FUN, false
			));
			put(DrakeCommand.class, new Argument(
					"drake", new String[]{"drk"}, REQ_TWO_OPTIONS, "cmd_drake", FUN, false
			));
			put(SpiderManCommand.class, new Argument(
					"homemaranha", new String[]{"spiderman", "spoda", "miranha"}, REQ_TEXT, "cmd_spider-man", FUN, false
			));
			put(TomCruiseCommand.class, new Argument(
					"tomcruise", new String[]{"vainessa", "iludido", "noyoullnot"}, REQ_TEXT, "cmd_tom-cruise", FUN, false
			));
			put(PixelCanvasCommand.class, new Argument(
					"canvas", new String[]{"pixel", "pixelcanvas"}, "req_x-y-color", "cmd_canvas", FUN, false
			));
			put(PixelChunkCommand.class, new Argument(
					"chunk", new String[]{"zone", "pixelchunk"}, "req_zone-x-y-color", "cmd_canvas-chunk", FUN, false
			));
			put(DivorceCommand.class, new Argument(
					"divorciar", new String[]{"separar", "divorce"}, "cmd_divorce", FUN, false
			));
			put(SlotsCommand.class, new Argument(
					"slots", new String[]{"roleta"}, "req_bet", "cmd_slots", FUN, false
			));
			put(QuizCommand.class, new Argument(
					"quiz", new String[]{"qna", "per"}, "req_difficulty", "cmd_quiz", FUN, true
			));
			put(GuessTheNumberCommand.class, new Argument(
					"adivinheonumero", new String[]{"aon", "guessthenumber", "gtn"}, "cmd_guess-the-number", FUN, true
			));
			put(CrissCrossCommand.class, new Argument(
					"jogodavelha", new String[]{"jdv", "crisscross", "cc"}, REQ_MENTION_BET, "cmd_criss-cross", FUN, true
			));
			put(ChessCommand.class, new Argument(
					"xadrez", new String[]{"chess"}, REQ_MENTION, "cmd_chess", FUN, true
			));
			put(HitotsuCommand.class, new Argument(
					"hitotsu", new String[]{"uno"}, "req_bet-mentions", "cmd_hitotsu", FUN, true
			));
			put(ReversiCommand.class, new Argument(
					"reversi", new String[]{"othello"}, REQ_MENTION_BET, "cmd_reversi", FUN, true
			));
			put(ShoukanCommand.class, new Argument(
					"shoukan", new String[]{"duelcards"}, "req_mention-bet-daily-custom", "cmd_shoukan", FUN, true
			));
			put(CatchKawaiponCommand.class, new Argument(
					"coletar", new String[]{"collect"}, "cmd_catch-kawaipon", FUN, false
			));
			put(CatchDropCommand.class, new Argument(
					"abrir", new String[]{"open"}, "req_captcha", "cmd_catch-drop", FUN, false
			));
			put(LearnToSearchCommand.class, new Argument(
					"pesquisar", new String[]{"search", "lts", "aap"}, "req_search", "cmd_learn-to-search", FUN, false
			));
			put(GuessTheCardsCommand.class, new Argument(
					"adivinheascartas", new String[]{"aac", "guessthecards", "gtc"}, "cmd_guess-the-cards", FUN, false
			));

			//MUSICA
			put(ControlCommand.class, new Argument(
					"controle", new String[]{"control", "c"}, "cmd_control", MUSICA, false
			));
			put(YoutubeCommand.class, new Argument(
					"play", new String[]{"yt", "youtube"}, REQ_NAME, "cmd_play", MUSICA, false
			));

			//EXCEED
			put(ExceedRankCommand.class, new Argument(
					"exceedrank", new String[]{"exrank", "topexceed", "topex"}, "req_actual", "cmd_exceed-rank", EXCEED, false
			));
			put(ExceedSelectCommand.class, new Argument(
					"exceedselect", new String[]{"exselect", "souex"}, "cmd_exceed", EXCEED, false
			));
			put(ExceedPaletteCommand.class, new Argument(
					"exceedpalette", new String[]{"expalette", "paletaex"}, "cmd_exceed-palette", EXCEED, false
			));
			put(ExceedLeaveCommand.class, new Argument(
					"exceedsair", new String[]{"exleave", "sairex"}, "cmd_exceed-leave", EXCEED, true
			));
			/*
			put(DisboardCommand.class, new Arguments(
					"disboard", new String[]{"exmap", "mapa"}, "cmd_disboard", EXCEED, false
			));
			*/
			put(ExceedMembersCommand.class, new Argument(
					"exceedmembros", new String[]{"exmembers", "membrosx"}, "cmd_exceed-members", EXCEED, true
			));

			//REACTIONS
			put(HugReaction.class, new ReactionArgument(
					"abraçar", new String[]{"abracar", "hug", "vemca"}, "cmd_hug", true, "hug"
			));
			put(KissReaction.class, new ReactionArgument(
					"beijar", new String[]{"beijo", "kiss", "smac"}, "cmd_kiss", true, "kiss"
			));
			put(PatReaction.class, new ReactionArgument(
					"cafuné", new String[]{"cafunhé", "pat", "cafu"}, "cmd_pat", true, "pat"
			));
			put(StareReaction.class, new ReactionArgument(
					"encarar", new String[]{"shiii", "stare", "..."}, "cmd_stare", true, "stare"
			));
			put(SlapReaction.class, new ReactionArgument(
					"estapear", new String[]{"tapa", "slap", "baka"}, "cmd_slap", true, "slap"
			));
			put(PunchReaction.class, new ReactionArgument(
					"socar", new String[]{"chega", "tomaessa", "punch"}, "cmd_punch", true, "smash"
			));
			put(BiteReaction.class, new ReactionArgument(
					"morder", new String[]{"moider", "bite", "moide"}, "cmd_bite", true, "bite"
			));
			put(BlushReaction.class, new ReactionArgument(
					"vergonha", new String[]{"n-nani", "blush", "pft"}, "cmd_blush", false, "blush"
			));
			put(CryReaction.class, new ReactionArgument(
					"chorar", new String[]{"buaa", "cry", "sadboy"}, "cmd_cry", false, "sad"
			));
			put(DanceReaction.class, new ReactionArgument(
					"dançar", new String[]{"dancar", "dance", "tuts"}, "cmd_dance", false, "dance"
			));
			put(FacedeskReaction.class, new ReactionArgument(
					"facedesk", new String[]{"mds", "ahnão", "nss"}, "cmd_facedesk", false, "facedesk"
			));
			put(LaughReaction.class, new ReactionArgument(
					"rir", new String[]{"kkk", "laugh", "aiai"}, "cmd_laugh", false, "laugh"
			));
			put(NopeReaction.class, new ReactionArgument(
					"nope", new String[]{"sqn", "hojenão", "esquiva"}, "cmd_nope", false, "nope"
			));
			put(RunReaction.class, new ReactionArgument(
					"corre", new String[]{"saisai", "run", "foge"}, "cmd_run", false, "run"
			));
		}
	};

	public HashMap<Class<? extends Command>, Argument> getCommands() {
		return commands;
	}

	public Command getCommand(String name) {
		Map.Entry<Class<? extends Command>, Argument> cmd = commands.entrySet().stream().filter(e -> e.getValue().getName().equalsIgnoreCase(name) || ArrayUtils.contains(e.getValue().getAliases(), name.toLowerCase())).findFirst().orElse(null);

		if (cmd == null) return null;

		try {
			if (cmd.getValue() instanceof ReactionArgument)
				//noinspection JavaReflectionInvocation
				return cmd.getKey()
						.getConstructor(String.class, String[].class, String.class, boolean.class, String.class)
						.newInstance(cmd.getValue().getArguments());
			else
				//noinspection JavaReflectionInvocation
				return cmd.getKey()
						.getConstructor(String.class, String[].class, String.class, String.class, Category.class, boolean.class)
						.newInstance(cmd.getValue().getArguments());
		} catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}
}
