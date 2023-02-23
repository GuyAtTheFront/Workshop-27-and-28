package iss.nus.Workshop27.repositories;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.SystemProperties;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.thymeleaf.standard.expression.AndExpression;

import com.mongodb.client.result.UpdateResult;

import iss.nus.Workshop27.models.Review;

import static iss.nus.Workshop27.utils.Constants.*;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.swing.GroupLayout.Group;

@Repository
public class BoardgameRepository {
    
    @Autowired
    MongoTemplate mongoTemplate;

    public Optional<String> findGameByGid(Integer gid){

        Query query = Query.query(Criteria.where(FIELD_GID).is(gid));
        query.fields().include(FIELD_NAME).exclude(FIELD_OBJECT_ID);

        Document document = mongoTemplate.findOne(query, Document.class, COLLECTION_GAMES);

        String name = (null == document) ? null : document.getString(FIELD_NAME);

        return Optional.ofNullable(name);
    }

    public String insertReview(Review review) {
        /*
         db.reviews.insertOne(
            {
                "user" : "test",
                "rating" : NumberInt(5),
                "comment" : "hahaha",
                "ID" : NumberInt(1),
                "posted" : ISODate("2023-02-16T16:00:00.000+0000"),
                "name" : "Die Macher"
            }
            )
         */

        Document document = mongoTemplate.insert(review.toDocument(), COLLECTION_REVIEWS);
        return document.getObjectId(FIELD_OBJECT_ID).toString();
    }

    public Boolean updateReview(String id, Review review) {
        /*
            db.reviews.updateOne(
                {"_id" : ObjectId("63ee6574c60f97330fc81513")},
                {$push: {"edited": {comment: "new comment", rating: "10", posted: ISODate("2023-02-18T17:19:23.067+0000")}}}
            );
         */
        
        Criteria criteria = Criteria.where(FIELD_OBJECT_ID).is(new ObjectId(id));
        Query query = Query.query(criteria);

        Document doc = new Document()
            .append(FIELD_COMMENT, review.getComment())
            .append(FIELD_RATING, review.getRating())
            .append(FIELD_POSTED, LocalDateTime.now());

        Update updateOps = new Update()
                            .push(FIELD_EDITED, doc);
        
        UpdateResult updateResult = mongoTemplate.updateFirst(query, updateOps, Document.class, COLLECTION_REVIEWS);
        
        // Assumes that if ModifiedCount == 0, then document not found
        // does not account for document found but not modified <-- possibly malformed update statement
        return (updateResult.getModifiedCount() == 1);
    }

    public Document getLatestComment(String id) {
        /*
            db.reviews.aggregate([
            {
                $match: {_id: ObjectId("63ee6574c60f97330fc81513")}
            }
            ,
            {
                $project: {
                    user: 1,
                    rating: {$ifNull: [{$last: "$edited.rating"}, "$rating"]},
                    comment: {$ifNull: [{$last: "$edited.comment"}, "$comment"]},
                    ID: 1,
                    posted: 1,
                    name: 1,
                    edited: {$ifNull: [true, false]},
                    timestamp: "$$NOW"
                }
        ])
         */

        
        MatchOperation matchObjectId = Aggregation.match(
                    Criteria.where(FIELD_OBJECT_ID).is(new ObjectId(id)));

        MongoExpression projectRating = MongoExpression.create("""
            $ifNull: [{$last: "$edited.rating"}, "$rating"]
                """);
        MongoExpression projectComment = MongoExpression.create("""
            $ifNull: [{$last: "$edited.comment"}, "$comment"]
                """);

        MongoExpression projectEdited = MongoExpression.create("""
            $ifNull: [true, false]
                """);


        // MongoExpression projectTimestamp = MongoExpression.create("""
        //     "$$NOW"
        //         """);


        ProjectionOperation project = Aggregation.project(FIELD_USER, FIELD_ID, FIELD_POSTED)
                                        .andExclude(FIELD_OBJECT_ID)
                                        .and(AggregationExpression.from(projectRating)).as(FIELD_RATING)
                                        .and(AggregationExpression.from(projectComment)).as(FIELD_COMMENT)
                                        .and(AggregationExpression.from(projectEdited)).as(FIELD_EDITED)
                                        .andExpression("$$NOW").as("timestamp")
                                        // .and(AggregationExpression.from(projectTimestamp)).as("timestamp")
                                        ;
    
        

        Aggregation pipeline = Aggregation.newAggregation(matchObjectId, project);

        AggregationResults<Document> results = mongoTemplate.aggregate(pipeline, COLLECTION_REVIEWS, Document.class);

        return results.getMappedResults().get(0);
    }

