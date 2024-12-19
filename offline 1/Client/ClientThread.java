package Client;

import java.io.*;
import java.net.Socket;

public class ClientThread extends Thread {
    private final BufferedReader bufferedReader;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;
    private final PrintWriter printWriter;
    private final Socket serverSocket;
    private final String request;
    private final static int PORT = 5101;

    public ClientThread(String request) throws IOException {
        serverSocket = new Socket("localhost", PORT);
        bufferedReader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        inputStream = new DataInputStream(serverSocket.getInputStream());
        outputStream = new DataOutputStream(serverSocket.getOutputStream());
        printWriter = new PrintWriter(serverSocket.getOutputStream(), true);
        this.request = request;
    }

    public void uploadFile(File file) {
        try {

            printWriter.println(request);
            printWriter.flush();

            String serverResponse = bufferedReader.readLine();
            if (!"OK".equals(serverResponse)) {
                System.out.println("Server did not acknowledge the upload request");
                return;
            }
            System.out.println("Server acknowledged");

            outputStream.writeLong(file.length());
            outputStream.flush();

            System.out.println("Uploading file: " + file.getName());
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }

            }
            System.out.println("File upload completed.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        if (request.startsWith("UPLOAD")) {
            String[] str = request.split(" ");
            String fileName = str[1];
            File file = new File("./"+fileName);

            if (file.isDirectory()) {
                System.out.println("Directory cannot be uploaded!");
                printWriter.println("Directory cannot be uploaded!");
                printWriter.flush();
            } else if (!file.exists()) {
                System.out.println("File Not Found!");
                printWriter.println("File Not Found!");
                printWriter.flush();
            } else {
                uploadFile(file);
            }
        } else {
            printWriter.println(request);
            printWriter.flush();

            try {
                String res = bufferedReader.readLine();
                System.out.println(res);
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
