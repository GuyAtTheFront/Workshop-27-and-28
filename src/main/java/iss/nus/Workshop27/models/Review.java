package iss.nus.Workshop27.models;


import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.bson.Document;


import iss.nus.Workshop27.utils.Converters;
import jakarta.json.JsonObject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import static iss.nus.Workshop27.utils.Constants.*;

public class Review {
    
    @NotBlank(message="user cannot be blank or null")
    private String user;

    @Min(value=0, message="rating cannot be less-than 0")
    @Max(value=10, message="rating cannot be greater-than 10")
    @NotNull(message="rating cannot be null")
    private Integer rating;
    
    private String comment;
    
    // custom validation
    private Integer gid;
    private LocalDate posted; 
    private String gameName;

    private Integer id;

    public Document toDocument() {
        Document doc = new Document();
        doc.append("user", this.getUser())
        .append("rating", this.getRating())
        .append("comment", this.getComment())
        .append("ID", this.getGid())
        .append("posted", this.getPosted()) 
        .append("name", this.getGameName());
        return doc;
    }

    public static Review fromMongoDocument(Document doc) {
        Review review = new Review();
        review.setUser(doc.getString(FIELD_USER));
        review.setRating(doc.getInteger(FIELD_RATING));
        review.setComment(doc.getString(FIELD_COMMENT));
        review.setId(doc.getInteger(FIELD_ID));
        review.setPosted(doc.getDate(FIELD_POSTED).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        review.setGameName(doc.getString(FIELD_NAME));
        return review;
    }

    public static Optional<Review> fromJsonString(String json) {
        //TODO:  how to validate json?

        try {
            JsonObject data = Converters.toJson(json);
            System.out.println(">>> \n\n\n\n\n" + "hello");
            Review review = new Review();
            // review.setUser(data.getString("user"));
            review.setRating(data.getInt("rating"));
            review.setComment(data.getString("comment"));
            // review.setId(data.getInt("id"));

            return Optional.of(review);

        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    @Override
    public String toString() {
        return "Review [user=" + user + ", rating=" + rating + ", comment=" + comment + ", gid=" + gid + ", posted="
                + posted + ", gameName=" + gameName + "]";
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
        this.gid = id;
    }
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user.trim();
    }
    public Integer getRating() {
        return rating;
    }
    public void setRating(Integer rating) {
        this.rating = rating;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public Integer getGid() {
        return gid;
    }
    public void setGid(Integer gid) {
        this.gid = gid;
    }
    public LocalDate getPosted() {
        return posted;
    }
    public void setPosted(LocalDate posted) {
        this.posted = posted;
    }
    public String getGameName() {
        return gameName;
    }
    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
}
