package chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;


public class Server implements Initializable{
	
	@FXML TextArea txtDisplay;
	@FXML Button btnStart;
	
	ExecutorService eService; //스레드 풀선언
	ServerSocketChannel serverSock;
	List<Client> connect=new Vector<Client>();
	
	
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		btnStart.setOnAction(e->startAction(e));
	}
	
	void startServer() {
		eService=Executors.newFixedThreadPool(
				Runtime.getRuntime().availableProcessors() // cpu 코어의 수에 맞게 스레드 생성
				);
		
		try {
			serverSock=ServerSocketChannel.open();
			serverSock.configureBlocking(true); //블로킹 채널
			serverSock.bind(new InetSocketAddress(8080));
		} catch (Exception e) {
			if(serverSock.isOpen()) {
				stopServer();
			}
			return;
		}
		
		Runnable runnable=new Runnable() {
			
			@Override
			public void run() {
				Platform.runLater(()->{
					displayText("[서버 시작]");
					btnStart.setText("stop");
				});
				while(true) {
					try {
						SocketChannel sockC=serverSock.accept();
						//클라이언트 ip와 쓰레드이름 문자열 생성
						String message="[연결 수락: "+sockC.getRemoteAddress()+": "+Thread.currentThread().getName()+"]";
						Platform.runLater(()->displayText(message));
						
						Client client=new Client(sockC); //socketChannel로 객체 생성
						connect.add(client); //connections 컬랙션에 추가
						
						Platform.runLater(()->displayText("[연결 개수: "+connect.size()+"]"));
					} catch (Exception e) {
						if(serverSock.isOpen()) {
							stopServer();
						}
						break;
					}
				}
			}			
		};
		eService.submit(runnable);
	}
	
	
	void stopServer() {
		
		try {
			Iterator<Client> iterator=connect.iterator();
			while(iterator.hasNext()) {
				Client client=iterator.next();
				client.sockC.close();
				iterator.remove();
			}
			
			if(serverSock!=null&&serverSock.isOpen()) {
				serverSock.close();
			}
			if(eService!=null&&!eService.isShutdown()) {
				eService.shutdown();
			}
			Platform.runLater(()->{
				displayText("[서버 멈춤]");
				btnStart.setText("start");
			});
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	class Client{
		SocketChannel sockC;
		
		
		Client(SocketChannel sockC) {
			this.sockC=sockC;
			receive();
		}
		
		void receive() {
			Runnable runnable=new Runnable() {
				
				@Override
				public void run() {
					while(true) {
						try {
							ByteBuffer byteB=ByteBuffer.allocate(100); //데이터를 보내기 전까지 블로킹 된다. 일반적인 할당(100)
							
							int count=sockC.read(byteB); //바이트 개수를 저장 
							
							if(count==-1) {
								throw new IOException();
							}
							
							String message="[요청 처리: "+sockC.getRemoteAddress()+": "+Thread.currentThread().getName()+"]";
							Platform.runLater(()->displayText(message));
							
							byteB.flip(); //포지션을 0으로 설정하고, 리미트를 현재 내용의 마지막 위치로 압축시킨다.
							Charset charset=Charset.forName("UTF-8"); 
							String data=charset.decode(byteB).toString();//utf-8로 디코딩한 문자열을 얻는다.
							
							for(Client client: connect) {
								client.send(data);
							}
							
						} catch (Exception e) {
							try {
								connect.remove(Client.this);
								String message="[클라이언트 통신 안됨: "+sockC.getRemoteAddress()+": "+Thread.currentThread().getName()+"]";
								Platform.runLater(()->displayText(message));
								sockC.close();
							} catch (IOException e2) {
								break;
							}
						}
					}
				}
			};
			eService.submit(runnable);
		}
		
		void send(String data) {
			Runnable runnable=new Runnable() {
				
				@Override
				public void run() {
					try {
						Charset charset=Charset.forName("UTF-8");
						ByteBuffer byteB=charset.encode(data);
						sockC.write(byteB);
					} catch (Exception e) {
						try {
							String message="[클라이언트 통신 안됨: "+sockC.getRemoteAddress()+": "+Thread.currentThread().getName()+"]";
							Platform.runLater(()->displayText(message));
							connect.remove(Client.this);
							sockC.close();
						} catch (IOException e2) {
							// TODO: handle exception
						}
					}
				}
			};
			eService.submit(runnable);
		}
	}	
	
	void displayText(String text) {
		txtDisplay.appendText(text + "\n");
	}

	@FXML public void startAction(ActionEvent e) {
		if(btnStart.getText().equals("start")) {
			startServer();
		}else if(btnStart.getText().equals("stop")) {
			stopServer();
		}
	}
}