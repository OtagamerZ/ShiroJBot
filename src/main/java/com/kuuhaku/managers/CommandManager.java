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
import com.kuuhaku.command.Executable;
import com.kuuhaku.command.commands.PreparedCommand;
import com.kuuhaku.command.commands.discord.fun.*;
import com.kuuhaku.command.commands.discord.misc.*;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.utils.Helper;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.kuuhaku.command.Category.*;

public class CommandManager {

	private static final String REQ_MENTION = "req_mention";
	private static final String REQ_MESSAGE = "req_message";
	private static final String REQ_NAME = "req_name";
	private static final String REQ_SERVER_ID = "req_server-id";
	private static final String REQ_MENTION_REASON = "req_mention-id-reason";
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
	private final HashMap<Class<? extends Executable>, Argument> commands = new HashMap<>() {
		{
			/*put(MatchStatsCommand.class, new Argument(
					"dadosdapartida", new String[]{"matchstats", "mstats", "estatisticas"}, REQ_ID, "cmd_match-stats", INFO, true
			));*/

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
					"leilao", new String[]{"auction", "auct"}, "req_card-type-price", "cmd_auction", MISC, true
			));
			put(ProfileTrophyCommand.class, new Argument(
					"trofeu", new String[]{"trophy"}, "req_id-reset", "cmd_trophy", MISC, true
			));
			put(DestinyCardsCommand.class, new Argument(
					"destino", new String[]{"destiny", "dest"}, "req_positions", "cmd_destiny-cards", MISC, false
			));
			put(DeckStashCommand.class, new Argument(
					"reserva", new String[]{"stash", "estoque"}, "req_slot", "cmd_deck-stash", MISC, true
			));
			put(GiftCodeCommand.class, new Argument(
					"giftcode", "req_type-amount-code", "cmd_gift-code", MISC, false
			));
			put(InvestCommand.class, new Argument(
					"investir", new String[]{"invest"}, "req_card-value", "cmd_invest", MISC, true
			));
			put(RecoverCommand.class, new Argument(
					"retirar", new String[]{"recover"}, "req_card-value", "cmd_recover", MISC, true
			));
			put(ShoukanMasterCommand.class, new Argument(
					"tutor", new String[]{"teacher", "mestre", "master"}, REQ_MENTION, "cmd_shoukan-master", MISC, true
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
			/*put(ChessCommand.class, new Argument(
					"xadrez", new String[]{"chess"}, REQ_MENTION, "cmd_chess", FUN, true
			));*/
			put(HitotsuCommand.class, new Argument(
					"hitotsu", new String[]{"uno"}, "req_bet-mentions", "cmd_hitotsu", FUN, true
			));
			put(ReversiCommand.class, new Argument(
					"reversi", new String[]{"othello"}, REQ_MENTION_BET, "cmd_reversi", FUN, true
			));
			put(ShoukanCommand.class, new Argument(
					"shoukan", new String[]{"duelcards"}, "req_shoukan-args", "cmd_shoukan", FUN, true
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
					"adivinheascartas", "cmd_guess-the-cards", FUN, false
			));

			/*
			put(DisboardCommand.class, new Arguments(
					"disboard", new String[]{"exmap", "mapa"}, "cmd_disboard", EXCEED, false
			));
			*/
		}
	};

	public Set<PreparedCommand> getCommands() {
		Reflections refl = new Reflections(this.getClass().getPackageName());
		Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);
		Set<PreparedCommand> commands = new HashSet<>();

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			extractCommand(commands, cmd, params);
		}

		return commands;
	}

	public Set<PreparedCommand> getCommands(Category category) {
		Reflections refl = new Reflections(this.getClass().getPackageName());
		Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);
		Set<PreparedCommand> commands = new HashSet<>();

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (params.category() == category) {
				extractCommand(commands, cmd, params);
			}
		}

		return commands;
	}

	public PreparedCommand getCommand(String name) {
		Reflections refl = new Reflections(this.getClass().getPackageName());
		Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (name.equalsIgnoreCase(params.name()) || Helper.equalsAny(name, params.aliases())) {
				Requires req = cmd.getDeclaredAnnotation(Requires.class);
				return new PreparedCommand(
						params.name(),
						params.aliases(),
						params.usage(),
						"cmd_" + cmd.getSimpleName()
								.replaceAll("Command|Reaction", "")
								.replaceAll("[a-z](?=[A-Z])", "$0-")
								.toLowerCase(),
						params.category(),
						req == null ? null : req.value()
				);
			}
		}

		return null;
	}

	private void extractCommand(Set<PreparedCommand> commands, Class<?> cmd, Command params) {
		Requires req = cmd.getDeclaredAnnotation(Requires.class);
		commands.add(new PreparedCommand(
				params.name(),
				params.aliases(),
				params.usage(),
				"cmd_" + cmd.getSimpleName()
						.replaceAll("Command|Reaction", "")
						.replaceAll("[a-z](?=[A-Z])", "$0-")
						.toLowerCase(),
				params.category(),
				req == null ? null : req.value()
		));
	}
}
