package iss.nus.Workshop27.controllers;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iss.nus.Workshop27.repositories.BoardgameRepository;
import jakarta.json.Json;

@RestController
@RequestMapping(path="/game")
public class GameRestController {

    @Autowired
    BoardgameRepository repo;
    
    @GetMapping(path="/{game_id}/reviews", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getReviewsbyGameId(@PathVariable(name="game_id") Integer gid) {

        Optional<String> payload = repo.getAllReviewsById(gid);

        if(payload.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Json.createObjectBuilder()
                            .add("error", HttpStatus.NOT_FOUND.value())
                            .add("timestamp", LocalDateTime.now().toString())
                            .add("message", "Game with id = %s not found".formatted(gid))
                            .build().toString());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload.get());
    }

    @GetMapping(path="/{direction}", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> ListBoardgameByRating(@PathVariable String direction) {
        
        Optional<String> payload = repo.getAllGamesOrderedByRating(direction);

        if (payload.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Json.createObjectBuilder()
                            .add("error", HttpStatus.NOT_FOUND.value())
                            .add("timestamp", LocalDateTime.now().toString())
                            .add("message", "Invalid URL or no result found")
                            .build().toString());
        }
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload.get());
    }
}
