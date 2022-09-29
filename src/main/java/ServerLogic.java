import java.util.Objects;


public class ServerLogic {

    private static final int TIMEOUT = 1;
    private static final int TIMEOUT_RECHARGING = 5;

    private static final int MODULO = 65536;

    private static final int[] SERVER_KEY = {
            23019, 32037, 18789, 16443, 18189
    };

    private static final int[] CLIENT_KEY = {
            32037, 29295, 13603, 29533, 21952
    };

    private static final String ESCAPE_SEQUENCE = "\u0007\b";

    private static final int MAX_USERNAME_LENGTH = 20;
    private static final int MAX_KEY_ID_LENGTH = 5;
    private static final int MAX_CLIENT_CONFIRMATION_LENGTH = 7;
    private static final int MAX_CLIENT_OK_LENGTH = 12;
    private static final int MAX_CLIENT_MESSAGE_LENGTH = 100;

    private static final String SYNTAX_ERROR_MESSAGE = "301 SYNTAX ERROR\u0007\b";

    private static final String SERVER_KEY_REQUEST = "107 KEY REQUEST\u0007\b";
    private static final String SERVER_OK = "200 OK\u0007\b";
    private static final String SERVER_LOGIN_FAILED = "300 LOGIN FAILED\u0007\b";
    private static final String SERVER_SYNTAX_ERROR = "301 SYNTAX ERROR\u0007\b";
    private static final String SERVER_LOGIC_ERROR = "302 LOGIC ERROR\u0007\b";
    private static final String SERVER_KEY_OUT_OF_RANGE_ERROR = "303 KEY OUT OF RANGE\u0007\b";

    private static final String SERVER_MOVE = "102 MOVE\u0007\b";
    private static final String SERVER_TURN_LEFT = "103 TURN LEFT\u0007\b";
    private static final String SERVER_TURN_RIGHT = "104 TURN RIGHT\u0007\b";
    private static final String SERVER_PICK_UP = "105 GET MESSAGE\u0007\b";
    private static final String SERVER_LOGOUT = "106 LOGOUT\u0007\b";
    private static final String SERVER_EMPTY_MESSAGE = "";

    private static final String CLIENT_RECHARGING = "RECHARGING\u0007\b";
    private static final String CLIENT_FULL_POWER = "FULL POWER\u0007\b";

    private static final int RECHARGING_LENGTH = 12;


    private ServerState serverState;
    private ServerState stateBeforeCharging;
    private String username;
    private int clientKey;
    private int serverKey;
    private int usernameHash;
    private boolean isDirectionSet;
    private boolean wasTurned;
    private Coordinates firstCoords;
    private Coordinates coordinates;

    public ServerLogic() {
        this.serverState = ServerState.CLIENT_USERNAME;
        this.stateBeforeCharging = this.serverState;
        this.firstCoords = new Coordinates();
        this.isDirectionSet = false;
        this.wasTurned = false;
    }

    public int getTimeout() {
        return (serverState == ServerState.CLIENT_CHARGING ? 5 : 1) * 1000;
    }

    public boolean isMessageValid(StringBuilder msg) {

        if (serverState == ServerState.CLIENT_USERNAME)     return (checkMessageLength(msg, MAX_USERNAME_LENGTH));
        if (serverState == ServerState.CLIENT_KEY_ID)       return (checkMessageLength(msg, MAX_KEY_ID_LENGTH));
        if (serverState == ServerState.CLIENT_CONFIRMATION) return (checkMessageLength(msg, MAX_CLIENT_CONFIRMATION_LENGTH));
        if (serverState == ServerState.CLIENT_OK)           return (checkMessageLength(msg, MAX_CLIENT_OK_LENGTH));
        if (serverState == ServerState.CLIENT_MESSAGE)      return (checkMessageLength(msg, MAX_CLIENT_MESSAGE_LENGTH));
        if (serverState == ServerState.CLIENT_CHARGING)     return (checkMessageLength(msg, RECHARGING_LENGTH));
        return false;
    }

