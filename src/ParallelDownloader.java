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

        // here we set the size of the file beforehand
        try (RandomAccessFile file = new RandomAccessFile(destinationPath, "rw")) {
            file.setLength(fileSize);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<Void>> tasks = new ArrayList<>();

        long chunkSize = fileSize / numThreads;

        for (int i = 0; i < numThreads; i++) {
            long startByte = i * chunkSize;
            // last thread will get what is left
            long endByte = (i == numThreads - 1) ? fileSize - 1 : (startByte + chunkSize - 1);

            tasks.add(new DownloadTask(url, destinationPath, startByte, endByte, i, httpClient));
        }

        // start all tasks simulatneously
        executor.invokeAll(tasks);
        executor.shutdown();
    }

    private long getFileSize(String url) throws IOException, InterruptedException {
        // we use that HEAD request to get the size
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        String contentLengthStr = response.headers().firstValue("Content-Length")
                .orElseThrow(() -> new RuntimeException("There is no content length header"));

        return Long.parseLong(contentLengthStr);
    }

    public static void main(String[] args) {
        String url = "http://localhost:8080/my-test-file.txt"; // we get the file from the server
        String destination = "downloaded_file.txt";   // we save it here
        int threads = 4;

        ParallelDownloader downloader = new ParallelDownloader();

        try {
            downloader.download(url, destination, threads);
        } catch (Exception e) {
            System.err.println("Download failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
