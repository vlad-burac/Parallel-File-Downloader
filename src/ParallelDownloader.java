import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelDownloader {

    private final HttpClient httpClient;

    public ParallelDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void download(String url, String destinationPath, int numThreads) throws Exception {
        long fileSize = getFileSize(url);
        System.out.println("File size: " + fileSize + " bytes");

        // Pre-allocate the file on disk
        try (RandomAccessFile file = new RandomAccessFile(destinationPath, "rw")) {
            file.setLength(fileSize);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<Void>> tasks = new ArrayList<>();

        long chunkSize = fileSize / numThreads;

        for (int i = 0; i < numThreads; i++) {
            long startByte = i * chunkSize;
            // The last thread gets the remainder of the file
            long endByte = (i == numThreads - 1) ? fileSize - 1 : (startByte + chunkSize - 1);

            tasks.add(new Intellij_Task.DownloadTask(url, destinationPath, startByte, endByte, i, httpClient));
        }

        System.out.println("Starting download with " + numThreads + " threads...");
        // Invoke all tasks and wait for them to finish
        executor.invokeAll(tasks);
        executor.shutdown();
        System.out.println("Download complete: " + destinationPath);
    }

    private long getFileSize(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        String contentLengthStr = response.headers().firstValue("Content-Length")
                .orElseThrow(() -> new RuntimeException("Server did not return Content-Length header"));

        return Long.parseLong(contentLengthStr);
    }
}