    public Document getCommentHistoryById(String id) {

        Query query = Query.query(Criteria.where(FIELD_OBJECT_ID).is(new ObjectId(id)));

        Document doc = mongoTemplate.findOne(query, Document.class, COLLECTION_REVIEWS);

        return doc;
    }


    // Workshop 28(a)
    public Optional<String> getAllReviewsById(Integer id) {
        /*
        db.reviews.aggregate([
            {
                $match: { ID: 1 }
            }
            ,
            {
                $project: {
                    ID: 1,
                    _id: 1,
                    path: { $concat: ["/review/", { $toString: "$_id" }] }
                }
            }
            ,
            {
                $group: {
                    _id: "$ID",
                    reviews: {$push: "$path"}
                }
            }
            ,
            {
                $lookup: {
                    from: "games",
                    localField: "_id",
                    foreignField: "gid",
                    as: "game"
                }
            }
            ,
            {
                $unwind: "$game"
            }
            ,
            {
                $project: {
                    game_id: "$game.gid",
                    name: "$game.name",
                    year: "$game.year",
                    rank: "$game.rank",
                    users_rated: "$game.users_rated",
                    url: "$game.url",
                    thumbnail: "$game.thumbnail",
                    reviews: "$reviews",
                    timestamp: {$toString: "$$NOW"}
                }
            }  
        ]);
        */

        MongoExpression matchIdExp = MongoExpression.create("""
            { ID: ?0 }""", id);

        MatchOperation matchId = Aggregation.match(AggregationExpression.from(matchIdExp));

        MongoExpression projectPathExp = MongoExpression.create("""
            $concat: ["/review/", { $toString: "$_id" }]
                """);
        ProjectionOperation projectPath = Aggregation.project()
                                            .andInclude(FIELD_ID, FIELD_OBJECT_ID)
                                            .and(AggregationExpression.from(projectPathExp)).as("path");
        
        MongoExpression groupIdExp = MongoExpression.create("""
            $push: "$path"
                """);
        GroupOperation groupId = Aggregation.group(FIELD_ID).and("reviews", AggregationExpression.from(groupIdExp));
    
        LookupOperation lookupGames = Aggregation.lookup(COLLECTION_GAMES, FIELD_OBJECT_ID, FIELD_GID, "game");
        UnwindOperation unwindGame = Aggregation.unwind("game");

        ProjectionOperation projectOutput = Aggregation.project()
                            .andExpression("game.gid").as("game_id")
                            .andExpression("game.name").as("name")
                            .andExpression("game.year").as("year")
                            .andExpression("game.rank").as("rank")
                            .andExpression("game.users_rated").as("users_rated")
                            .andExpression("game.url").as("url")
                            .andExpression("game.thumbnail").as("thumbnail")
                            .andExpression("reviews").as("reviews")
                            .and(AggregationExpression
                                .from(MongoExpression.create("{$toString: '$$NOW'}")))
                                .as("timestamp");


        Aggregation pipeline = Aggregation.newAggregation(matchId, projectPath, groupId, 
                                                        lookupGames, unwindGame, projectOutput);
            
        System.out.println(pipeline.toString());

        AggregationResults<Document> results = mongoTemplate.aggregate(pipeline, COLLECTION_REVIEWS, Document.class);
        
        return (results.getMappedResults().size() == 0) ? 
                    Optional.empty() : 
                    Optional.of(results.getUniqueMappedResult().toJson());

    }

