import java.io.*;
import java.net.*;

public class ServerThread extends Thread {

    private final Socket socket;

    BufferedReader  reader;
    PrintWriter     writer;
    ServerLogic     serverLogic;
    boolean         keepConnectionAlive;

    private static final String SYNTAX_ERROR_MESSAGE = "301 SYNTAX ERROR\u0007\b";
    private static final String SERVER_LOGOUT = "106 LOGOUT\u0007\b";
    private static final String SERVER_LOGIN_FAILED = "300 LOGIN FAILED\u0007\b";
    private static final String SERVER_KEY_OUT_OF_RANGE_ERROR = "303 KEY OUT OF RANGE\u0007\b";

    private static final int MIN_LENGTH_FOR_RECHARGING = 12;

    private static final String CLIENT_RECHARGING = "RECHARGING\u0007\b";
    private static final String CLIENT_FULL_POWER = "FULL POWER\u0007\b";

    private static final String SERVER_LOGIC_ERROR = "302 LOGIC ERROR\u0007\b";

    private static final String ESCAPE_SEQUENCE = "\u0007\b";

    public ServerThread(Socket socket) throws IOException {
        this.socket = socket;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        serverLogic = new ServerLogic();
        keepConnectionAlive = true;
    }

    public void run() {
        try {
            String server_message;
            for(;;) {
                socket.setSoTimeout(serverLogic.getTimeout());
                StringBuilder message = getMessage();

                if (!(message + ESCAPE_SEQUENCE).equals(CLIENT_RECHARGING) && !(message + ESCAPE_SEQUENCE).equals(CLIENT_FULL_POWER)) {
                    if (!serverLogic.isMessageValid(message)) {
                        keepConnectionAlive = false;
                    }
                }

                System.out.println("C: " + message.toString());

                if (!keepConnectionAlive)
                {
                    writer.write(serverLogic.getSyntaxErrorMessage());
                    writer.flush();
                    break;
                }

                server_message = serverLogic.processMessage(message.toString());
                writer.write(server_message);
                writer.flush();
                System.out.println("S: " + server_message);;

                if (server_message.equals(SERVER_LOGOUT) ||
                        server_message.equals(SERVER_LOGIN_FAILED) ||
                        server_message.equals(SYNTAX_ERROR_MESSAGE) ||
                        server_message.equals(SERVER_LOGIC_ERROR) ||
                        server_message.equals(SERVER_KEY_OUT_OF_RANGE_ERROR)  )
                    break;
            }
            socket.close();
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
            try {
                socket.close();
                reader.close();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public StringBuilder getMessage() throws IOException {
        StringBuilder message = new StringBuilder("");
        int currentSymbol = reader.read();
        message.append((char)currentSymbol);
        for(;;)
        {
            if (currentSymbol == '\u0007')
            {
                currentSymbol = reader.read();
                message.append((char)currentSymbol);
                if (message.length() > MIN_LENGTH_FOR_RECHARGING)
                {
                    if (!serverLogic.isMessageValid(message)) {
                        keepConnectionAlive = false;
                        break;
                    }
                }
                if (currentSymbol == '\b') {
                    if (message.length() > MIN_LENGTH_FOR_RECHARGING)
                    {
                        if (!serverLogic.isMessageValid(message)) {
                            keepConnectionAlive = false;
                            break;
                        }
                    }
                    message.deleteCharAt(message.length() - 1);
                    message.deleteCharAt(message.length() - 1);
                    break;
                }
            }
            else {
                currentSymbol = reader.read();
                message.append((char)currentSymbol);
                if (message.length() > MIN_LENGTH_FOR_RECHARGING)
                {
                    if (!serverLogic.isMessageValid(message)) {
                        keepConnectionAlive = false;
                        break;
                    }
                }
            }
        }
        return message;
    }

}