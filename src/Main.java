import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    // BUG If user sends a message amid other users' prompt, the content won't be displayed due to influence limitation.

    private final int PERIOD_FOR_SCHEDULER = 1;
    private final int PORT = 12346;
    private final int GUEST_COUNT = 32;

    public final ArrayList<Socket> clients = new ArrayList<>();
    private final ExecutorService clientHandling = Executors.newFixedThreadPool(GUEST_COUNT);

    public static Main instance;

    public static void main(String[] args) {
        instance = new Main();
        ScheduledExecutorService checkUserAliveStatusThread = Executors.newSingleThreadScheduledExecutor();
        checkUserAliveStatusThread.scheduleAtFixedRate(() -> instance.checkUserAliveStatus(instance.clients), 0, instance.PERIOD_FOR_SCHEDULER, TimeUnit.SECONDS);
        try {
            ServerSocket serverSocket = new ServerSocket(getInstance().PORT);
            while (true) {
                Socket client = serverSocket.accept();
                instance.clientHandling.submit(new ClientHandler(client));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Main getInstance() {
        return instance;
    }


    /**
     * A broadcasting message is sent to every client, except to user itself, to avoid a buggy UI
     *
     * @param message
     * @param client
     */
    public void broadCastMessage(String message, Socket sender) {
        clients.forEach(client -> {

            try {
                PrintWriter writer = new PrintWriter(client.getOutputStream());

                if (client.equals(sender)) {
                    return;
                }
                if (message.isBlank()) return;

                // Clear the current line to make room for the displayed message
                writer.print("\r");
                writer.println(message);
                writer.print(" > ");
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }

    /**
     * It sends a null byte (0x00) to every user in the list to check for their status. User with failed connection will be removed from the list and considered disconnected.
     *
     * @param users
     * @throws IOException
     */
    public void checkUserAliveStatus(ArrayList<Socket> users) {
        Iterator<Socket> user = users.iterator();

        while (user.hasNext()) {
            Socket client = user.next();
            try {
                client.getOutputStream().write(0);
                client.getOutputStream().flush();
            } catch (IOException e) {
                System.out.println("User " + client.getInetAddress() + " left the server.");
                broadCastMessage("User " + client.getInetAddress() + " left the server.", null);
                user.remove();
            }
        }
    }
}