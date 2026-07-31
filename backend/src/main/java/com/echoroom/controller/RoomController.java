package com.echoroom.controller;

import com.echoroom.dto.RoomDtos.CreateRoomRequest;
import com.echoroom.dto.RoomDtos.JoinRoomRequest;
import com.echoroom.dto.RoomDtos.RoomDetail;
import com.echoroom.dto.RoomDtos.RoomSummary;
import com.echoroom.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomSummary> createRoom(Authentication auth, @Valid @RequestBody CreateRoomRequest req) {
        return ResponseEntity.ok(roomService.createRoom(auth.getName(), req));
    }

    @PostMapping("/join")
    public ResponseEntity<Map<String, String>> requestJoin(Authentication auth, @Valid @RequestBody JoinRoomRequest req) {
        roomService.requestJoin(auth.getName(), req.getRoomCode());
        return ResponseEntity.ok(Map.of("message", "Join request sent. Waiting for owner approval."));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<RoomSummary>> myRooms(Authentication auth) {
        return ResponseEntity.ok(roomService.myRooms(auth.getName()));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDetail> roomDetail(Authentication auth, @PathVariable String roomId) {
        return ResponseEntity.ok(roomService.roomDetail(auth.getName(), roomId));
    }

    @PostMapping("/{roomId}/approve/{username}")
    public ResponseEntity<RoomSummary> approve(Authentication auth, @PathVariable String roomId, @PathVariable String username) {
        return ResponseEntity.ok(roomService.approveJoin(auth.getName(), roomId, username));
    }

    @PostMapping("/{roomId}/deny/{username}")
    public ResponseEntity<Map<String, String>> deny(Authentication auth, @PathVariable String roomId, @PathVariable String username) {
        roomService.denyJoin(auth.getName(), roomId, username);
        return ResponseEntity.ok(Map.of("message", "Join request denied."));
    }

    @DeleteMapping("/{roomId}/members/{username}")
    public ResponseEntity<Map<String, String>> removeMember(Authentication auth, @PathVariable String roomId, @PathVariable String username) {
        roomService.removeMember(auth.getName(), roomId, username);
        return ResponseEntity.ok(Map.of("message", username + " removed from the room."));
    }
}