    public Optional<String> getAllGamesOrderedByRating(String direction) {
        /*
                db.reviews.aggregate([
            {
                $addFields: {
                    edited: {$last: "$edited"}
                }
            }
            ,
            {
                $project: {
                    rating: {
                        $cond: [
                            {$not: {$isNumber: "$edited.rating"}}, 
                            "$rating",
                            "$edited.rating"
                        ]
                    },
                    user: 1,
                    comment: {
                        $cond: [
                            {$not: {$isNumber: "$edited.rating"}}, 
                            "$comment",
                            "$edited.comment"
                        ]
                    },
                    gid: "$ID",
                    name: "$name"
                }
            }
            ,
            {
                $sort: {"rating": -1}
            }
            ,
            {
                $group: {
                    _id: "$gid",
                    others: {$push: "$$ROOT"}
                }
            }
            ,
            {
                $set: {
                    "others": {$first: "$others"}
                }
            }
            ,
            {
                $project: {
                    _id: 1,
                    name: "$others.name",
                    rating: "$others.rating",
                    user: "$others.user",
                    comment: "$others.user",
                    review_id: {$toString: "$others._id"}
                }
            }
            ,
            {
                $group: {
                    _id: null,
                    games: {$push: '$$ROOT'}
                }
            }
            ,
            {
                $project: {
                    _id: 0,
                    rating: "highest",
                    timestamp: {$toString: "$$NOW"},
                    games: '$games'
                }
            }
        ]);
        */

        Direction dir = null;

        switch(direction.toUpperCase()) {
            case "HIGHEST", "HIGH", "BEST", "TOP", "DESC":
                dir = Direction.DESC;
                break;
            
            case "LOWEST", "LOW", "WORST", "BOTTOM", "ASC":
                dir = Direction.ASC;
                break;

            default:
                return Optional.empty();
        }

        AddFieldsOperation addEdited = Aggregation.addFields().addField("edited").withValueOfExpression("{$last: '$edited'}").build();
        
        final String addRatingFormatted = 
        """
            $cond: [
                {$not: {$isNumber: "$edited.rating"}}, 
                "$rating",
                "$edited.rating"
                ]
        """;

        final String addCommentFormatted = 
        """
            $cond: [
                {$not: {$isNumber: "$edited.rating"}}, 
                "$comment",
                "$edited.comment"
                ]
        """;

        MongoExpression addCommentExp = MongoExpression.create(addCommentFormatted);
        MongoExpression addRatingExp = MongoExpression.create(addRatingFormatted);

        ProjectionOperation projectData = Aggregation.project("gid")
                                            .andExpression("$ID").as("gid")
                                            .andExpression("$name").as("name")
                                            .and(AggregationExpression.from(addCommentExp)).as("comment")
                                            .and(AggregationExpression.from(addRatingExp)).as("rating");

        SortOperation sortRating = Aggregation.sort(dir, "rating");

        GroupOperation groupByGid = Aggregation.group("$gid")
                                    .and("others", AggregationExpression.from(MongoExpression.create("{$push: '$$ROOT'}")));
        
        AddFieldsOperation addFirstReview = Aggregation.addFields()
                                            .addField("others")
                                            .withValueOfExpression("{$first: '$others'}")
                                            .build();
                                
        ProjectionOperation projectGamesList = Aggregation.project()
                    .andExpression("'$_id'").as("_id")
                    .andExpression("'$others.name'").as("name")
                    .andExpression("'$others.rating'").as("rating")
                    .andExpression("'$others.user'").as("user")
                    .andExpression("'$others.comment'").as("comment")
                    .andExpression("'$others.review_id'").as("review_id");

        GroupOperation groupNull = Aggregation.group()
                                    .and("games", AggregationExpression.from(MongoExpression.create("{$push: '$$ROOT'}")));

        ProjectionOperation projectOutput = Aggregation.project().andExclude("_id")
                        .and(direction).asLiteral().as("rating")
                        .andExpression("{$toString: '$$NOW'}").as("timestamp")
                        .andExpression("'$games'").as("games");

        Aggregation pipeline = Aggregation.newAggregation(addEdited, projectData, sortRating,
                                                            groupByGid, addFirstReview, projectGamesList,
                                                            groupNull, projectOutput);

        System.out.println(pipeline.toString());
        
        AggregationResults<Document> results = mongoTemplate.aggregate(pipeline, COLLECTION_REVIEWS, Document.class);

        // System.out.println(">>> \n\n\n\n\n" + results.getRawResults().toJson());
        
        return (null == results) ? Optional.empty() : Optional.of(results.getUniqueMappedResult().toJson());
    }

    // public Optional<Review> findReviewById(String id) {
    //     Criteria criteria = Criteria.where(FIELD_OBJECT_ID).is(new ObjectId(id));

    //     Query query = Query.query(criteria);

    //     Document document = mongoTemplate.findOne(query, Document.class, COLLECTION_REVIEWS);

    //     Review review = (null == document) ? null : Review.fromMongoDocument(document);
    //     return Optional.ofNullable(review);
    // }
}
