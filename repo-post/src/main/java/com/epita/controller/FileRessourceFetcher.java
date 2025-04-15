package com.epita.controller;

import com.mongodb.client.MongoClient;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/files")
public class FileRessourceFetcher {

    @Inject
    MongoClient mongoClient;

    @GET
    @Path("/{fileId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Télécharge un fichier média")
    @APIResponse(responseCode = "200", content = @Content(mediaType = "application/octet-stream"))
    @APIResponse(responseCode = "404", description = "Fichier non trouvé")
    public Response getFile(@PathParam("fileId") String fileId) {
        try {
            GridFSBucket gridFSBucket = GridFSBuckets.create(
                    mongoClient.getDatabase("posts_db"),
                    "files"
            );

            return Response.ok(
                    (StreamingOutput) output -> {
                        gridFSBucket.downloadToStream(new ObjectId(fileId), output);
                    }
            ).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("/clear")
    @Operation(summary = "Supprime tous les fichiers média")
    @APIResponse(responseCode = "200", description = "Tous les fichiers média ont été supprimés")
    public Response clearFiles() {
        try {
            GridFSBucket gridFSBucket = GridFSBuckets.create(
                    mongoClient.getDatabase("posts_db"),
                    "files"
            );
            gridFSBucket.drop();
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}