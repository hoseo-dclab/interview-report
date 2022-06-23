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
	
	//���� ���� �ڵ�
	void startClient() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					socket = new Socket();
					socket.connect(new InetSocketAddress("localhost",5001));//������ ���� IP�ּ� ��Ʈ��ȣ�� ��Ĺ�� ������
					Platform.runLater(()->{
						displayText("[���� �Ϸ�:"+socket.getRemoteSocketAddress() + "]");
						//����� ������ IP�ּҿ� ��Ʈ��ȣ�� ���
						btnConn.setText("stop");//��ư�� stop���� ����
						btnSend.setDisable(false); //send��ư�� Ȱ��ȭ��
					});
				} catch (IOException e) {
					Platform.runLater(()->displayText("[���� ��� �ȵ�]"));
					if(!socket.isClosed()) {stopClient();}//server�� �������� ������ stopClient�޼ҵ带 �θ�
					return; //run�޼ҵ带 ����
				}
				receive();//���� ������ receive�� ȣ����
			}
		};
		thread.start();//�����带 ������
	}
	
	//���� ���� �ڵ�
	void stopClient() {
		try {
			Platform.runLater(()->{
				displayText("[���� ����]");
				btnConn.setText("start");//��ư�� start�� ����
				btnSend.setDisable(true);//send��ư�� ��Ȱ��ȭ��
			});
			if(socket != null && !socket.isClosed()) {
				socket.close(); //���� ������ �����Ҷ� ��Ĺ�� ����
			}
		}catch(Exception e) {}
	}
	
	//������ �ޱ� �ڵ�
	void receive() {
		while(true) {//�׻� ������ �����͸� �޾ƾ� �ϱ� ������ ���ѷ����� �����
			try {
				byte[] byteArr = new byte[100];
				InputStream inputStream = socket.getInputStream();
				
				int readByteCount = inputStream.read(byteArr);
				if(readByteCount == -1) {
					throw new IOException();
				}
				
				String data = new String(byteArr,0,readByteCount,"UTF-8");
				
				Platform.runLater(()->displayText("[" + socket.getRemoteSocketAddress() + "]" + data)); // ���̵�� ������ ���
			}catch(Exception e) {
				Platform.runLater(()->displayText("[���� ��� �ȵ�]"));
				stopClient();
				break;
			}
		}
	}
	
	//������ �����ڵ�
	void send(String data) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					byte[] byteArr = data.getBytes("UTF-8");
					OutputStream outputStream = socket.getOutputStream();
					outputStream.write(byteArr); //�Ű������� �־��� ��� ����Ʈ�� ��� ��Ʈ������ ����
					outputStream.flush();//���ۿ� �����ִ� �����͸� ��� ��½�Ű�� ���۸� ���
					Platform.runLater(()->displayText("[������ �Ϸ�]"));
				} catch (Exception e) {
					Platform.runLater(()->displayText("[���� ��� �ȵ�]"));
					stopClient();
				}
			}
		
		};
		thread.start();
	}
	
	//id���� �ڵ�
	void setName() {
		Platform.runLater(()->{
			displayText("Ŭ���̾�Ʈ ID�Է�:");
			
		});
	}

	////////////////////////////////////////////////////////////////////////////////////
	//javafx�� UI����
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
