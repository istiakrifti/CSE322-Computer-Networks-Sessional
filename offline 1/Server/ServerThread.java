package Server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class ServerThread extends Thread {
    private final BufferedReader bufferedReader;
    private final BufferedWriter bufferedWriter;
    private final PrintWriter printWriter;
    private final Socket clientSocket;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;

    public ServerThread(Socket clientSocket) throws IOException {

        this.clientSocket = clientSocket;
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        bufferedWriter = new BufferedWriter(new FileWriter("log.txt", true));
        printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        inputStream = new DataInputStream(clientSocket.getInputStream());
        outputStream = new DataOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            String req = bufferedReader.readLine();
            logRequest(req);

            if (req == null || req.length() == 0) return;

            if (req.startsWith("GET")) {
                handleGetRequest(req);
            } else if(req.startsWith("UPLOAD"))
            {
                handleUploadRequest(req);
            }
            else {
                sendErrorResponse(404, "Page not found");
                System.out.println("404 (Page not found)");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedReader.close();
                bufferedWriter.close();
                printWriter.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleGetRequest(String requestLine) throws IOException {
        String[] parts = requestLine.split(" ");
        String req = parts[2];
        if (parts.length != 3 || !req.startsWith("HTTP/")) {
            sendErrorResponse(404, "Page not found");
            System.out.println("404 (Page not found)");
            return;
        }

        String path = parts[1];
//        System.out.println(path);
        File file = new File("." + path);

        if(!file.exists()) {
            sendErrorResponse(404, "Page Not Found");
            System.out.println("404 (Page not found)");
            return;
        }

        String dir = "";

        if(!path.equals("/")) {
            String[] str = path.split("/");
            dir = str[str.length-1];
        }


        if(dir.contains(".")) {
            sendFileResponse(file,path,dir);
        }
        else {
            if(!path.equals("/")) sendDirectoryListing(file,path);
            else sendDirectoryListing(file,dir);
        }
    }

    private void sendDirectoryListing(File directory,String path) throws IOException {

        StringBuilder response = new StringBuilder("<html><body><h1>List of Directories: </h1><ul>");

        for (File file : directory.listFiles()) {

            if (file.isDirectory()) {
                response.append("<li><b><i><a href=\"" + path + "/" + file.getName() + "\">" + file.getName() + "/</a></i></b></li>");
            } else {
                response.append("<li><a href=\"" + path + "/" + file.getName() + "\">" + file.getName() + "</a></li>");
            }
        }
        response.append("</ul></body></html>");
        sendHttpResponse(200, "text/html", response.toString());
        System.out.println("200 (OK)");
    }

    private void sendFileResponse(File file, String path, String dir) throws IOException {
        String mimeType = getContentType(dir);
//        System.out.println("dfdsgsdf");


        if (mimeType.equals("text/plain")) {
            StringBuilder response = new StringBuilder("<html><body>");
            response.append("<h1>Text File: " + file.getName() + "</h1><p>" + Files.readString(file.toPath()) + "</p>" + "</body></html>");
            sendHttpResponse(200, "text/html", response.toString());
        } else if (mimeType.equals("image/jpeg") || mimeType.equals("image/png")){

            printWriter.println("HTTP/1.1 200 OK");
            printWriter.println("Server: Java HTTP Server: 1.0");
            printWriter.println("Date: " + new Date());
            printWriter.println("Content-Type: " + mimeType);
            printWriter.println("Content-Length: " + file.length());
            printWriter.println("Connection: close");
            printWriter.println();
            printWriter.flush();

            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                 OutputStream out = clientSocket.getOutputStream()) {
                byte[] buffer = new byte[32];
                int bytes;
                while ((bytes = bis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytes);
                }
                out.flush();
            }
        }
        else{

            printWriter.println("HTTP/1.1 200 OK");
            printWriter.println("Server: Java HTTP Server: 1.0");
            printWriter.println("Date: " + new Date());
            printWriter.println("Content-Type: " + mimeType);
            printWriter.println("Content-Length: " + file.length());
            printWriter.println("Connection: close");
            printWriter.println();
            printWriter.flush();


            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[4096];
                int bytes = 0;
                OutputStream out = clientSocket.getOutputStream();
                while ((bytes = bis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytes);
                    out.flush();
                }

            }
        }
    }

    private void handleUploadRequest(String requestLine) throws IOException {
        String[] parts = requestLine.split(" ");
        if (parts.length != 2) {
            sendErrorResponse(400, "Invalid UPLOAD request");
            return;
        }

        String fileName = parts[1];
        if (!(fileName.endsWith(".txt") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png"))) {
            sendErrorResponse(400, "Invalid File Format");
            return;
        }

        printWriter.println("OK");
        printWriter.flush();
        System.out.println("Acknowledgment sent: OK");

        File uploadDir = new File("./uploaded");
        if (!uploadDir.exists()) uploadDir.mkdirs();
        File file = new File(uploadDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            long fileSize = inputStream.readLong();
            byte[] buffer = new byte[4096];
            long totalRead = 0;
            int bytesRead;

            while (totalRead < fileSize && (bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            fos.flush();

            printWriter.println("HTTP/1.1 200 OK");
            printWriter.println("Server: Java HTTP Server: 1.0");
            printWriter.println("Date: " + new Date());
            printWriter.println("Content-Type: text/plain");
            printWriter.println("Content-Length: 0");
            printWriter.println("Connection: close");
            printWriter.println();
            printWriter.flush();

            logResponse(200, "File uploaded successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getContentType(String fileName) {
        String contentType;

        if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) contentType = "image/jpeg";
        else if(fileName.endsWith(".png")) contentType = "image/png";
        else if(fileName.endsWith(".txt")) contentType = "text/plain";
        else contentType = "application/octet-stream";
        return contentType;
    }

    private void sendErrorResponse(int statusCode, String message) throws IOException {
        String response = "<html><body><h1>" + statusCode + message + "</h1></body></html>";
        printWriter.println("HTTP/1.1 " + statusCode + " " + message);
        printWriter.println("Server: Java HTTP Server: 1.0");
        printWriter.println("Date: " + new Date());
        printWriter.println("Content-Type: text/html");
        printWriter.println("Content-Length: " + response.length());
        printWriter.println("Connection: close");
        printWriter.println();
        printWriter.println(response);
        printWriter.flush();
        logResponse(statusCode, message);
    }

    private void sendHttpResponse(int statusCode, String contentType, String content) throws IOException {
        printWriter.println("HTTP/1.1 " + statusCode + "OK");
        printWriter.println("Server: Java HTTP Server: 1.0");
        printWriter.println("Date: " + new Date());
        printWriter.println("Content-Type: " + contentType);
        printWriter.println("Content-Length: " + content.length());
        printWriter.println("Connection: close");
        printWriter.println();
        printWriter.println(content);
        printWriter.flush();
        logResponse(statusCode, "OK");
    }

    private void logRequest(String requestLine) throws IOException {
        String req = "REQUEST: " + requestLine;
        bufferedWriter.write(req);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    private void logResponse(int statusCode, String message) throws IOException {
        String res = "RESPONSE: " + statusCode + " (" + message + ")";
        bufferedWriter.write(res);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }
}
