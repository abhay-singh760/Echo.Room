package com.echoroom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String roomId;
    private String senderUsername;

    /** TEXT, IMAGE, FILE, SYSTEM */
    private MessageType type;

    private String content;

    /** Populated for IMAGE / FILE messages */
    private String attachmentUrl;
    private String attachmentName;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private boolean edited = false;

    @Builder.Default
    private boolean deleted = false;

    /** emoji -> set of usernames who reacted */
    @Builder.Default
    private Map<String, Set<String>> reactions = new HashMap<>();

    /** usernames who have read this message */
    @Builder.Default
    private Set<String> readBy = new HashSet<>();

    public enum MessageType {
        TEXT, IMAGE, FILE, SYSTEM
    }
}
