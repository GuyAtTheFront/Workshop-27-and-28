package iss.nus.Workshop27.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iss.nus.Workshop27.models.Review;
import iss.nus.Workshop27.models.ReviewValidationResult;
import iss.nus.Workshop27.services.BoardgameService;
import jakarta.json.Json;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/review")
public class ReviewRestController {
    
    @Autowired
    private BoardgameService boardgameService;

    @PostMapping(path="", 
        consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE, 
        produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addReview(@Valid Review review, BindingResult bindingResult) {
        
        Optional<String> name = boardgameService.findGameByGid(review.getGid());

        // custom validation
        if(name.isEmpty()) {
            FieldError err = new FieldError("review", "gid", "id not found in database");
            bindingResult.addError(err);
        }

        // if validation fails
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors()
                                                .stream()
                                                .map(x -> x.getDefaultMessage())
                                                .toList();

            ReviewValidationResult validation = 
                new ReviewValidationResult(HttpStatus.BAD_REQUEST.value(), errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(validation.toJson().toString());
        }

        // Fill in missing data in review
        review.setGameName(name.get());
        review.setPosted(LocalDate.now());

        // Insert document to Mongo db
        String id = boardgameService.insertReview(review);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("status", HttpStatus.CREATED.value())
                        .add("timestamp", LocalDateTime.now().toString())
                        .add("message", "Review created with id %s".formatted(id))
                        .build().toString());
    }

    @PutMapping(path="/{review_id}", 
        consumes=MediaType.APPLICATION_JSON_VALUE, 
        produces=MediaType.APPLICATION_JSON_VALUE)
    
    public ResponseEntity<String> updateReview(
            @PathVariable(name="review_id") String id, 
            @RequestBody String json) {

        // Optional here because user might pass in wrong data
        // null = review if fail to parse data
        Optional<Review> review = Review.fromJsonString(json);

        if (review.isEmpty()) {
            return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Json.createObjectBuilder()
                        .add("status", HttpStatus.BAD_REQUEST.value())
                        .add("timestamp", LocalDateTime.now().toString())
                        .add("error", "failed to process payload")
                        .build().toString());
        }

        // TODO: validate model
        Review newReview = review.get();
        newReview.setPosted(LocalDate.now());

        Boolean isUpdated = boardgameService.updateReview(id, newReview);

        if(!isUpdated) {
            // not updated --> document not found
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                    .add("status", HttpStatus.BAD_REQUEST.value())
                    .add("timestamp", LocalDateTime.now().toString())
                    .add("error", "No record of review with id=%s".formatted(id))
                    .build().toString());
        }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Json.createObjectBuilder()
                .add("status", HttpStatus.CREATED.value())
                .add("timestamp", LocalDateTime.now().toString())
                .add("message", "Review with id=%s updated".formatted(id))
                .build().toString());
    }

    @GetMapping(path="/{review_id}", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getLatestCommentAndRating(@PathVariable(name="review_id") String id) {
        String payload = boardgameService.getLatestComment(id);
        return ResponseEntity.ok().body(payload);
    }

    @GetMapping(path="/{review_id}/history", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getCommentHistory(@PathVariable(name="review_id") String id) {
        String payload = boardgameService.getCommentHistory(id);
        return ResponseEntity.ok().body(payload);
    }
}
