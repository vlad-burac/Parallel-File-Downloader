import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Callable;

public class DownloadTask implements Callable<Void> {
    private String url;
    private String destinationPath;
    private long startByte;
    private long endByte;
    private int partId;
    private HttpClient httpClient;

    public DownloadTask(String url, String destinationPath, long startByte, long endByte, int partId, HttpClient httpClient) {
        this.url = url;
        this.destinationPath = destinationPath;
        this.startByte = startByte;
        this.endByte = endByte;
        this.partId = partId;
        this.httpClient = httpClient;
    }

    @Override
    public Void call() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Range", "bytes=" + startByte + "-" + endByte)
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        // write the specific chunk to the correct location in the file
        try (InputStream is = response.body();
             RandomAccessFile file = new RandomAccessFile(destinationPath, "rw")) {

            file.seek(startByte); // we jump to where we need

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                file.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("Part " + partId + " downloaded (" + startByte + " to " + endByte + ")");
        return null;
    }
}
