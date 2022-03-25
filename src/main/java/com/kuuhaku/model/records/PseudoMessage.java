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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

	@Nonnull
	@Override
	public List<User> getMentionedUsers() {
		return mentionedUsers;
	}

	@Nonnull
	@Override
	public Bag<User> getMentionedUsersBag() {
		return new HashBag<>(mentionedUsers);
	}

	@Nonnull
	@Override
	public List<TextChannel> getMentionedChannels() {
		return mentionedChannels;
	}

	@Nonnull
	@Override
	public Bag<TextChannel> getMentionedChannelsBag() {
		return new HashBag<>(mentionedChannels);
	}

	@Nonnull
	@Override
	public List<Role> getMentionedRoles() {
		return mentionedRoles;
	}

	@Nonnull
	@Override
	public Bag<Role> getMentionedRolesBag() {
		return new HashBag<>(mentionedRoles);
	}

	@Nonnull
	@Override
	public List<Member> getMentionedMembers(@Nonnull Guild guild) {
		return mentionedMembers.stream()
				.filter(m -> m.getGuild().getId().equals(guild.getId()))
				.toList();
	}

	@Nonnull
	@Override
	public List<Member> getMentionedMembers() {
		return mentionedMembers;
	}

	@Nonnull
	@Override
	public List<IMentionable> getMentions(@Nonnull MentionType... types) {
		return Stream.of(mentionedUsers, mentionedMembers, mentionedRoles, mentionedChannels)
				.flatMap(List::stream)
				.filter(m -> types.length == 0 || Stream.of(types).anyMatch(t -> t.getPattern().matcher(m.getAsMention()).matches()))
				.collect(Collectors.toList());
	}

	@Override
	public boolean isMentioned(@Nonnull IMentionable mentionable, @Nonnull MentionType... types) {
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

	@Nonnull
	@Override
	public User getAuthor() {
		return author;
	}

	@Nullable
	@Override
	public Member getMember() {
		return member;
	}

	@Nonnull
	@Override
	public String getJumpUrl() {
		return "";
	}

	@Nonnull
	@Override
	public String getContentDisplay() {
		return content;
	}

	@Nonnull
	@Override
	public String getContentRaw() {
		return content;
	}

	@Nonnull
	@Override
	public String getContentStripped() {
		return content;
	}

	@Nonnull
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
	public boolean isFromType(@Nonnull ChannelType type) {
		return type == ChannelType.TEXT;
	}

	@Override
	public boolean isFromGuild() {
		return true;
	}

	@Nonnull
	@Override
	public ChannelType getChannelType() {
		return ChannelType.TEXT;
	}

	@Override
	public boolean isWebhookMessage() {
		return false;
	}

	@Nonnull
	@Override
	public MessageChannel getChannel() {
		return channel;
	}

	@Nonnull
	@Override
	public PrivateChannel getPrivateChannel() {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public TextChannel getTextChannel() {
		return channel;
	}

	@Nullable
	@Override
	public Category getCategory() {
		return channel.getParent();
	}

	@Nonnull
	@Override
	public Guild getGuild() {
		return channel.getGuild();
	}

	@Nonnull
	@Override
	public List<Attachment> getAttachments() {
		return List.of();
	}

	@Nonnull
	@Override
	public List<MessageEmbed> getEmbeds() {
		return List.of();
	}

	@Nonnull
	@Override
	public List<ActionRow> getActionRows() {
		return List.of();
	}

	@Nonnull
	@Override
	public List<Button> getButtons() {
		return List.of();
	}

	@Nullable
	@Override
	public Button getButtonById(@Nonnull String id) {
		return null;
	}

	@Nonnull
	@Override
	public List<Button> getButtonsByLabel(@Nonnull String label, boolean ignoreCase) {
		return List.of();
	}

	@Nonnull
	@Override
	public List<Emote> getEmotes() {
		return List.of();
	}

	@Nonnull
	@Override
	public Bag<Emote> getEmotesBag() {
		return new HashBag<>();
	}

	@Nonnull
	@Override
	public List<MessageReaction> getReactions() {
		return List.of();
	}

	@Nonnull
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

	@Nonnull
	@Override
	public MessageAction editMessage(@Nonnull CharSequence newContent) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction editMessageEmbeds(@Nonnull Collection<? extends MessageEmbed> embeds) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction editMessageEmbeds(@Nonnull MessageEmbed... embeds) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction editMessageComponents(@Nonnull Collection<? extends ComponentLayout> components) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction editMessageComponents(@Nonnull ComponentLayout... components) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction editMessageFormat(@Nonnull String format, @Nonnull Object... args) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction editMessage(@Nonnull Message newContent) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction reply(@Nonnull CharSequence content) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction replyEmbeds(@Nonnull MessageEmbed embed, @Nonnull MessageEmbed... other) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction replyEmbeds(@Nonnull Collection<? extends MessageEmbed> embeds) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction reply(@Nonnull Message content) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction replyFormat(@Nonnull String format, @Nonnull Object ... args) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction reply(@Nonnull File file, @Nonnull AttachmentOption ... options) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction reply(@Nonnull File data, @Nonnull String name, @Nonnull AttachmentOption... options) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction reply(@Nonnull InputStream data, @Nonnull String name, @Nonnull AttachmentOption... options) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public MessageAction reply(@Nonnull byte[] data, @Nonnull String name, @Nonnull AttachmentOption... options) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public AuditableRestAction<Void> delete() {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public JDA getJDA() {
		return author.getJDA();
	}

	@Override
	public boolean isPinned() {
		return false;
	}

	@Nonnull
	@Override
	public RestAction<Void> pin() {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public RestAction<Void> unpin() {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public RestAction<Void> addReaction(@Nonnull Emote emote) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public RestAction<Void> addReaction(@Nonnull String unicode) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public RestAction<Void> clearReactions() {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public RestAction<Void> clearReactions(@Nonnull String unicode) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public RestAction<Void> clearReactions(@Nonnull Emote emote) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public RestAction<Void> removeReaction(@Nonnull Emote emote) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public RestAction<Void> removeReaction(@Nonnull Emote emote, @Nonnull User user) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public RestAction<Void> removeReaction(@Nonnull String unicode) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public RestAction<Void> removeReaction(@Nonnull String unicode, @Nonnull User user) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public ReactionPaginationAction retrieveReactionUsers(@Nonnull Emote emote) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public ReactionPaginationAction retrieveReactionUsers(@Nonnull String unicode) {
		throw new IllegalStateException();
	}

	@Nullable
	@Override
	public MessageReaction.ReactionEmote getReactionByUnicode(@Nonnull String unicode) {
		return null;
	}

	@Nullable
	@Override
	public MessageReaction.ReactionEmote getReactionById(@Nonnull String id) {
		return null;
	}

	@Nullable
	@Override
	public MessageReaction.ReactionEmote getReactionById(long id) {
		return null;
	}

	@Nonnull
	@Override
	public AuditableRestAction<Void> suppressEmbeds(boolean suppressed) {
		throw new IllegalStateException();
	}

	@Nonnull
	@Override
	public RestAction<Message> crosspost() {
		throw new IllegalStateException();
	}

	@Override
	public boolean isSuppressedEmbeds() {
		return false;
	}

	@Nonnull
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

	@Nonnull
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
