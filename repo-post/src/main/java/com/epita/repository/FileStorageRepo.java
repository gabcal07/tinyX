package com.epita.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;

@ApplicationScoped
public class FileStorageRepo {

    @Inject
    MongoClient mongoClient;

    public String storeFile(FileUpload fileUpload) throws IOException {
        GridFSBucket gridFSBucket = GridFSBuckets.create(
                mongoClient.getDatabase("posts_db"),
                "files"  // Nom du bucket
        );

        GridFSUploadOptions options = new GridFSUploadOptions()
                .metadata(new Document("contentType", fileUpload.contentType()));

        ObjectId fileId = gridFSBucket.uploadFromStream(
                fileUpload.fileName(),
                Files.newInputStream(fileUpload.uploadedFile()),
                options
        );

        return fileId.toString();
    }

    public String getFileUrl(String fileId) {
        return "/api/files/" + fileId;
    }

    public String getFileIdFromUrl(String mediaUrl) {
        return mediaUrl.substring(mediaUrl.lastIndexOf('/') + 1);
    }

    public void deleteFile(String fileId) {
        GridFSBucket gridFSBucket = GridFSBuckets.create(
                mongoClient.getDatabase("posts_db"),
                "files"
        );

        gridFSBucket.delete(new ObjectId(fileId));
    }
}