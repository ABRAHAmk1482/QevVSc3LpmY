// 代码生成时间: 2025-09-22 02:23:55
import io.vertx.core.Vertx;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.AsyncResult;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.streams.Pipe;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BatchFileRenamer extends AbstractVerticle {

    private FileSystem fileSystem;

    @Override
    public void start(Promise<Void> startPromise) {
        fileSystem = vertx.fileSystem();
        startPromise.complete();
    }

    /**
     * Renames files in the specified directory based on a given prefix and a numeric index.
     *
     * @param config Configuration object containing the directory path and prefix.
     * @return A JsonObject with a list of renamed files.
     */
    public void renameFiles(JsonObject config) {
        String directory = config.getString("directory");
        String prefix = config.getString("prefix");

        // Check if the directory exists
        if (!Files.isDirectory(Paths.get(directory))) {
            vertx.failedFuture(new IllegalArgumentException("Directory does not exist: " + directory));
            return;
        }

        // List all files in the directory
        try (Stream<File> files = Files.walk(Paths.get(directory)).filter(File::isFile)) {
            List<File> fileList = files.collect(Collectors.toList());

            int index = 1;
            for (File file : fileList) {
                String fileName = file.getName();
                String newFileName = prefix + index++ + getFileExtension(fileName);
                File newFile = new File(file.getParent(), newFileName);

                // Rename the file
                fileSystem.rename(file.getAbsolutePath(), newFile.getAbsolutePath(), res -> {
                    if (res.succeeded()) {
                        vertx.logger().info("Renamed file: " + fileName + " to " + newFileName);
                    } else {
                        vertx.logger().error("Failed to rename file: " + fileName, res.cause());
                    }
                });
            }
        } catch (Exception e) {
            vertx.failedFuture(e);
        }
    }

    /**
     * Retrieves the file extension from a file name.
     *
     * @param fileName The name of the file.
     * @return The file extension.
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex);
        }
        return "";
    }

    /**
     * Deploys the BatchFileRenamer verticle with configuration.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        JsonObject config = new JsonObject().put("directory", "./").put("prefix", "new_");
        vertx.deployVerticle(new BatchFileRenamer(), config, result -> {
            if (result.succeeded()) {
                vertx.logger().info("BatchFileRenamer deployed successfully");
            } else {
                vertx.logger().error("Failed to deploy BatchFileRenamer", result.cause());
            }
        });
    }
}
