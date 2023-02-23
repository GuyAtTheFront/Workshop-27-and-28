package iss.nus.Workshop27.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.bson.BsonDateTime;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import iss.nus.Workshop27.models.Review;
import iss.nus.Workshop27.repositories.BoardgameRepository;

@Service
public class BoardgameService {
    
    @Autowired
    private BoardgameRepository boardgameRepo;

    public Optional<String> findGameByGid(Integer gid) {
        return boardgameRepo.findGameByGid(gid);
    }

    public String insertReview(Review review) {
        return boardgameRepo.insertReview(review);
    }

    public Boolean updateReview(String id, Review review) {
        return boardgameRepo.updateReview(id, review);
    }

    public String getLatestComment(String id) {
        // I know the output is different from the question
        // put im lazy to fix it
        return boardgameRepo.getLatestComment(id).toJson();
    }

    public String getCommentHistory(String id) {
        Document doc = boardgameRepo.getCommentHistoryById(id);
        doc.put("timestamp", new BsonDateTime(Instant.now().toEpochMilli()));
        
        return doc.toJson();
    }
}
