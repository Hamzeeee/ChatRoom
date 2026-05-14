import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket client;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;

    public ClientHandler(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        setupUsername();
    }

    // Adds the user after setup in the 'clients' list, to check for the user's alive status
    public void setupUsername() {
        try {
            printWriter = new PrintWriter(client.getOutputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));

            String username;
            do {
                printWriter.print("Enter username: ");
                printWriter.flush();
                username = bufferedReader.readLine();
            } while (username.isBlank());

            Main.getInstance().clients.add(client);
            Main.getInstance().broadCastMessage(username + " has joined this chat!", client);

            while (true) {
                writeToChat();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeToChat() {
        try {
            printWriter.print(" > ");
            printWriter.flush();
            String message = bufferedReader.readLine();
            Main.getInstance().broadCastMessage(message, client);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
