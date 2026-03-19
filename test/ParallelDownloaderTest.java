import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParallelDownloaderTest {

    private static HttpServer server;
    private static final byte[] TEST_DATA = ("Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
            " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
            "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.").getBytes();
    private static final int PORT = 8081;

    @BeforeAll
    public static void setupServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/testfile.txt", exchange -> {  // basically we simulate scenarios here
            Headers headers = exchange.getResponseHeaders();
            headers.add("Accept-Ranges", "bytes");
            headers.add("Content-Length", String.valueOf(TEST_DATA.length));

            if (exchange.getRequestMethod().equalsIgnoreCase("HEAD")) {   // if he wants the head, we're done
                exchange.sendResponseHeaders(200, -1);
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {   // otherwise, it has to be this specific format
                String rangeHeader = exchange.getRequestHeaders().getFirst("Range");
                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    String[] parts = rangeHeader.substring(6).split("-");
                    int start = Integer.parseInt(parts[0]);
                    int end = Integer.parseInt(parts[1]);

                    int length = end - start + 1;
                    byte[] chunk = Arrays.copyOfRange(TEST_DATA, start, end + 1);

                    exchange.sendResponseHeaders(206, length); // we give the client a partial content
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(chunk);
                    }
                } else {   // if he didn't ask for a chunk, send it all
                    exchange.sendResponseHeaders(200, TEST_DATA.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(TEST_DATA);
                    }
                }
            }
        });
        server.start();
    }

    @AfterAll
    public static void stopServer() {
        server.stop(0);
    }

    @Test
    public void testParallelDownload() throws Exception {
        ParallelDownloader downloader = new ParallelDownloader();
        String destination = "downloaded_testfile.txt";
        String url = "http://localhost:" + PORT + "/testfile.txt";

        // we'll use 3 threads
        downloader.download(url, destination, 3);

        File downloadedFile = new File(destination);

        // we verify that what we downloaded matches the original
        byte[] downloadedData = Files.readAllBytes(downloadedFile.toPath());
        assertArrayEquals(TEST_DATA, downloadedData);

        downloadedFile.delete();
    }
}