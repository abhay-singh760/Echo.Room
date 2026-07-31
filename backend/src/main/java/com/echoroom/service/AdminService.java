package com.echoroom.service;

import com.echoroom.dto.RoomDtos.RoomSummary;
import com.echoroom.exception.ApiExceptions.NotFoundException;
import com.echoroom.model.Room;
import com.echoroom.repository.RoomRepository;
import com.echoroom.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int INACTIVITY_DAYS_HINT = 7;

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public DashboardStats stats() {
        List<Room> rooms = roomRepository.findAll();
        long active = rooms.stream().filter(r -> r.getStatus() == Room.RoomStatus.ACTIVE).count();
        long closed = rooms.size() - active;
        long totalMessages = rooms.stream().mapToLong(r -> r.getMessages().size()).sum();
        long inactiveCandidates = rooms.stream()
                .filter(r -> r.getStatus() == Room.RoomStatus.ACTIVE)
                .filter(this::looksInactive)
                .count();

        return DashboardStats.builder()
                .totalUsers(userRepository.count())
                .totalRooms(rooms.size())
                .activeRooms(active)
                .closedRooms(closed)
                .totalMessages(totalMessages)
                .inactiveRoomCandidates(inactiveCandidates)
                .build();
    }

    public List<RoomSummary> allRooms() {
        return roomRepository.findAll().stream()
                .map(roomService::toSummary)
                .collect(Collectors.toList());
    }

    public RoomSummary closeRoom(String roomId, String reason) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        room.setStatus(Room.RoomStatus.CLOSED);
        roomRepository.save(room);

        messagingTemplate.convertAndSend("/topic/room/" + room.getId(),
                com.echoroom.dto.WsDtos.RoomEvent.builder()
                        .type("ROOM_CLOSED")
                        .roomId(room.getId())
                        .message("This room was closed by an administrator" + (reason != null && !reason.isBlank() ? ": " + reason : "."))
                        .build());
        return roomService.toSummary(room);
    }

    private boolean looksInactive(Room room) {
        Instant reference = room.getLastActivityAt() != null ? room.getLastActivityAt() : room.getCreatedAt();
        return reference != null && reference.isBefore(Instant.now().minus(INACTIVITY_DAYS_HINT, ChronoUnit.DAYS));
    }

    @Data
    @Builder
    public static class DashboardStats {
        private long totalUsers;
        private int totalRooms;
        private long activeRooms;
        private long closedRooms;
        private long totalMessages;
        private long inactiveRoomCandidates;
    }
}
