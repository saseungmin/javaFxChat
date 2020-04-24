package chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class Client implements Initializable {

	@FXML Button btnStart;
	@FXML TextArea txtDisplay;
	@FXML Button btnSend;
	@FXML TextField txtInput;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		btnStart.setOnAction(e->startAction(e));
		btnSend.setOnAction(e->sendAction(e));
	}

	SocketChannel sockC;
	@FXML TextField ClientName;
	@FXML TextField IPText;
	@FXML TextField PortText;
	void startClient(String IP,int port) {
		Thread thread=new Thread() {
			
			public void run() {
				try {
					sockC=SocketChannel.open();
					sockC.configureBlocking(true);
					sockC.connect(new InetSocketAddress(IP,port));
					
					Platform.runLater(()->{
						try {
							displayText("[채팅방 접속 : "+sockC.getRemoteAddress()+"]");
						btnStart.setText("stop");
						btnSend.setDisable(false);
						}catch(Exception e) {}
					});
				} catch (Exception e) {
					Platform.runLater(()->displayText("[서버 통신 안됨]"));
					if(sockC.isOpen()) {
						stopClient();
					}
					return;
				}
				receive();
			}
		};
		thread.start();
	}
	
	
	
	void stopClient() {
		try {
			Platform.runLater(()->{
				displayText("[연결 끊음]");
				btnStart.setText("start");
				btnSend.setDisable(true);
			});
			if(sockC!=null&&sockC.isOpen()) {
				sockC.close();
			}
		} catch (Exception e) {
		}
	}
	
	void receive() {
		while(true) {
			try {
				ByteBuffer byteB=ByteBuffer.allocate(100);
				
				int count=sockC.read(byteB);
				if(count==-1) {
					throw new IOException();
				}
				
				byteB.flip();
				Charset charset=Charset.forName("UTF-8");
				String data=charset.decode(byteB).toString();
				
				Platform.runLater(()->displayText(data));
			} catch (Exception e) {
				Platform.runLater(()->displayText("[서버 통신 안됨]"));
				stopClient();
				break;
			}
		}
	}
	
	void send(String data) {
		Thread thread =new Thread() {
			public void run() {
				try {
					Charset charset=Charset.forName("UTF-8");
					ByteBuffer byteB=charset.encode(data);
					sockC.write(byteB);
					Platform.runLater(()->displayText("[보내기 완료]"));
				} catch (Exception e) {
					Platform.runLater(()->displayText("[서버 통신 안됨]"));
					stopClient();
				}
			}
		};
		thread.start();
	}
	
	
	
	void displayText(String text) {
		txtDisplay.appendText(text+"\n");
	}
	
	@FXML public void startAction(ActionEvent e) {
		if(btnStart.getText().equals("start")) {
			int port=Integer.parseInt(PortText.getText());
			startClient(IPText.getText(), port);
		}else if(btnStart.getText().equals("stop")) {
			stopClient();
		}
	}

	@FXML public void sendAction(ActionEvent e) {
		send(ClientName.getText()+": "+txtInput.getText());
		txtInput.setText("");
		txtInput.requestFocus();
	}



	@FXML public void inputAction(ActionEvent e) {
		send(ClientName.getText()+": "+txtInput.getText());
		txtInput.setText("");
		txtInput.requestFocus();
	}

}
