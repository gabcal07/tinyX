// MongoDB initialization script for shared MongoDB instance
print("MongoDB initialization script for shared MongoDB instance loaded");

function initReplSet() {
    try {
        // Check if replica set is already initialized
        let status = rs.status();
        if (status.ok) {
            print("Replica set is already initialized");
            return true;
        }
    } catch (e) {
        // If rs.status() fails, the replica set is not initialized yet
        print("Initializing replica set...");
        try {
            rs.initiate({
                _id: "rs0",
                members: [
                    { _id: 0, host: "shared-mongodb-0.shared-mongodb.tinyx.svc.cluster.local:27017" },
                    { _id: 1, host: "shared-mongodb-1.shared-mongodb.tinyx.svc.cluster.local:27017" }
                ]
            });
            return true;
        } catch (error) {
            print("Error initializing replica set:", error);
            return false;
        }
    }
    return true;
}

function waitForPrimary() {
    let attempts = 0;
    while (attempts < 30) {  // Limit attempts to avoid infinite loop
        try {
            let status = rs.status();
            if (status.members) {
                let primary = status.members.find(m => m.state === 1);
                if (primary) {
                    print("Primary found!");
                    return true;
                }
            }
        } catch (error) {
            print("Waiting for primary...");
        }
        sleep(2000);  // Wait 2 seconds between attempts
        attempts++;
    }
    print("Timed out waiting for primary election");
    return false;
}

function initDatabases() {
    try {
        // Create admin user
        db = db.getSiblingDB('admin');
        
        // Check if admin user already exists
        let adminUsers = db.getUsers();
        let adminExists = adminUsers.users.some(user => user.user === "admin");
        
        if (!adminExists) {
            print("Creating admin user...");
            db.createUser({
                user: "admin",
                pwd: "admin",
                roles: [
                    { role: "root", db: "admin" }
                ]
            });
        } else {
            print("Admin user already exists");
        }

        // Create user database and user
        createDatabaseWithUser('users_db', 'Users');
        
        // Create post database and user
        createDatabaseWithUser('posts_db', 'Posts');
        
        // Create social database and user
        createDatabaseWithUser('social_db', 'Relationships');
        
        // Create user timeline database and user
        createDatabaseWithUser('user_timelines_db', 'UserTimelines');
        
        // Create home timeline database and user
        createDatabaseWithUser('home_timelines_db', 'HomeTimelines');
        
        // Create search database and user
        createDatabaseWithUser('search_db', 'SearchIndex');
        
        return true;
    } catch (error) {
        print("Error initializing databases:", error);
        return false;
    }
}

function createDatabaseWithUser(dbName, collectionName) {
    try {
        // Switch to the database
        db = db.getSiblingDB(dbName);
        
        // Check if user already exists
        let dbUsers = db.getUsers();
        let dbAdminExists = dbUsers.users ? dbUsers.users.some(user => user.user === "admin") : false;
        
        if (!dbAdminExists) {
            print("Creating " + dbName + " user...");
            db.createUser({
                user: "admin",
                pwd: "admin",
                roles: [
                    { role: "readWrite", db: dbName }
                ]
            });
            
            // Create a collection to ensure database is initialized
            db.createCollection(collectionName);
            print("Database " + dbName + " initialized successfully");
        } else {
            print("User for " + dbName + " already exists");
        }
    } catch (error) {
        print("Error creating database " + dbName + ":", error);
    }
}

// Main execution block
try {
    print("Starting initialization process...");
    if (initReplSet()) {
        print("Waiting for primary election...");
        if (waitForPrimary()) {
            print("Primary elected, initializing databases...");
            if (initDatabases()) {
                print("All databases initialization completed successfully!");
            }
        }
    }
} catch (e) {
    print("Error during initialization:", e);
} 