package ECConnectFour;

import java.io.*;
import java.net.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class Connect4Client extends Application {
    private DataOutputStream toServer;
    private DataInputStream fromServer;
    private boolean myTurn = false;
    private int myPlayer;
    private Circle[] circles = new Circle[48]; // 6x8 grid like working code
    private Label statusLabel = new Label();
    private boolean gameOver = false;

    @Override
    public void start(Stage primaryStage) {
        BorderPane pane = new BorderPane();

        GridPane board = new GridPane();
        board.setAlignment(Pos.CENTER);
        board.setStyle("-fx-background-color: blue; -fx-padding: 10;");

        // Create 6x7 grid matching server's 1D array: indices 0-7, 8-15, 16-23, 24-31, 32-39, 40-47
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                int location = row * 8 + col; // Map to server's 1D array

                Pane cell = new Pane();
                cell.setStyle("-fx-border-color: black;");
                cell.setPrefSize(60, 60);

                Circle circle = new Circle(25);
                circle.setCenterX(30);
                circle.setCenterY(30);
                circle.setFill(Color.WHITE);
                circles[location] = circle;
                cell.getChildren().add(circle);

                int clickCol = col;
                cell.setOnMouseClicked(e -> {
                    if (!gameOver) {
                        // Send the column number (0-6)
                        handleClick(clickCol);
                    }
                });

                board.add(cell, col, row);
            }
        }

        statusLabel.setText("Waiting to connect...");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-padding: 10;");
        pane.setTop(statusLabel);
        pane.setCenter(board);

        Scene scene = new Scene(pane, 500, 450);
        primaryStage.setTitle("Connect4Client");
        primaryStage.setScene(scene);
        primaryStage.show();

        connectToServer();
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 8000);
                fromServer = new DataInputStream(socket.getInputStream());
                toServer = new DataOutputStream(socket.getOutputStream());

                myPlayer = fromServer.readInt();
                String colorName = (myPlayer == 1) ? "RED" : "YELLOW";
                Platform.runLater(() ->
                        statusLabel.setText("You are player " + myPlayer + " (" + colorName + "). Waiting for game to start..."));

                while (!gameOver) {
                    int turnStatus = fromServer.readInt();

                    if (turnStatus == 1) {
                        // It's my turn
                        myTurn = true;
                        Platform.runLater(() ->
                                statusLabel.setText("Your turn - Click a column"));
                        // Wait for user to click
                    } else {
                        // turnStatus == 0, wait for opponent's move
                        myTurn = false;
                        Platform.runLater(() ->
                                statusLabel.setText("Waiting for opponent..."));

                        int location = fromServer.readInt();
                        int result = fromServer.readInt();

                        Platform.runLater(() -> {
                            if (location >= 0 && location < 48) {
                                // Show opponent's color
                                Color opponentColor = (myPlayer == 1) ? Color.YELLOW : Color.RED;
                                circles[location].setFill(opponentColor);
                            }

                            if (result == 1) {
                                statusLabel.setText("You win!");
                                gameOver = true;
                            } else if (result == 2) {
                                statusLabel.setText("You lose");
                                gameOver = true;
                            } else if (result == 3) {
                                statusLabel.setText("Draw");
                                gameOver = true;
                            }
                        });

                        if (result != 0) {
                            break;
                        }
                    }
                }
            } catch (IOException ex) {
                Platform.runLater(() ->
                        statusLabel.setText("Connection error: " + ex.getMessage()));
                ex.printStackTrace();
            }
        }).start();
    }

    private void handleClick(int col) {
        if (myTurn && !gameOver) {
            try {
                toServer.writeInt(col);
                toServer.flush();

                int location = fromServer.readInt();
                int result = fromServer.readInt();

                Platform.runLater(() -> {
                    if (location == -1) {
                        // Invalid move - column full
                        statusLabel.setText("Column full! Try another column");
                        return;
                    }

                    // Show my color at the returned location
                    Color myColor = (myPlayer == 1) ? Color.RED : Color.YELLOW;
                    circles[location].setFill(myColor);

                    if (result == 1) {
                        statusLabel.setText("You win!");
                        gameOver = true;
                    } else if (result == 2) {
                        statusLabel.setText("You lose");
                        gameOver = true;
                    } else if (result == 3) {
                        statusLabel.setText("Draw");
                        gameOver = true;
                    } else {
                        statusLabel.setText("Waiting for opponent...");
                    }
                });

                myTurn = false;
            } catch (IOException ex) {
                Platform.runLater(() ->
                        statusLabel.setText("Error sending move: " + ex.getMessage()));
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}