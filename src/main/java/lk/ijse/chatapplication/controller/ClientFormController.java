package lk.ijse.chatapplication.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

public class ClientFormController {
    @FXML
    private TextField name;

    @FXML
    private VBox chatBox;

    @FXML
    private TextField txtFeild;

    @FXML
    private ScrollPane scrollPane;

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;


    public void initialize() {
        connectToServer();
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 5001);

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
                    addMessageToChat("Server", message);
                } else if (messageType.equals("image")) {
                    int imageSize = dataInputStream.readInt();
                    byte[] imageBytes = new byte[imageSize];
                    dataInputStream.readFully(imageBytes);
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    addImageToChat("Server", image);
                } else if (messageType.equals("file")) {
                    String fileName = dataInputStream.readUTF();
                    int fileSize = dataInputStream.readInt();
                    byte[] fileBytes = new byte[fileSize];
                    dataInputStream.readFully(fileBytes);
                    addFileToChat("Server", fileName, fileBytes);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    @FXML
    void btnSendOnAction(ActionEvent event) {
        try {
            String msg = txtFeild.getText();
            String sendername = name.getText();
            dataOutputStream.writeUTF("text");
            dataOutputStream.writeUTF(msg);
            dataOutputStream.writeUTF(sendername);
            dataOutputStream.flush();
            addMessageToChat(sendername, msg);
            txtFeild.clear();
            name.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addMessageToChat(String sender, String message) {
        javafx.application.Platform.runLater(() -> {
            Label label = new Label(sender + ": " + message);
            label.setStyle("-fx-background-color: #8188b1; -fx-padding: 5; -fx-background-radius: 12;");
            chatBox.getChildren().add(label);
            scrollPane.setVvalue(1.0);
        });
    }

    @FXML
    void btnSendImageOnAction(ActionEvent event) {
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

                Image image = new Image(new ByteArrayInputStream(imageBytes));
                addImageToChat("Client", image);
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
            Label label = new Label(sender + ": Sent an image.");
            label.setStyle("-fx-background-color: #717fb6; -fx-padding: 5; -fx-background-radius: 12;");
            chatBox.getChildren().addAll(label, imageView);
            scrollPane.setVvalue(1.0);
        });
    }


    @FXML
    void btnSendFileOnAction(ActionEvent event) {
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

                addFileToChat("Client", selectedFile.getName(), fileBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void addFileToChat(String sender, String fileName, byte[] fileBytes) {
        javafx.application.Platform.runLater(() -> {
            Label label = new Label(sender + ": Sent a file - " + fileName);
            label.setStyle("-fx-background-color: #5c7eaf; -fx-padding: 5; -fx-background-radius: 12;");
            chatBox.getChildren().add(label);
            scrollPane.setVvalue(1.0);
        });
    }

    public void disconnet(ActionEvent actionEvent) throws IOException {
        String error = "Cannot Connect eny more.. Please re run Application";
        addMessageToChat("Sever", error);
        dataInputStream.close();
        dataOutputStream.close();


    }

}