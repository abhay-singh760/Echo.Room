package com.echoroom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

public class RoomDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRoomRequest {
        @NotBlank
        private String name;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinRoomRequest {
        @NotBlank
        private String roomCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomSummary {
        private String id;
        private String roomCode;
        private String name;
        private String description;
        private String ownerUsername;
        private String status;
        private int memberCount;
        private int onlineCount;
        private int pendingCount;
        private Instant createdAt;
        private Instant lastActivityAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomDetail {
        private String id;
        private String roomCode;
        private String name;
        private String description;
        private String ownerUsername;
        private String status;
        private List<String> members;
        private List<String> onlineMembers;
        private List<String> pendingUsernames;
        private List<MessageDto> messages;
    }
}
