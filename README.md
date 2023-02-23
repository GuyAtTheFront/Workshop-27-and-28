# Contents

* [Mongo Queries](#mongo-queries)
    * `insertOne()`
    * `updateOne()`

* [Mongo Aggregation](mongo-aggregation)
    * `$project`
        * `$ifNull` / `$last` / `$$NOW` / `$concat`
        * `$cond` / `$isNumber` / `$not`
    * `$group`
        * `$push`
        * `{$push: $$ROOT}`
    * `$lookup`
    * `$unwind`
        * `{$toString: '$$NOW'}`
    * `$sort`
    * `$group`


* [Mongo Aggregation to Java](mongo-aggregation-to-java)
    * [`$match`]($match)
    * `$addfields`
    * `$project`
    * `$group`
    * `$lookup`
    * `$unwind`
    * `$sort`

## Mongo Queries

* `insertOne()`

```
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
```


* `updateOne()` with `$push`

```
db.reviews.updateOne(
    {"_id" : ObjectId("63ee6574c60f97330fc81513")},
    {$push: {"edited": {comment: "new comment", rating: "10", posted: ISODate("2023-02-18T17:19:23.067+0000")}}}
);
```


## Mongo Aggregations


* `$project` with `$ifNull` / `$last` / `$$NOW`

```
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
```

* `$project` with `$concat`
* `$group` with `$push`
* `$lookup`
* `$unwind`
* `$project` with `{$toString: '$$NOW'}`

```
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
```


* `$addFields` (aka `$set`)
* `$project` with `$cond` / `$isNumber` / `$not`
* `$sort`
* `$group` with `{$push: $$ROOT}`
* `$addFields` (aka `$set`)
* `$project`
* `$group`
* `$project`

```
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
```

## Mongo Aggregation To Java

### `$match`

> Returns all matching documents

Mongo: 
```
{
    $match: { ID: 1 }
}
```

Java:
```
```

### `$addfields` / `$set`

> Insert a new field to all documents, and/or Update an existing field in all documents
> Returns all fields in all documents, including the insertion/update
> `$project` stage can acheive the same effect, but `$project` only returns projected fields
> i.e. `$addfields` needs fewer lines-of-code than `$project`

Mongo: 
```
{
    $addFields: {
        edited: {$last: "$edited"}
    }
}
```

Java:
```
AddFieldsOperation addEdited = Aggregation.addFields()
                                .addField("edited")
                                .withValueOfExpression("{$last: '$edited'}")
                                .build();

```

### `$project`

#### Projecting Values / Objects

Mongo:
```
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
```

Java:
```
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
```

---------------------

#### Projecting Literals

Mongo:
```
{
    $project: {
        _id: 0,
        rating: "highest",
        timestamp: {$toString: "$$NOW"},
        games: '$games'
    }
}
```

Java:
```
ProjectionOperation projectOutput = Aggregation.project().andExclude("_id")
                .and(direction).asLiteral().as("rating")
                .andExpression("{$toString: '$$NOW'}").as("timestamp")
                .andExpression("'$games'").as("games");
```

### `$group`

#### Group by _id and push original document into output

Mongo:
```
{
    $group: {
        _id: "$gid",
        others: {$push: "$$ROOT"}
    }
}
```

Java:
```
final String pushRoot = 
"""
$push: '$$ROOT'
""";

MongoExpression pushRootExp = MongoExpression.create(pushRoot);


GroupOperation groupByGid = Aggregation.group("$gid")
                            .and("others", AggregationExpression.from(pushRootExp));
```

---------------------

#### Group Null - Group all documents into a single Array

Mongo:
```
{
    $group: {
        _id: null,
        games: {$push: '$$ROOT'}
    }
}
```

Java:
```
final String pushRoot = 
"""
$push: '$$ROOT'
""";

MongoExpression pushRootExp = MongoExpression.create(pushRoot);


GroupOperation groupByGid = Aggregation.group()
                            .and("others", AggregationExpression.from(pushRootExp));
```


### `$lookup`
Mongo:
```
{
    $lookup: {
        from: "games",
        localField: "_id",
        foreignField: "gid",
        as: "game"
    }
}
```

Java:
```
LookupOperation lookupGames = Aggregation.lookup(
                                COLLECTION_GAMES, 
                                FIELD_OBJECT_ID, 
                                FIELD_GID, 
                                "game");
```

### `$unwind`
Mongo:
```
{
    $unwind: "$game"
}
```

Java:
```
UnwindOperation unwindGame = Aggregation.unwind("game");
```


### `$sort`
Mongo:
```
{
    $sort: {"rating": -1}
}
```

Java:
```
SortOperation sortRating = Aggregation.sort(Sort.Direction.DESC, "rating");
```