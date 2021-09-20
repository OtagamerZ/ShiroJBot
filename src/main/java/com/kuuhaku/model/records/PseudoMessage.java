/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.records;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ComponentLayout;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;
import net.dv8tion.jda.api.utils.AttachmentOption;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record PseudoMessage(
		String content, User author,
		Member member,
		TextChannel channel,
		List<User> mentionedUsers,
		List<Member> mentionedMembers,
		List<Role> mentionedRoles,
		List<TextChannel> mentionedChannels
) implements Message {

	@Nullable
	@Override
	public MessageReference getMessageReference() {
		return null;
	}

	@Nullable
	@Override
	public Message getReferencedMessage() {
		return null;
	}

	@NotNull
	@Override
	public List<User> getMentionedUsers() {
		return mentionedUsers;
	}

	@NotNull
	@Override
	public Bag<User> getMentionedUsersBag() {
		return new HashBag<>(mentionedUsers);
	}

	@NotNull
	@Override
	public List<TextChannel> getMentionedChannels() {
		return mentionedChannels;
	}

	@NotNull
	@Override
	public Bag<TextChannel> getMentionedChannelsBag() {
		return new HashBag<>(mentionedChannels);
	}

	@NotNull
	@Override
	public List<Role> getMentionedRoles() {
		return mentionedRoles;
	}

	@NotNull
	@Override
	public Bag<Role> getMentionedRolesBag() {
		return new HashBag<>(mentionedRoles);
	}

	@NotNull
	@Override
	public List<Member> getMentionedMembers(@NotNull Guild guild) {
		return mentionedMembers.stream()
				.filter(m -> m.getGuild().getId().equals(guild.getId()))
				.toList();
	}

	@NotNull
	@Override
	public List<Member> getMentionedMembers() {
		return mentionedMembers;
	}

	@NotNull
	@Override
	public List<IMentionable> getMentions(@NotNull MentionType @NotNull ... types) {
		return Stream.of(mentionedUsers, mentionedMembers, mentionedRoles, mentionedChannels)
				.flatMap(List::stream)
				.filter(m -> types.length == 0 || Stream.of(types).anyMatch(t -> t.getPattern().matcher(m.getAsMention()).matches()))
				.collect(Collectors.toList());
	}

	@Override
	public boolean isMentioned(@NotNull IMentionable mentionable, @NotNull MentionType @NotNull ... types) {
		return Stream.of(mentionedUsers, mentionedMembers, mentionedRoles, mentionedChannels)
				.flatMap(List::stream)
				.filter(m -> m.getId().equals(mentionable.getId()))
				.anyMatch(m -> types.length == 0 || Stream.of(types).anyMatch(t -> t.getPattern().matcher(m.getAsMention()).matches()));
	}

	@Override
	public boolean mentionsEveryone() {
		return Stream.of(mentionedUsers, mentionedMembers, mentionedRoles, mentionedChannels)
				.flatMap(List::stream)
				.anyMatch(m -> MentionType.EVERYONE.getPattern().matcher(m.getAsMention()).matches() || MentionType.HERE.getPattern().matcher(m.getAsMention()).matches());
	}

	@Override
	public boolean isEdited() {
		return false;
	}

	@Nullable
	@Override
	public OffsetDateTime getTimeEdited() {
		return null;
	}

	@NotNull
	@Override
	public User getAuthor() {
		return author;
	}

	@Nullable
	@Override
	public Member getMember() {
		return member;
	}

	@NotNull
	@Override
	public String getJumpUrl() {
		return "";
	}

	@NotNull
	@Override
	public String getContentDisplay() {
		return content;
	}

	@NotNull
	@Override
	public String getContentRaw() {
		return content;
	}

	@NotNull
	@Override
	public String getContentStripped() {
		return content;
	}

	@NotNull
	@Override
	public List<String> getInvites() {
		return List.of();
	}

	@Nullable
	@Override
	public String getNonce() {
		return null;
	}

	@Override
	public boolean isFromType(@NotNull ChannelType type) {
		return type == ChannelType.TEXT;
	}

	@Override
	public boolean isFromGuild() {
		return true;
	}

	@NotNull
	@Override
	public ChannelType getChannelType() {
		return ChannelType.TEXT;
	}

	@Override
	public boolean isWebhookMessage() {
		return false;
	}

	@NotNull
	@Override
	public MessageChannel getChannel() {
		return channel;
	}

	@NotNull
	@Override
	public PrivateChannel getPrivateChannel() {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public TextChannel getTextChannel() {
		return channel;
	}

	@Nullable
	@Override
	public Category getCategory() {
		return channel.getParent();
	}

	@NotNull
	@Override
	public Guild getGuild() {
		return channel.getGuild();
	}

	@NotNull
	@Override
	public List<Attachment> getAttachments() {
		return List.of();
	}

	@NotNull
	@Override
	public List<MessageEmbed> getEmbeds() {
		return List.of();
	}

	@NotNull
	@Override
	public List<ActionRow> getActionRows() {
		return List.of();
	}

	@NotNull
	@Override
	public List<Button> getButtons() {
		return List.of();
	}

	@Nullable
	@Override
	public Button getButtonById(@NotNull String id) {
		return null;
	}

	@NotNull
	@Override
	public List<Button> getButtonsByLabel(@NotNull String label, boolean ignoreCase) {
		return List.of();
	}

	@NotNull
	@Override
	public List<Emote> getEmotes() {
		return List.of();
	}

	@NotNull
	@Override
	public Bag<Emote> getEmotesBag() {
		return new HashBag<>();
	}

	@NotNull
	@Override
	public List<MessageReaction> getReactions() {
		return List.of();
	}

	@NotNull
	@Override
	public List<MessageSticker> getStickers() {
		return List.of();
	}

	@Override
	public boolean isTTS() {
		return false;
	}

	@Nullable
	@Override
	public MessageActivity getActivity() {
		return null;
	}

	@NotNull
	@Override
	public MessageAction editMessage(@NotNull CharSequence newContent) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction editMessageEmbeds(@NotNull Collection<? extends MessageEmbed> embeds) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction editMessageEmbeds(@NotNull MessageEmbed @NotNull ... embeds) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction editMessageComponents(@NotNull Collection<? extends ComponentLayout> components) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction editMessageComponents(@NotNull ComponentLayout @NotNull ... components) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction editMessageFormat(@NotNull String format, @NotNull Object @NotNull ... args) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction editMessage(@NotNull Message newContent) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction reply(@NotNull CharSequence content) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction replyEmbeds(@NotNull MessageEmbed embed, @NotNull MessageEmbed @NotNull ... other) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction replyEmbeds(@NotNull Collection<? extends MessageEmbed> embeds) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction reply(@NotNull Message content) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction replyFormat(@NotNull String format, @NotNull Object @NotNull ... args) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction reply(@NotNull File file, @NotNull AttachmentOption @NotNull ... options) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction reply(@NotNull File data, @NotNull String name, @NotNull AttachmentOption @NotNull ... options) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction reply(@NotNull InputStream data, @NotNull String name, @NotNull AttachmentOption @NotNull ... options) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public MessageAction reply(byte @NotNull [] data, @NotNull String name, @NotNull AttachmentOption @NotNull ... options) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public AuditableRestAction<Void> delete() {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public JDA getJDA() {
		return author.getJDA();
	}

	@Override
	public boolean isPinned() {
		return false;
	}

	@NotNull
	@Override
	public RestAction<Void> pin() {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public RestAction<Void> unpin() {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public RestAction<Void> addReaction(@NotNull Emote emote) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public RestAction<Void> addReaction(@NotNull String unicode) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public RestAction<Void> clearReactions() {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public RestAction<Void> clearReactions(@NotNull String unicode) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public RestAction<Void> clearReactions(@NotNull Emote emote) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public RestAction<Void> removeReaction(@NotNull Emote emote) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public RestAction<Void> removeReaction(@NotNull Emote emote, @NotNull User user) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public RestAction<Void> removeReaction(@NotNull String unicode) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public RestAction<Void> removeReaction(@NotNull String unicode, @NotNull User user) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public ReactionPaginationAction retrieveReactionUsers(@NotNull Emote emote) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public ReactionPaginationAction retrieveReactionUsers(@NotNull String unicode) {
		throw new IllegalStateException();
	}

	@Nullable
	@Override
	public MessageReaction.ReactionEmote getReactionByUnicode(@NotNull String unicode) {
		return null;
	}

	@Nullable
	@Override
	public MessageReaction.ReactionEmote getReactionById(@NotNull String id) {
		return null;
	}

	@Nullable
	@Override
	public MessageReaction.ReactionEmote getReactionById(long id) {
		return null;
	}

	@NotNull
	@Override
	public AuditableRestAction<Void> suppressEmbeds(boolean suppressed) {
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public RestAction<Message> crosspost() {
		throw new IllegalStateException();
	}

	@Override
	public boolean isSuppressedEmbeds() {
		return false;
	}

	@NotNull
	@Override
	public EnumSet<MessageFlag> getFlags() {
		return EnumSet.noneOf(MessageFlag.class);
	}

	@Override
	public long getFlagsRaw() {
		return 0;
	}

	@Override
	public boolean isEphemeral() {
		return false;
	}

	@NotNull
	@Override
	public MessageType getType() {
		return MessageType.APPLICATION_COMMAND;
	}

	@Nullable
	@Override
	public Interaction getInteraction() {
		return null;
	}

	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision) {

	}

	@Override
	public long getIdLong() {
		return 0;
	}
}
