package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.nat.FutureNAT;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by johnson on 12/10/14.
 */
public class ChatRoomController implements Initializable{
    static Logger logger = LogManager.getLogger();
    static final int imageSize = 50;

    @FXML private TextField input;

    @FXML private Button shout;

    @FXML private ListView chatList;

    ObservableList<String> data = FXCollections.observableArrayList("lHello");


    String peerName;

    @Override @FXML
    public void initialize(URL location, ResourceBundle resources) {
        chatList.setItems(data);
        chatList.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            public ListCell call(ListView param) {
                return new ColorRectCell();
            }
        });
    }

    public void init(String peerName) {
        this.peerName = peerName;
    }

    @FXML
    private void handleShoutAction(ActionEvent actionEvent) {
        data.add("r" + input.getText());
        FuturePut futurePut = MyPeer.clientPeerDHT.add(Number160.createHash(peerName)).data(new Data(input.getText().getBytes())).start();
        futurePut.awaitUninterruptibly();
        logger.debug(futurePut.isSuccess());
    }

    static class ColorRectCell extends ListCell<String> {
        @Override
        public void updateItem(String item, boolean empty) {
            this.setStyle("-fx-background-color: transparent");
            if (item != null) {
                HBox hBox = new HBox();
                Text text = new Text(item.substring(1));
                ImageView imageView = createImageView("file:///home/johnson/Pictures/head.jpeg");
                hBox.setStyle("-fx-background-color: transparent");
                if (item.startsWith("l")) {
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.getChildren().addAll(imageView, text);
                    setGraphic(hBox);
                }
                else if (item.startsWith("r")) {
                    hBox.setAlignment(Pos.CENTER_RIGHT);
                    hBox.getChildren().addAll(text, imageView);
                    setGraphic(hBox);
                }
                else {
                    logger.error("expecting l/r leading error: " + item);
                }
            }
        }

        ImageView createImageView(String uri) {
            ImageView imageView = new ImageView();
            imageView.setImage(new Image(uri));
            imageView.setFitHeight(imageSize);
            imageView.setFitWidth(imageSize);
            return imageView;
        }
    }

}