    public boolean checkMessageLength(StringBuilder message, int MAX_LENGTH) {
        if (serverState == ServerState.CLIENT_CHARGING)
        {
            MAX_LENGTH = switch (stateBeforeCharging) {
                case CLIENT_USERNAME -> MAX_USERNAME_LENGTH;
                case CLIENT_KEY_ID -> MAX_KEY_ID_LENGTH;
                case CLIENT_CONFIRMATION -> MAX_CLIENT_CONFIRMATION_LENGTH;
                case CLIENT_OK -> MAX_CLIENT_OK_LENGTH;
                case CLIENT_MESSAGE -> MAX_CLIENT_MESSAGE_LENGTH;
                default -> MAX_LENGTH;
            };
        }
        if (message.length() >= MAX_LENGTH && message.length() <= RECHARGING_LENGTH)
        {
            if (message.toString().equals(CLIENT_RECHARGING.substring(0, message.length())))
                return true;
        }
        if (message.length() > MAX_LENGTH)
            return false;
        if (message.length() == MAX_LENGTH && message.charAt(MAX_LENGTH - 2) != '\u0007' & message.charAt(MAX_LENGTH - 1) != '\b') {
            return false;
        }
        if (message.length() == MAX_LENGTH - 1 && (message.charAt(MAX_LENGTH - 3) != '\u0007' && message.charAt(MAX_LENGTH - 2) != '\b' &&
                message.charAt(MAX_LENGTH - 2) != '\u0007')) {
            return false;
        }
        return true;
    }

    public boolean validateMessage(String message) {
        try {
            Integer.parseInt(message);
            return true;
        } catch (NumberFormatException ex){
            return false;
        }
    }

    public String getSyntaxErrorMessage() {
        return SYNTAX_ERROR_MESSAGE;
    }

