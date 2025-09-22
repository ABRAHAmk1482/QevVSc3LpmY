// 代码生成时间: 2025-09-22 13:50:11
package com.example.services;

import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.VertxContextPRNG;
import io.vertx.reactivex.ext.auth.impl.JWTAuthProvider;
import io.vertx.reactivex.ext.auth.JWTOptions;
import io.vertx.reactivex.ext.auth.authentication.AuthenticationProvider;
import io.vertx.reactivex.ext.web.handler.AuthenticationHandler;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.UserSessionHandler;
import io.vertx.reactivex.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.reactivex.ext.web.codec廓;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.example.utils.Utils.convertToJson;

public class UserPermissionService {
    private Router router;
    private JWTAuthProvider authProvider;
    private final String secretKey = "secret-key";
    private final String audience = "audience";
    private final String issuer = "issuer";
    private final int ttl = 3600; // Time to live in seconds

    public UserPermissionService() {
        this.router = Router.router(io.vertx.reactivex.core.Vertx.vertx());
        initAuthProvider();
        setupRoutes();
    }

    private void initAuthProvider() {
        // Initialize JWTAuth provider with secret key
        this.authProvider = JWTAuthProvider.create(io.vertx.reactivex.core.Vertx.vertx(), new JWTOptions()
                .addPubSecKey(io.vertx.reactivex.core.Vertx.vertx().getOrCreateContext(), "classpath:publickey.pem", "classpath:privatekey.pem")
                .setJWTOptions(new JWTOptions()
                        .setExpiresInMinutes(ttl)
                        .setAudience(audience)
                        .setIssuer(issuer)));
    }

    private void setupRoutes() {
        // Body handler to read JSON bodies
        router.route("/*").handler(BodyHandler.create());

        // User session handler
        router.route("/*").handler(UserSessionHandler.create(io.vertx.reactivex.core.Vertx.vertx().sharedData()));

        // Authentication endpoint
        router.post("/login").handler(this::handleLogin);

        // Protected route example
        router.get("/protected").handler(JWTAuthHandler.create(authProvider));
        router.get("/protected").handler(this::handleProtectedRoute);
    }

    private void handleLogin(RoutingContext context) {
        // Extract credentials from body
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(context.getBodyAsJson().getString("username"), context.getBodyAsJson().getString("password"));

        // Authenticate the user
        authProvider.authenticate(credentials, res -> {
            if (res.succeeded()) {
                // User is authenticated, put in session
                User user = res.result();
                context.setUser(user);
                context.response()
                        .putHeader("content-type", "application/json")
                        .end(convertToJson(new HashMap<String, String>() {{ put("status", "success"); put("message", "User authenticated"); }}));
            } else {
                // Authentication failed
                context.response()
                        .setStatusCode(401)
                        .putHeader("content-type", "application/json")
                        .end(convertToJson(new HashMap<String, String>() {{ put("status", "error"); put("message", "Authentication failed"); }}));
            }
        });
    }

    private void handleProtectedRoute(RoutingContext context) {
        // Get user from session
        User user = context.user();
        if (user != null) {
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(convertToJson(new HashMap<String, String>() {{ put("status", "success"); put("message", "Access granted"); put("user", user.principal().getString("username")); }}));
        } else {
            context.response().setStatusCode(403).end("Access denied");
        }
    }

    // Method to start the service
    public void start() {
        int port = 8080;
        io.vertx.reactivex.core.Vertx.vertx()
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(port, result -> {
                    if (result.succeeded()) {
                        System.out.println("User Permission Service started on port " + port);
                    } else {
                        System.out.println("Could not start User Permission Service on port " + port);
                    }
                });
    }

    // Main method to run the service
    public static void main(String[] args) {
        UserPermissionService service = new UserPermissionService();
        service.start();
    }
}

/*
 * Utils.java
 * Utility class to convert objects to JSON.
 */
package com.example.utils;

import io.vertx.reactivex.core.json.JsonObject;
import java.util.Map;

public class Utils {
    public static String convertToJson(Map<String, String> map) {
        JsonObject jsonObject = new JsonObject(map);
        return jsonObject.encode();
    }
}
