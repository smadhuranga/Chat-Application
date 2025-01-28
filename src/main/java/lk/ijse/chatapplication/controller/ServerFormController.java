package lk.ijse.chatapplication.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerFormController {
    @FXML
    private VBox chatBox;

    @FXML
    private javafx.scene.control.TextField txtFeild;

    @FXML
    private ScrollPane scrollPane;


    private ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public void initialize() {
        startServer();
    }

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(5001);
                socket = serverSocket.accept();

                addMessageToChat("Client", "Acccepted");
                if (socket.isClosed()) {
                    addMessageToChat("Client", "Disconnected");
                }

                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                new Thread(this::listenForMessages).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void listenForMessages() {
        try {
            while (true) {
                String messageType = dataInputStream.readUTF();
                if (messageType.equals("text")) {
                    String message = dataInputStream.readUTF();
                    addMessageToChat("Client", message);
                } else if (messageType.equals("image")) {
                    int imageSize = dataInputStream.readInt();
                    byte[] imageBytes = new byte[imageSize];
                    dataInputStream.readFully(imageBytes);
                    javafx.scene.image.Image image = new javafx.scene.image.Image(new ByteArrayInputStream(imageBytes));
                    addImageToChat("Client", image);
                } else if (messageType.equals("file")) {
                    String fileName = dataInputStream.readUTF();
                    int fileSize = dataInputStream.readInt();
                    byte[] fileBytes = new byte[fileSize];
                    dataInputStream.readFully(fileBytes);
                    addFileToChat("Client", fileName, fileBytes);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void btnSendOnAction(javafx.event.ActionEvent event) {
        try {
            String msg = txtFeild.getText();
            dataOutputStream.writeUTF("text");
            dataOutputStream.writeUTF(msg);
            dataOutputStream.flush();
            addMessageToChat("Server", msg);
            txtFeild.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void addMessageToChat(String sender, String message) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Label label = new javafx.scene.control.Label(sender + ": " + message);
            label.setStyle("-fx-background-color: #c4c8df; -fx-padding: 5; -fx-background-radius: 12;");
            chatBox.getChildren().add(label);
            scrollPane.setVvalue(1.0);
        });
    }


    @FXML
    void btnSendImageOnAction(javafx.event.ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                BufferedImage bufferedImage = ImageIO.read(selectedFile);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                dataOutputStream.writeUTF("image");
                dataOutputStream.writeInt(imageBytes.length);
                dataOutputStream.write(imageBytes);
                dataOutputStream.flush();

                javafx.scene.image.Image image = new javafx.scene.image.Image(new ByteArrayInputStream(imageBytes));
                addImageToChat("Server", image);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void addImageToChat(String sender, Image image) {
        javafx.application.Platform.runLater(() -> {
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(100);
            imageView.setFitWidth(100);
            javafx.scene.control.Label label = new javafx.scene.control.Label(sender + ": Sent an image.");
            label.setStyle("-fx-background-color: #5675a8; -fx-padding: 5; -fx-background-radius: 12;");
            chatBox.getChildren().addAll(label, imageView);
            scrollPane.setVvalue(1.0);
        });
    }

    @FXML
    void btnSendFileOnAction(javafx.event.ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                byte[] fileBytes = new byte[(int) selectedFile.length()];
                FileInputStream fileInputStream = new FileInputStream(selectedFile);
                fileInputStream.read(fileBytes);
                fileInputStream.close();

                dataOutputStream.writeUTF("file");
                dataOutputStream.writeUTF(selectedFile.getName());
                dataOutputStream.writeInt(fileBytes.length);
                dataOutputStream.write(fileBytes);
                dataOutputStream.flush();

                addFileToChat("Server", selectedFile.getName(), fileBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addFileToChat(String sender, String fileName, byte[] fileBytes) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Label label = new javafx.scene.control.Label(sender + ": Sent a file - " + fileName);
            label.setStyle("-fx-background-color: #9cb0c6; -fx-padding: 5; -fx-background-radius: 12;");
            chatBox.getChildren().add(label);
            scrollPane.setVvalue(1.0);
        });
    }

}
