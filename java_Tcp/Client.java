package java_Tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Client extends Application{
	Socket socket;
	
	//연결 시작 코드
	void startClient() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					socket = new Socket();
					socket.connect(new InetSocketAddress("localhost",5001));//다음과 같은 IP주소 포트번호로 소캣을 연결함
					Platform.runLater(()->{
						displayText("[연결 완료:"+socket.getRemoteSocketAddress() + "]");
						//연결된 서버의 IP주소와 포트번호를 출력
						btnConn.setText("stop");//버튼을 stop으로 변경
						btnSend.setDisable(false); //send버튼을 활성화함
					});
				} catch (IOException e) {
					Platform.runLater(()->displayText("[서버 통신 안됨]"));
					if(!socket.isClosed()) {stopClient();}//server가 닫혀있지 않으면 stopClient메소드를 부름
					return; //run메소드를 종료
				}
				receive();//연결 성공시 receive를 호출함
			}
		};
		thread.start();//스레드를 시작함
	}
	
	//연결 끊기 코드
	void stopClient() {
		try {
			Platform.runLater(()->{
				displayText("[연결 끊음]");
				btnConn.setText("start");//버튼을 start로 변경
				btnSend.setDisable(true);//send버튼을 비활성화함
			});
			if(socket != null && !socket.isClosed()) {
				socket.close(); //위의 조건을 만족할때 소캣을 닫음
			}
		}catch(Exception e) {}
	}
	
	//데이터 받기 코드
	void receive() {
		while(true) {//항상 서버에 데이터를 받아야 하기 때문에 무한루프를 사용함
			try {
				byte[] byteArr = new byte[100];
				InputStream inputStream = socket.getInputStream();
				
				int readByteCount = inputStream.read(byteArr);
				if(readByteCount == -1) {
					throw new IOException();
				}
				
				String data = new String(byteArr,0,readByteCount,"UTF-8");
				
				Platform.runLater(()->displayText("[" + socket.getRemoteSocketAddress() + "]" + data)); // 아이디및 데이터 출력
			}catch(Exception e) {
				Platform.runLater(()->displayText("[서버 통신 안됨]"));
				stopClient();
				break;
			}
		}
	}
	
	//데이터 전송코드
	void send(String data) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					byte[] byteArr = data.getBytes("UTF-8");
					OutputStream outputStream = socket.getOutputStream();
					outputStream.write(byteArr); //매개값으로 주어진 모든 바이트를 출력 스트립으로 보냄
					outputStream.flush();//버퍼에 남아있는 데이터를 모두 출력시키고 버퍼를 비움
					Platform.runLater(()->displayText("[보내기 완료]"));
				} catch (Exception e) {
					Platform.runLater(()->displayText("[서버 통신 안됨]"));
					stopClient();
				}
			}
		
		};
		thread.start();
	}
	
	//id설정 코드
	void setName() {
		Platform.runLater(()->{
			displayText("클라이언트 ID입력:");
			
		});
	}

	////////////////////////////////////////////////////////////////////////////////////
	//javafx로 UI생성
	TextArea txtDisplay;
	TextField txtInput;
	Button btnConn, btnSend;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		BorderPane root = new BorderPane();
		root.setPrefSize(500, 300);
		
		txtDisplay = new TextArea();
		txtDisplay.setEditable(false);
		BorderPane.setMargin(txtDisplay, new Insets(0,0,2,0));
		root.setCenter(txtDisplay);
		
		BorderPane bottom = new BorderPane();
			txtInput = new TextField();
			txtInput.setPrefSize(60, 30);
			BorderPane.setMargin(txtInput, new Insets(0,1,1,1));
				
			btnConn = new Button("start");
			btnConn.setPrefSize(60, 30);
			btnConn.setOnAction(e->{
				if(btnConn.getText().equals("start")) {
					startClient();
				}else if(btnConn.getText().equals("stop")) {
					stopClient();
				}
			});
			
			btnSend = new Button("send");
			btnSend.setPrefSize(60, 30);
			btnSend.setDisable(true);
			btnSend.setOnAction(e->send(txtInput.getText()));
			
			bottom.setCenter(txtInput);
			bottom.setLeft(btnConn);
			bottom.setRight(btnSend);
		root.setBottom(bottom);
		
		Scene scene = new Scene(root);
		//scene.getStylesheets().add(getClass().getResource("app.css").toString());
		primaryStage.setScene(scene);
		primaryStage.setTitle("Client");
		primaryStage.setOnCloseRequest(event->stopClient());
		primaryStage.show();
	}
	
	void displayText(String text) {
		txtDisplay.appendText(text + "\n");
	}
	/////////////////////////////////////////////////////////////////////////////////////////
	public static void main(String[] args) {
		launch(args);
	}

	

}
