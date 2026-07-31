package com.echoroom.repository;

import com.echoroom.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends MongoRepository<Room, String> {
    Optional<Room> findByRoomCode(String roomCode);
    boolean existsByRoomCode(String roomCode);
    List<Room> findByOwnerUsername(String ownerUsername);
    List<Room> findByMembersContaining(String username);
    List<Room> findByStatus(Room.RoomStatus status);
}
