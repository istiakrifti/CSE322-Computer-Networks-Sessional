package Client;

import java.io.IOException;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        Scanner input = new Scanner(System.in);

        while (true) {
            new ClientThread(input.nextLine()).start();
        }
    }
}
