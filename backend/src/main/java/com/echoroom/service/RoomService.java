package com.echoroom.service;

import com.echoroom.dto.RoomDtos.CreateRoomRequest;
import com.echoroom.dto.RoomDtos.RoomDetail;
import com.echoroom.dto.RoomDtos.RoomSummary;
import com.echoroom.dto.WsDtos.RoomEvent;
import com.echoroom.exception.ApiExceptions.BadRequestException;
import com.echoroom.exception.ApiExceptions.ForbiddenException;
import com.echoroom.exception.ApiExceptions.NotFoundException;
import com.echoroom.model.Room;
import com.echoroom.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no ambiguous chars
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RoomRepository roomRepository;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomSummary createRoom(String ownerUsername, CreateRoomRequest req) {
        String code = generateUniqueRoomCode();
        Room room = Room.builder()
                .roomCode(code)
                .name(req.getName())
                .description(req.getDescription())
                .ownerUsername(ownerUsername)
                .status(Room.RoomStatus.ACTIVE)
                .members(new java.util.ArrayList<>(List.of(ownerUsername)))
                .createdAt(Instant.now())
                .lastActivityAt(Instant.now())
                .build();
        room = roomRepository.save(room);
        chatService.postSystemMessage(room.getId(), ownerUsername + " created the room.");
        return toSummary(room);
    }

    /** Registered user requests to join by room code. Owner must approve. */
    public void requestJoin(String username, String roomCode) {
        Room room = requireByCode(roomCode);
        assertActive(room);

        if (room.getMembers().contains(username)) {
            throw new BadRequestException("You are already a member of this room");
        }
        boolean alreadyPending = room.getPendingRequests().stream()
                .anyMatch(r -> r.getUsername().equals(username));
        if (alreadyPending) {
            throw new BadRequestException("Your join request is already pending approval");
        }

        room.getPendingRequests().add(Room.JoinRequest.builder().username(username).build());
        roomRepository.save(room);

        // Notify the owner in real time
        messagingTemplate.convertAndSendToUser(room.getOwnerUsername(), "/queue/room-events",
                RoomEvent.builder().type("JOIN_REQUEST").roomId(room.getId()).username(username)
                        .message(username + " requested to join \"" + room.getName() + "\"").build());
    }

    public RoomSummary approveJoin(String ownerUsername, String roomId, String username) {
        Room room = requireById(roomId);
        assertOwner(room, ownerUsername);

        boolean removed = room.getPendingRequests().removeIf(r -> r.getUsername().equals(username));
        if (!removed) {
            throw new BadRequestException("No pending join request from " + username);
        }
        room.getMembers().add(username);
        room.setLastActivityAt(Instant.now());
        roomRepository.save(room);

        chatService.postSystemMessage(room.getId(), username + " joined the room.");
        messagingTemplate.convertAndSendToUser(username, "/queue/room-events",
                RoomEvent.builder().type("JOIN_APPROVED").roomId(room.getId())
                        .message("Your request to join \"" + room.getName() + "\" was approved.").build());
        broadcastRoomUpdate(room);
        return toSummary(room);
    }

    public void denyJoin(String ownerUsername, String roomId, String username) {
        Room room = requireById(roomId);
        assertOwner(room, ownerUsername);

        boolean removed = room.getPendingRequests().removeIf(r -> r.getUsername().equals(username));
        if (!removed) {
            throw new BadRequestException("No pending join request from " + username);
        }
        roomRepository.save(room);

        messagingTemplate.convertAndSendToUser(username, "/queue/room-events",
                RoomEvent.builder().type("JOIN_DENIED").roomId(room.getId())
                        .message("Your request to join \"" + room.getName() + "\" was denied.").build());
    }

    public void removeMember(String ownerUsername, String roomId, String username) {
        Room room = requireById(roomId);
        assertOwner(room, ownerUsername);
        if (username.equals(ownerUsername)) {
            throw new BadRequestException("Owner cannot remove themselves; close the room instead");
        }
        boolean removed = room.getMembers().remove(username);
        room.getOnlineMembers().remove(username);
        if (!removed) {
            throw new BadRequestException(username + " is not a member of this room");
        }
        roomRepository.save(room);

        chatService.postSystemMessage(room.getId(), username + " was removed by the room owner.");
        messagingTemplate.convertAndSendToUser(username, "/queue/room-events",
                RoomEvent.builder().type("MEMBER_REMOVED").roomId(room.getId())
                        .message("You were removed from \"" + room.getName() + "\".").build());
        broadcastRoomUpdate(room);
    }

    public List<RoomSummary> myRooms(String username) {
        return roomRepository.findByMembersContaining(username).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    public RoomDetail roomDetail(String username, String roomId) {
        Room room = requireById(roomId);
        assertMember(room, username);
        return toDetail(room);
    }

    public void markOnline(String roomId, String username, boolean online) {
        Room room = requireById(roomId);
        if (!room.getMembers().contains(username)) return;
        if (online) {
            if (!room.getOnlineMembers().contains(username)) room.getOnlineMembers().add(username);
        } else {
            room.getOnlineMembers().remove(username);
        }
        roomRepository.save(room);
    }

    public Room requireById(String roomId) {
        return roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
    }

    public Room requireByCode(String roomCode) {
        return roomRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new NotFoundException("No room with code " + roomCode));
    }

    public void assertMember(Room room, String username) {
        if (!room.getMembers().contains(username)) {
            throw new ForbiddenException("You are not a member of this room");
        }
    }

    public void assertOwner(Room room, String username) {
        if (!room.getOwnerUsername().equals(username)) {
            throw new ForbiddenException("Only the room owner can perform this action");
        }
    }

    private void assertActive(Room room) {
        if (room.getStatus() != Room.RoomStatus.ACTIVE) {
            throw new BadRequestException("This room has been closed");
        }
    }

    private void broadcastRoomUpdate(Room room) {
        messagingTemplate.convertAndSend("/topic/room/" + room.getId() + "/roster", toDetail(room));
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (roomRepository.existsByRoomCode(code));
        return code;
    }

    public RoomSummary toSummary(Room room) {
        return RoomSummary.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .name(room.getName())
                .description(room.getDescription())
                .ownerUsername(room.getOwnerUsername())
                .status(room.getStatus().name())
                .memberCount(room.getMembers().size())
                .onlineCount(room.getOnlineMembers().size())
                .pendingCount(room.getPendingRequests().size())
                .createdAt(room.getCreatedAt())
                .lastActivityAt(room.getLastActivityAt())
                .build();
    }

    public RoomDetail toDetail(Room room) {
        return RoomDetail.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .name(room.getName())
                .description(room.getDescription())
                .ownerUsername(room.getOwnerUsername())
                .status(room.getStatus().name())
                .members(room.getMembers())
                .onlineMembers(room.getOnlineMembers())
                .pendingUsernames(room.getPendingRequests().stream().map(Room.JoinRequest::getUsername).collect(Collectors.toList()))
                .messages(chatService.toDtoList(room.getMessages()))
                .build();
    }
}
