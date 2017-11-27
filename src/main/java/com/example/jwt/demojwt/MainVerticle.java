package com.example.jwt.demojwt;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class MainVerticle extends AbstractVerticle {

    public static void main(String[] args) {
        Launcher.executeCommand("run", MainVerticle.class.getName());
    }

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);

        JWTAuthOptions config = new JWTAuthOptions()
            .setKeyStore(new KeyStoreOptions()
                .setPath("keystore.jceks")
                .setPassword("secret"));

        // Create a JWT Auth Provider
        JWTAuth jwt = JWTAuth.create(vertx, config);
        JWTAuthHandler jwtAuthHandler = JWTAuthHandler.create(jwt, "/api/newToken");

        // protect the API
        router.route("/api/*").handler(jwtAuthHandler);
        router.route().failureHandler(ctx -> {
            int statusCode = ctx.statusCode();
            System.out.println("statusCode = " + statusCode);
//            if (ctx.failed()) {
            System.out.println("response statusCode = " + statusCode);
            String statusMessage = HttpResponseStatus.valueOf(statusCode).reasonPhrase();
            System.out.println("response statusMessage = " + statusMessage);
//            }
            ctx.response()
                .setStatusCode(statusCode)
                .setStatusMessage(statusMessage)
                .end(statusMessage);


//            String refreshToken = ctx.request().headers().get("RefreshToken");
//            String r = refreshToken.substring(refreshToken.indexOf(' '));
//            jwt.authenticate(new JsonObject()
//            .put("jwt", r), event -> {
//
//            });

            jwtAuthHandler.handle();

        });

//        router.get("/").handler(ctx -> {
//            ctx.response().putHeader("Content-Type", "text/html");
//
//            try {
//                FileInputStream inputStream = new FileInputStream(new File("/Users/dalu-tp/IdeaProjects/demojwt/src/main/resources/index.html"));
//                int size = inputStream.available();
//                byte[] buf = new byte[size];
//                inputStream.read(buf);
//
//                ctx.response().end(new String(buf, "utf-8"));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//
//        });

        // this route is excluded from the auth handler
        router.get("/api/newToken").handler(ctx -> {
            ctx.response().putHeader("Content-Type", "text/plain");

            String token = jwt.generateToken(new JsonObject(), new JWTOptions().setExpiresInSeconds(10L));
            String res = "Bearer " + token;
            System.out.println("token = " + res);

            String refreshToken = jwt.generateToken(new JsonObject(), new JWTOptions().setExpiresInSeconds(3600L));
            ctx.response().end(res + " == " + refreshToken);
        });

        // this is the secret API
        router.get("/api/protected").handler(ctx -> {
            ctx.response().putHeader("Content-Type", "text/plain");
            ctx.response().end("a secret you should keep for yourself...");
        });

        // Serve the non private static pages
        router.route().handler(StaticHandler.create());

        vertx.createHttpServer().requestHandler(router::accept).listen(8383);
    }
}
