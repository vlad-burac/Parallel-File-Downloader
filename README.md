# Parallel File Downloader

This is a file downloader in Java that utilizes multithreading to download a file in parallel chunks.

## Features
* Uses HTTP `HEAD` requests to determine file size.
* Calculates byte ranges and uses HTTP `GET` with `Range` headers to download chunks in the same time, placing them in the right spot.

## How to Run it
To test this locally, you need a web server hosting a file. You can start one using Docker.

1. Create the folder to which the server will have access
2. Run the following Docker command to start an Apache server pointing to that folder:

```bash
docker run --rm -p 8080:80 -v /path/to/your/server-data:/usr/local/apache2/htdocs/ httpd:latest