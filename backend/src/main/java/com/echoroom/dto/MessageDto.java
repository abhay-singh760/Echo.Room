package com.echoroom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private String id;
    private String roomId;
    private String senderUsername;
    private String type;
    private String content;
    private String attachmentUrl;
    private String attachmentName;
    private Instant timestamp;
    private boolean edited;
    private boolean deleted;
    private Map<String, Set<String>> reactions;
    private Set<String> readBy;
}
