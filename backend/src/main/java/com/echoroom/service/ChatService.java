package com.echoroom.service;

import com.echoroom.dto.MessageDto;
import com.echoroom.dto.WsDtos.ChatMessageRequest;
import com.echoroom.exception.ApiExceptions.BadRequestException;
import com.echoroom.exception.ApiExceptions.ForbiddenException;
import com.echoroom.exception.ApiExceptions.NotFoundException;
import com.echoroom.model.Message;
import com.echoroom.model.Room;
import com.echoroom.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_HISTORY = 500;

    private final RoomRepository roomRepository;
    private final MongoTemplate mongoTemplate;

    public MessageDto postMessage(String roomId, String senderUsername, ChatMessageRequest req) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        if (!room.getMembers().contains(senderUsername)) {
            throw new ForbiddenException("You are not a member of this room");
        }
        if (room.getStatus() != Room.RoomStatus.ACTIVE) {
            throw new BadRequestException("This room has been closed");
        }

        Message.MessageType type = parseType(req.getType());
        if (type == Message.MessageType.TEXT && (req.getContent() == null || req.getContent().isBlank())) {
            throw new BadRequestException("Message content cannot be empty");
        }

        Message message = Message.builder()
                .roomId(roomId)
                .senderUsername(senderUsername)
                .type(type)
                .content(req.getContent())
                .attachmentUrl(req.getAttachmentUrl())
                .attachmentName(req.getAttachmentName())
                .timestamp(Instant.now())
                .build();

        room.getMessages().add(message);
        trimHistory(room);
        room.setLastActivityAt(Instant.now());
        roomRepository.save(room);

        return toDto(message);
    }

    public MessageDto postSystemMessage(String roomId, String text) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        Message message = Message.builder()
                .roomId(roomId)
                .senderUsername("system")
                .type(Message.MessageType.SYSTEM)
                .content(text)
                .timestamp(Instant.now())
                .build();
        room.getMessages().add(message);
        room.setLastActivityAt(Instant.now());
        roomRepository.save(room);
        return toDto(message);
    }

    public MessageDto editMessage(String roomId, String editorUsername, String messageId, String newContent) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        Message target = findMessage(room, messageId);

        if (!target.getSenderUsername().equals(editorUsername)) {
            throw new ForbiddenException("You can only edit your own messages");
        }
        if (target.isDeleted()) {
            throw new BadRequestException("Cannot edit a deleted message");
        }
        if (newContent == null || newContent.isBlank()) {
            throw new BadRequestException("Message content cannot be empty");
        }

        target.setContent(newContent);
        target.setEdited(true);
        roomRepository.save(room);
        return toDto(target);
    }

    public MessageDto deleteMessage(String roomId, String requesterUsername, String messageId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        Message target = findMessage(room, messageId);

        boolean isSender = target.getSenderUsername().equals(requesterUsername);
        boolean isOwner = room.getOwnerUsername().equals(requesterUsername);
        if (!isSender && !isOwner) {
            throw new ForbiddenException("Only the sender or the room owner can delete this message");
        }

        target.setDeleted(true);
        target.setContent(null);
        target.setAttachmentUrl(null);
        roomRepository.save(room);
        return toDto(target);
    }

    public MessageDto toggleReaction(String roomId, String username, String messageId, String emoji) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        Message target = findMessage(room, messageId);

        Set<String> reactors = target.getReactions().computeIfAbsent(emoji, k -> new java.util.HashSet<>());
        if (!reactors.add(username)) {
            reactors.remove(username);
            if (reactors.isEmpty()) target.getReactions().remove(emoji);
        }
        roomRepository.save(room);
        return toDto(target);
    }

    public MessageDto markRead(String roomId, String username, String messageId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        Message target = findMessage(room, messageId);
        target.getReadBy().add(username);
        roomRepository.save(room);
        return toDto(target);
    }

    private Message findMessage(Room room, String messageId) {
        return room.getMessages().stream()
                .filter(m -> m.getId().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Message not found"));
    }

    private void trimHistory(Room room) {
        List<Message> messages = room.getMessages();
        if (messages.size() > MAX_HISTORY) {
            room.setMessages(new java.util.ArrayList<>(
                    messages.subList(messages.size() - MAX_HISTORY, messages.size())));
        }
    }

    private Message.MessageType parseType(String raw) {
        if (raw == null) return Message.MessageType.TEXT;
        try {
            return Message.MessageType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unsupported message type: " + raw);
        }
    }

    public MessageDto toDto(Message m) {
        return MessageDto.builder()
                .id(m.getId())
                .roomId(m.getRoomId())
                .senderUsername(m.getSenderUsername())
                .type(m.getType().name())
                .content(m.getContent())
                .attachmentUrl(m.getAttachmentUrl())
                .attachmentName(m.getAttachmentName())
                .timestamp(m.getTimestamp())
                .edited(m.isEdited())
                .deleted(m.isDeleted())
                .reactions(m.getReactions())
                .readBy(m.getReadBy())
                .build();
    }

    public List<MessageDto> toDtoList(List<Message> messages) {
        return messages.stream().map(this::toDto).collect(Collectors.toList());
    }
}
