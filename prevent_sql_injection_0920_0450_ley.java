// 代码生成时间: 2025-09-20 04:50:21
import io.vertx.core.AbstractVerticle;
    import io.vertx.core.AsyncResult;
    import io.vertx.core.Future;
    import io.vertx.core.Handler;
    import io.vertx.core.json.JsonArray;
    import io.vertx.core.json.JsonObject;
    import io.vertx.ext.sql.SQLClient;
    import io.vertx.ext.sql.SQLConnection;
    import io.vertx.ext.sql.UpdateResult;
    import io.vertx.ext.sql.impl.actions.prepared.PreparedQueryAction;
    import io.vertx.ext.sql.impl.actions.prepared.PreparedStatement;

    /**
     * This class demonstrates a Vert.x application using SQL client to prevent SQL injection.
     */
    public class PreventSqlInjection extends AbstractVerticle {

        @Override
        public void start(Future<Void> startFuture) {
            // Obtain the client from the Verticle context
            SQLClient sqlClient = getVertx().getOrCreateContext().config().getJsonObject("dbClientConfig").toSQLClient(getVertx());

            // Example usage: Prevent SQL injection by using parameterized queries
            preventSqlInjectionWithParameterizedQuery(sqlClient, startFuture);
        }

        /**
         * This method demonstrates how to prevent SQL injection using parameterized queries.
         *
         * @param sqlClient The SQL client to use for database operations
         * @param startFuture The future to complete once the example is done
         */
        private void preventSqlInjectionWithParameterizedQuery(SQLClient sqlClient, Future<Void> startFuture) {
            JsonObject user = new JsonObject()
                    .put("username", "safe_user")
                    .put("password", "safe_password");

            // Prepare the query with placeholders for parameters
            String preparedQueryString = "INSERT INTO users (username, password) VALUES (?, ?)";

            // Create a prepared statement using the SQL client
            PreparedQueryAction<PreparedStatement> preparedQuery = sqlClient.getConnection().compose(sqlConnection -> {
                return sqlConnection.prepare(preparedQueryString);
            });

            // Execute the prepared statement with parameters
            preparedQuery.compose(preparedStatement -> {
                return preparedStatement.execute(new JsonArray().add(user.getString("username")).add(user.getString("password")));
            }).onComplete(ar -> {
                if (ar.succeeded()) {
                    UpdateResult result = (UpdateResult) ar.result();
                    System.out.println("Number of rows affected: " + result.getUpdatedCount());
                    startFuture.complete();
                } else {
                    startFuture.fail(ar.cause());
                }
            });
        }
    }