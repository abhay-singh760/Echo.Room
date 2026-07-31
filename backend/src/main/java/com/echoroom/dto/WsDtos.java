package com.echoroom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payloads exchanged over the WebSocket/STOMP channel. */
public class WsDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageRequest {
        private String roomId;
        private String content;
        /** TEXT, IMAGE, FILE */
        private String type;
        private String attachmentUrl;
        private String attachmentName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditMessageRequest {
        private String roomId;
        private String messageId;
        private String newContent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteMessageRequest {
        private String roomId;
        private String messageId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionRequest {
        private String roomId;
        private String messageId;
        private String emoji;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypingEvent {
        private String roomId;
        private String username;
        private boolean typing;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadReceiptRequest {
        private String roomId;
        private String messageId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresenceEvent {
        private String roomId;
        private String username;
        private boolean online;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomEvent {
        /** JOIN_REQUEST, JOIN_APPROVED, JOIN_DENIED, MEMBER_REMOVED, ROOM_CLOSED */
        private String type;
        private String roomId;
        private String username;
        private String message;
    }
}
