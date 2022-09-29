public class Coordinates {

    private static final int INIT_X = 1000000000;
    private static final int INIT_Y = 1000000000;

    private static final String SERVER_MOVE = "102 MOVE\u0007\b";
    private static final String SERVER_TURN_LEFT = "103 TURN LEFT\u0007\b";
    private static final String SERVER_TURN_RIGHT = "104 TURN RIGHT\u0007\b";

    boolean isBypassing;
    boolean isLastWasMove;

    private int currentBypassingStage;

    private Direction direction;

    int x;
    int y;

    public Coordinates(){
        x = INIT_X;
        y = INIT_Y;
        isBypassing = false;
        currentBypassingStage = 0;
        isLastWasMove = false;
    }

    public Coordinates(int x, int y){
        this.x = x;
        this.y = y;
        isBypassing = false;
        currentBypassingStage = 0;
        isLastWasMove = false;
    }

    public void setCoordinates(int x, int y) {
        if (this.x == x && this.y == y && isLastWasMove)
        {
            isBypassing = true;
            return;
        }
        this.x = x;
        this.y = y;
    }

    public boolean isInitValue() {
        return this.x == INIT_X && this.y == INIT_Y;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public String getNextMove() {
        if (!isBypassing)
            return getMove();

        switch (currentBypassingStage) {
            case 0 -> {
                direction = turnLeft();
                isLastWasMove = false;
                currentBypassingStage += 1;
                return SERVER_TURN_LEFT;
            }
            case 1, 3, 4 -> {
                currentBypassingStage += 1;
                isLastWasMove = true;
                return SERVER_MOVE;
            }
            case 2 -> {
                direction = turnRight();
                isLastWasMove = false;
                currentBypassingStage += 1;
                return SERVER_TURN_RIGHT;
            }
            default -> {
                currentBypassingStage = 0;
                isBypassing = false;
                isLastWasMove = true;
                return getMove();
            }
        }

    }

    public Direction turnLeft() {
        if (direction == Direction.LEFT) return Direction.DOWN;
        if (direction == Direction.DOWN) return Direction.RIGHT;
        if (direction == Direction.RIGHT) return Direction.UP;
        if (direction == Direction.UP) return Direction.LEFT;
        return Direction.NONE;
    }

    public Direction turnRight() {
        if (direction == Direction.LEFT) return Direction.UP;
        if (direction == Direction.DOWN) return Direction.LEFT;
        if (direction == Direction.RIGHT) return Direction.DOWN;
        if (direction == Direction.UP) return Direction.RIGHT;
        return Direction.NONE;
    }

    boolean isReached() {
        return this.x == 0 && this.y == 0;
    }

    public String getMove(){
        if (x > 0 && direction != Direction.LEFT)
        {
            isLastWasMove = false;

            switch (direction) {
                case UP -> {
                    direction = Direction.LEFT;
                    return SERVER_TURN_LEFT;
                }
                case RIGHT -> {
                    direction = Direction.UP;
                    return SERVER_TURN_LEFT;
                }
                case DOWN -> {
                    direction = Direction.LEFT;
                    return SERVER_TURN_RIGHT;
                }
            }
        }
        else if (x < 0 && direction != Direction.RIGHT)
        {
            isLastWasMove = false;

            switch (direction) {
                case UP -> {
                    direction = Direction.RIGHT;
                    return SERVER_TURN_RIGHT;
                }
                case LEFT -> {
                    direction = Direction.UP;
                    return SERVER_TURN_RIGHT;
                }
                case DOWN -> {
                    direction = Direction.RIGHT;
                    return SERVER_TURN_LEFT;
                }
            }
        }
        else if (x != 0) {
            isLastWasMove = true;
            return SERVER_MOVE;
        }
        else if (y < 0 && direction != Direction.UP)
        {
            isLastWasMove = false;

            switch (direction) {
                case RIGHT -> {
                    direction = Direction.UP;
                    return SERVER_TURN_LEFT;
                }
                case LEFT -> {
                    direction = Direction.UP;
                    return SERVER_TURN_RIGHT;
                }
                case DOWN -> {
                    direction = Direction.RIGHT;
                    return SERVER_TURN_LEFT;
                }
            }
        }
        else if (y > 0 && direction != Direction.DOWN)
        {
            isLastWasMove = false;

            switch (direction) {
                case RIGHT -> {
                    direction = Direction.DOWN;
                    return SERVER_TURN_RIGHT;
                }
                case LEFT -> {
                    direction = Direction.DOWN;
                    return SERVER_TURN_LEFT;
                }
                case UP -> {
                    direction = Direction.RIGHT;
                    return SERVER_TURN_RIGHT;
                }
            }
        }
        isLastWasMove = true;
        return SERVER_MOVE;
    }

    public String getDirection() {
        return direction.toString();
    }

}