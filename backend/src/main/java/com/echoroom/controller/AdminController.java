package com.echoroom.controller;

import com.echoroom.dto.RoomDtos.RoomSummary;
import com.echoroom.service.AdminService;
import com.echoroom.service.AdminService.DashboardStats;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Every endpoint here is additionally gated by hasRole("ADMIN") in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> stats() {
        return ResponseEntity.ok(adminService.stats());
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<RoomSummary>> rooms() {
        return ResponseEntity.ok(adminService.allRooms());
    }

    @PostMapping("/rooms/{roomId}/close")
    public ResponseEntity<RoomSummary> closeRoom(@PathVariable String roomId, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ResponseEntity.ok(adminService.closeRoom(roomId, reason));
    }
}
