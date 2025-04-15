// Initialize users_db
db = db.getSiblingDB('users_db');
db.createUser(
    {
        user: "admin",
        pwd: "admin",
        roles:[
            {
                role: "readWrite",
                db:   "users_db"
            }
        ]
    }
);
db.createCollection("Users");

// Initialize posts_db
db = db.getSiblingDB('posts_db');
db.createUser(
    {
        user: "admin",
        pwd: "admin",
        roles:[
            {
                role: "readWrite",
                db:   "posts_db"
            }
        ]
    }
);
db.createCollection("Posts");

// Initialize timelines_db
db = db.getSiblingDB('timelines_db');
db.createUser(
    {
        user: "admin",
        pwd: "admin",
        roles:[
            {
                role: "readWrite",
                db:   "timelines_db"
            }
        ]
    }
);
db.createCollection("UserTimelines");
db.createCollection("HomeTimelines");