package com.echoroom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    private String id;

    /** Human-friendly room code used to join, e.g. "ABC123" */
    @Indexed(unique = true)
    private String roomCode;

    private String name;
    private String description;
    private String ownerUsername;

    @Builder.Default
    private RoomStatus status = RoomStatus.ACTIVE;

    /** approved members */
    @Builder.Default
    private List<String> members = new ArrayList<>();

    /** users waiting for owner approval */
    @Builder.Default
    private List<JoinRequest> pendingRequests = new ArrayList<>();

    /** currently connected/online members of this room */
    @Builder.Default
    private List<String> onlineMembers = new ArrayList<>();

    /** messages embedded in the room document (fine for moderate-volume chat rooms) */
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant lastActivityAt;

    public enum RoomStatus {
        ACTIVE, CLOSED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinRequest {
        private String username;
        @Builder.Default
        private Instant requestedAt = Instant.now();
    }
}