    public String processMessage(String message) {

        if (serverState == ServerState.CLIENT_CHARGING && !Objects.equals(message + ESCAPE_SEQUENCE, CLIENT_FULL_POWER)) {
            return SERVER_LOGIC_ERROR;
        }
        if (Objects.equals(message + ESCAPE_SEQUENCE, CLIENT_RECHARGING))
        {
            stateBeforeCharging = serverState;
            serverState = ServerState.CLIENT_CHARGING;
            return SERVER_EMPTY_MESSAGE;
        }
        if (Objects.equals(message + ESCAPE_SEQUENCE, CLIENT_FULL_POWER)) {
            serverState = stateBeforeCharging;
            return SERVER_EMPTY_MESSAGE;
        }
        if (serverState == ServerState.CLIENT_USERNAME)
        {
            serverState = ServerState.CLIENT_KEY_ID;
            username = message;
            return SERVER_KEY_REQUEST;
        }
        else if (serverState == ServerState.CLIENT_KEY_ID)
        {
            serverState = ServerState.CLIENT_CONFIRMATION;
            if (!validateMessage(message))
                return SERVER_SYNTAX_ERROR;
            if (isClientKeyOutOfRange(message))
                return SERVER_KEY_OUT_OF_RANGE_ERROR;
            clientKey = getClientKey(message);
            serverKey = getServerKey(message);
            setUsernameHash();
            return Integer.toString((getUsernameHash() + serverKey) % MODULO) + ESCAPE_SEQUENCE;
        }
        else if (serverState == ServerState.CLIENT_CONFIRMATION)
        {
            serverState = ServerState.CLIENT_OK;
            if (!validateMessage(message))
                return SYNTAX_ERROR_MESSAGE;
            if ((getUsernameHash() + clientKey) % MODULO == getKeyFromClientConfirmation(message))
                return SERVER_OK + SERVER_MOVE;
            else
                return SERVER_LOGIN_FAILED;
        }
        else if (serverState == ServerState.CLIENT_OK)
        {
            if (Objects.equals(message + ESCAPE_SEQUENCE, CLIENT_RECHARGING))
            {
                serverState = ServerState.CLIENT_CHARGING;
                return SERVER_EMPTY_MESSAGE;
            }
            String[] arrOfStr = message.split(" ", -1);
            if (arrOfStr.length != 3 || !Objects.equals(arrOfStr[0], "OK") ||
                    !validateMessage(arrOfStr[1]) || !validateMessage(arrOfStr[2])) {
                return SYNTAX_ERROR_MESSAGE;
            }
            if (firstCoords.isInitValue()) {
                firstCoords.setCoordinates(Integer.parseInt(arrOfStr[1].trim()), Integer.parseInt(arrOfStr[2].trim()));
                if (firstCoords.isReached()) {
                    serverState = ServerState.CLIENT_MESSAGE;
                    return SERVER_PICK_UP;
                }
                return SERVER_MOVE;
            }
            else if (!isDirectionSet) {
                coordinates = new Coordinates(Integer.parseInt(arrOfStr[1].trim()), Integer.parseInt(arrOfStr[2].trim()));
                if (!isCoordsChanged() && !wasTurned) {
                    wasTurned = true;
                    return SERVER_TURN_LEFT;
                }
                if (!isCoordsChanged() && wasTurned)
                    return SERVER_MOVE;
                setDirection();
                System.out.println("Direction set to the " + coordinates.getDirection());
            }
            coordinates.setCoordinates(Integer.parseInt(arrOfStr[1].trim()), Integer.parseInt(arrOfStr[2].trim()));
            if (coordinates.x == 0 && coordinates.y == 0) {
                serverState = ServerState.CLIENT_MESSAGE;
                return SERVER_PICK_UP;
            }
            return coordinates.getNextMove();
        }
        else if (serverState == ServerState.CLIENT_CHARGING) {
            if (!Objects.equals(message + ESCAPE_SEQUENCE, CLIENT_FULL_POWER)) {
                return SERVER_LOGIC_ERROR;
            }
            else
                serverState = ServerState.CLIENT_OK;

            return SERVER_MOVE;
        }
        else if (serverState == ServerState.CLIENT_MESSAGE) {
            serverState = ServerState.CLIENT_LOGOUT;
            return SERVER_LOGOUT;
        }
        else if (serverState == ServerState.CLIENT_LOGOUT)
            return SERVER_LOGOUT;

        return SERVER_EMPTY_MESSAGE;
    }

    public boolean isClientKeyOutOfRange(String message) {
        int key = Integer.parseInt(message);
        if (key < 0 || key > 4)
            return true;
        clientKey = key;
        return false;
    }

    public int getClientKey(String message) {
        return CLIENT_KEY[(int)message.charAt(0) - '0'];
    }

    public int getServerKey(String message) {
        return SERVER_KEY[(int)message.charAt(0) - '0'];
    }

    public void setUsernameHash() {
        for (int i = 0; i < username.length(); i++)
            usernameHash += username.charAt(i);
        usernameHash = (usernameHash * 1000) % MODULO;
    }

    public int getUsernameHash() {
        return usernameHash;
    }

    public int getKeyFromClientConfirmation(String message) {
        return Integer.parseInt(message);
    }

    public void setDirection() {
        isDirectionSet = true;
        if (coordinates.x - firstCoords.x == -1 && coordinates.y - firstCoords.y == 0)
            coordinates.setDirection(Direction.LEFT);
        else if (coordinates.x - firstCoords.x == 1 && coordinates.y - firstCoords.y == 0)
            coordinates.setDirection(Direction.RIGHT);
        else if (coordinates.x - firstCoords.x == 0 && coordinates.y - firstCoords.y == 1)
            coordinates.setDirection(Direction.UP);
        else if (coordinates.x - firstCoords.x == 0 && coordinates.y - firstCoords.y == -1)
            coordinates.setDirection(Direction.DOWN);
    }

    public boolean isCoordsChanged() {
        return !(firstCoords.x == coordinates.x && firstCoords.y == coordinates.y);
    }

}