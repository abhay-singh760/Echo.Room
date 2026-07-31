package com.echoroom.controller;

import com.echoroom.dto.MessageDto;
import com.echoroom.dto.WsDtos.*;
import com.echoroom.service.ChatService;
import com.echoroom.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * All STOMP handlers trust only java.security.Principal, which
 * StompAuthChannelInterceptor derived from the verified JWT -- never a
 * username field the client places in the message payload.
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/{roomId}/send")
    public void send(@DestinationVariable String roomId, @Payload ChatMessageRequest req, Principal principal) {
        MessageDto saved = chatService.postMessage(roomId, principal.getName(), req);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, saved);
    }

    @MessageMapping("/room/{roomId}/edit")
    public void edit(@DestinationVariable String roomId, @Payload EditMessageRequest req, Principal principal) {
        MessageDto updated = chatService.editMessage(roomId, principal.getName(), req.getMessageId(), req.getNewContent());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, updated);
    }

    @MessageMapping("/room/{roomId}/delete")
    public void delete(@DestinationVariable String roomId, @Payload DeleteMessageRequest req, Principal principal) {
        MessageDto updated = chatService.deleteMessage(roomId, principal.getName(), req.getMessageId());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, updated);
    }

    @MessageMapping("/room/{roomId}/react")
    public void react(@DestinationVariable String roomId, @Payload ReactionRequest req, Principal principal) {
        MessageDto updated = chatService.toggleReaction(roomId, principal.getName(), req.getMessageId(), req.getEmoji());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, updated);
    }

    @MessageMapping("/room/{roomId}/read")
    public void read(@DestinationVariable String roomId, @Payload ReadReceiptRequest req, Principal principal) {
        MessageDto updated = chatService.markRead(roomId, principal.getName(), req.getMessageId());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, updated);
    }

    @MessageMapping("/room/{roomId}/typing")
    public void typing(@DestinationVariable String roomId, @Payload TypingEvent event, Principal principal) {
        TypingEvent verified = new TypingEvent(roomId, principal.getName(), event.isTyping());
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/typing", verified);
    }

    @MessageMapping("/room/{roomId}/presence")
    public void presence(@DestinationVariable String roomId, @Payload PresenceEvent event, Principal principal) {
        roomService.markOnline(roomId, principal.getName(), event.isOnline());
        PresenceEvent verified = PresenceEvent.builder()
                .roomId(roomId).username(principal.getName()).online(event.isOnline()).build();
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/presence", verified);
    }
}
