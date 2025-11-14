package ECConnectFour;

import java.io.*;
import java.net.*;
import java.util.Date;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class Connect4Server extends Application {
    private int sessionNo = 1;

    @Override
    public void start(Stage primaryStage) {
        TextArea ta = new TextArea();
        Scene scene = new Scene(new ScrollPane(ta), 450, 200);
        primaryStage.setTitle("Connect4Server");
        primaryStage.setScene(scene);
        primaryStage.show();

        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(8000);
                Platform.runLater(() ->
                        ta.appendText("Server started at " + new Date() + '\n'));

                while (true) {
                    Platform.runLater(() ->
                            ta.appendText("Waiting for players...\n"));

                    Socket player1 = serverSocket.accept();
                    Platform.runLater(() ->
                            ta.appendText("Player 1 connected for session " + sessionNo + "\n"));

                    Socket player2 = serverSocket.accept();
                    Platform.runLater(() ->
                            ta.appendText("Player 2 connected for session " + sessionNo + "\n"));

                    Platform.runLater(() ->
                            ta.appendText("Starting session " + sessionNo + '\n'));

                    new Thread(new HandleSession(player1, player2, sessionNo++, ta)).start();
                }
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    class HandleSession implements Runnable {
        private Socket player1, player2;
        private int session;
        private TextArea ta;
        // Using 1D array like the working code: 48 squares (6 rows x 8 cols, but only use 7)
        private int[] board = new int[48];

        public HandleSession(Socket player1, Socket player2, int session, TextArea ta) {
            this.player1 = player1;
            this.player2 = player2;
            this.session = session;
            this.ta = ta;
        }

        public void run() {
            try {
                DataInputStream fromPlayer1 = new DataInputStream(player1.getInputStream());
                DataOutputStream toPlayer1 = new DataOutputStream(player1.getOutputStream());
                DataInputStream fromPlayer2 = new DataInputStream(player2.getInputStream());
                DataOutputStream toPlayer2 = new DataOutputStream(player2.getOutputStream());

                // Send player numbers
                toPlayer1.writeInt(1);
                toPlayer2.writeInt(2);

                int currentPlayer = 1;
                boolean continueToPlay = true;

                while (continueToPlay) {
                    if (currentPlayer == 1) {
                        // Tell player 1 it's their turn, tell player 2 to wait
                        toPlayer1.writeInt(1);
                        toPlayer2.writeInt(0);

                        // Get player 1's move (column clicked)
                        int column = fromPlayer1.readInt();
                        int location = legalMove(column, 1);

                        if (location == -1) {
                            // Invalid move
                            toPlayer1.writeInt(-1);
                            toPlayer1.writeInt(column);
                            toPlayer1.writeInt(0);
                            continue;
                        }

                        // Send actual location to both players
                        toPlayer1.writeInt(location);
                        toPlayer2.writeInt(location);

                        // Check game status
                        if (isWinner(location)) {
                            toPlayer1.writeInt(1); // Win
                            toPlayer2.writeInt(2); // Lose
                            continueToPlay = false;
                            Platform.runLater(() ->
                                    ta.appendText("Session " + session + ": Player 1 wins\n"));
                        } else if (boardFilledUp()) {
                            toPlayer1.writeInt(3); // Draw
                            toPlayer2.writeInt(3); // Draw
                            continueToPlay = false;
                            Platform.runLater(() ->
                                    ta.appendText("Session " + session + ": Draw\n"));
                        } else {
                            toPlayer1.writeInt(0); // Continue
                            toPlayer2.writeInt(0); // Continue
                            currentPlayer = 2;
                        }
                    } else {
                        // Tell player 2 it's their turn, tell player 1 to wait
                        toPlayer2.writeInt(1);
                        toPlayer1.writeInt(0);

                        // Get player 2's move
                        int column = fromPlayer2.readInt();
                        int location = legalMove(column, 2);

                        if (location == -1) {
                            // Invalid move
                            toPlayer2.writeInt(-1);
                            toPlayer2.writeInt(column);
                            toPlayer2.writeInt(0);
                            continue;
                        }

                        // Send actual location to both players
                        toPlayer2.writeInt(location);
                        toPlayer1.writeInt(location);

                        // Check game status
                        if (isWinner(location)) {
                            toPlayer2.writeInt(1); // Win
                            toPlayer1.writeInt(2); // Lose
                            continueToPlay = false;
                            Platform.runLater(() ->
                                    ta.appendText("Session " + session + ": Player 2 wins\n"));
                        } else if (boardFilledUp()) {
                            toPlayer2.writeInt(3); // Draw
                            toPlayer1.writeInt(3); // Draw
                            continueToPlay = false;
                            Platform.runLater(() ->
                                    ta.appendText("Session " + session + ": Draw\n"));
                        } else {
                            toPlayer2.writeInt(0); // Continue
                            toPlayer1.writeInt(0); // Continue
                            currentPlayer = 1;
                        }
                    }
                }
            } catch(IOException ex) {
                Platform.runLater(() ->
                        ta.appendText("Session " + session + " error: " + ex.getMessage() + "\n"));
            }
        }

        // Adapted from working code - drops piece with gravity
        private synchronized int legalMove(int column, int player) {
            // Bottom row for this column (row 5 = index 40+col)
            int minLocation = (column % 8) + 8 * 5;
            // Try from bottom to top
            for (int i = minLocation; i >= column; i -= 8) {
                if (board[i] == 0) {
                    board[i] = player;
                    return i;
                }
            }
            return -1; // Column full
        }

        // Win detection from working code
        private boolean isWinner(int location) {
            int player = board[location];

            // Horizontal check
            for (int j = 0; j < 9 - 4; j++) {
                for (int i = 0; i < 48; i += 8) {
                    if (board[i + j] != 0 && board[i + j] == board[i + j + 1] &&
                            board[i + j] == board[i + j + 2] && board[i + j] == board[i + j + 3]) {
                        return true;
                    }
                }
            }

            // Vertical check
            for (int i = 0; i < 24; i += 8) {
                for (int j = 0; j < 9; j++) {
                    if (board[i + j] != 0 && board[i + j] == board[i + 8 + j] &&
                            board[i + j] == board[i + 16 + j] && board[i + j] == board[i + 24 + j]) {
                        return true;
                    }
                }
            }

            // Ascending diagonal
            for (int i = 24; i < 48; i += 8) {
                for (int j = 0; j < 4; j++) {
                    if (board[i + j] != 0 && board[i + j] == board[(i - 8) + j + 1] &&
                            board[(i - 8) + j + 1] == board[i - 16 + j + 2] &&
                            board[(i - 16) + j + 2] == board[(i - 24) + j + 3]) {
                        return true;
                    }
                }
            }

            // Descending diagonal
            for (int i = 24; i < 48; i += 8) {
                for (int j = 3; j < 8; j++) {
                    if (board[i + j] != 0 && board[i + j] == board[(i - 8) + j - 1] &&
                            board[(i - 8) + j - 1] == board[(i - 16) + j - 2] &&
                            board[(i - 16) + j - 2] == board[(i - 24) + j - 3]) {
                        return true;
                    }
                }
            }

            return false;
        }

        private boolean boardFilledUp() {
            for (int i = 0; i < board.length; i++) {
                if (board[i] == 0) {
                    return false;
                }
            }
            return true;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}