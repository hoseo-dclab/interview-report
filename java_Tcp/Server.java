package java_Tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Server extends Application{ 
	ExecutorService executorService; //������ Ǯ����
	ServerSocket serverSocket; //Ŭ���̾�Ʈ �����û�� �ޱ����� ����
	List<Client> connections = new Vector<Client>(); //������ Ŭ���̾�Ʈ�� ����Ʈ�� �����Ͽ� ����
	
	//���� ���� �ڵ�
	void startServer() {
		executorService = Executors.newFixedThreadPool( //��ȣ���� �ִ� ����ŭ ������Ǯ�� ����
				Runtime.getRuntime().availableProcessors() //cpu�� �ھ��� ���� ��
				);
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress("localhost",5001));//localhost ip�� 5001������ ������Ĺ�� ���ε���
			
		}catch(Exception e) {
			if(!serverSocket.isClosed())
				stopServer(); //������Ĺ�� �������� ������ ������ ����
			return; //startServer�޼ҵ带 ����
		}
		
		Runnable runnable = new Runnable() { //������� �۾���ü ����
			@Override
			public void run() {
				//UI���� ������ javafx Application �����常 �Ҽ�����
				Platform.runLater(()->{ //���� ������ �ش�޼ҵ带 ����Ͽ� javafx���� �����û����
					displayText("[���� ����]"); //UI�� ���������� ���
					btnStartStop.setText("stop");//start ��ư�� ������ stop���� �����
				});
				
				while(true) { //Ŭ���̾�Ʈ�� ���� �����۾�����
					try {
						Socket socket = serverSocket.accept(); //Ŭ���̾�Ʈ�� �����û�� �����
						//->�����û�� ������ �����ϰ� ��ſ� ��Ĺ�� ����
						String message = "[���� ����:" + socket.getRemoteSocketAddress() + ":" + Thread.currentThread().getName() + "]";
						//message = Ŭ���̾�Ʈ�� IP�� ��Ʈ��ȣ + ������Ǯ�� ����̸� 
						Platform.runLater(()->{
							displayText(message);
						});
						
						Client client = new Client(socket); //������ ��Ĺ�� ������ �Ű������� ����
						connections.add(client); //����Ʈ�� �ش� Ŭ���̾�Ʈ �߰�
						Platform.runLater(()->{
							displayText("[���� ����: "+ connections.size() + "]");
						});
					} catch (IOException e) {
						if(!serverSocket.isClosed())
							stopServer();
						break;
						//���ܹ߻��� �Ұ��� �ݰ� ���ѷ����� ��������
					}
										
				}
			}
		};
		executorService.submit(runnable); //ť�� �ִ� �۾�ó����û
	}
	
	//���� ���� �ڵ� (Ŭ���̾�Ʈ ����->������Ĺ ����->������ Ǯ ����)
	void stopServer() {
		try {
			Iterator<Client> iterator = connections.iterator(); //connections�� ����Ʈ�� iterator�޼ҵ� ����Ͽ� ������
			while(iterator.hasNext()) { //iterator�� ����Ʈ��Ҹ� ���������� Ȯ���ϰ���(hasNext�� booleanŸ����)
				Client client = iterator.next(); //Ȯ�ε� ��Ҹ� client�� ����
				client.socket.close(); //���Ե� client socket�� ����
				iterator.remove(); //���Ե� client�� ������
			}
			if(serverSocket !=null && !serverSocket.isClosed()) { //serverSocket�� null�� �ƴϸ� �������� �ʴ´ٸ�
				serverSocket.close(); //serverSocket�� ����
			}
			if(executorService != null && !executorService.isShutdown()) {//executorService�� null�� �ƴϸ� �������� �ʴ´ٸ�
				executorService.shutdown();//executorService�� ����
			}
			Platform.runLater(()->{
				displayText("[���� ����]");
				btnStartStop.setText("start");
			});
		}catch(Exception e) {}
	}
	
	//������ ��� �ڵ�
	class Client{
		Socket socket; //socket�� �ʵ�� ����
		Client(Socket socket){ //Client ������ ���� socket�� �ʵ忡 ��������
			this.socket = socket;
			receive();//client�� �����ɶ� ȣ��
		}
		
		void receive() { //client���� ���� �����͸� ����
			Runnable runnable = new Runnable() { //�۾���ü ����
				@Override
				public void run() {
					try {
						while(true) {//client���� ���� �޼����� ������ ���� �� �ְ���
							byte[] byteArr = new byte[100]; //���� ������ũ�� ����
							InputStream inputStream = socket.getInputStream();//��Ĺ���κ��� InputSream�� ����
							
							//byteArr�� ũ�⸸ŭ �����͸� �о� byteArr�� �Ѱ��ְ� �� ũ�⸦ readByteCount�� �־��ش�
							int readByteCount = inputStream.read(byteArr);//1.���������� �����͸� ���� ���
							//2.Clinet�� ���������� �������� ��� 3.Client�� ������������ �������� ���
							if(readByteCount == -1) {//���̻� ���� ����Ʈ�� ���ٸ�
								throw new IOException(); //���ܸ� �߻��ϰ� ��
							}
							
							String message = "[��û ó��" + socket.getRemoteSocketAddress() + ":" + Thread.currentThread().getName() + "]";
							Platform.runLater(()->displayText(message));//���� �޼����� UI�� ���
							
							String data = new String(byteArr,0,readByteCount,"UTF-8"); //���ڿ��� �����Ͽ� data ����
							//����� ��� client�� �����͸� ����
							for(Client clinet : connections) {
								clinet.send(data);
							}
						}
					}catch(Exception e) { //���� �߻���
						connections.remove(Client.this); //��� Ŭ���̾�Ʈ�� ������
						String message = "[Ŭ���̾�Ʈ ��� �ȵ�:"+socket.getRemoteSocketAddress() +":"+Thread.currentThread().getName() + "]";
						Platform.runLater(()->displayText(message)); //���� �޼����� ���
						try {
							socket.close(); //������ ������
						} catch (IOException e1) {}
					}
				}
			};
			executorService.submit(runnable); //���� �ִ� runnable ť�� �ְ� �۾�ó����û
		} 
		
		void send(String data) {//client�� �����͸� ����(���ڿ��� �Ű����� �޾� client�� ������)
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						byte[] byteArr = data.getBytes("UTF-8"); //���� data�� byteArr�� ����
						OutputStream outputStream = socket.getOutputStream();//��Ĺ���κ��� OutputStream�� ����
						outputStream.write(byteArr);//byteArr�� ��� ����Ʈ�� ��� ��Ʈ������ ����
						outputStream.flush();//���ۿ� �����ִ� �����͸� ��� ��½�Ű�� ���۸� ���
					} catch (Exception e) {//���� �߻���
						String message = "[Ŭ���̾�Ʈ ��� �ȵ�:"+socket.getRemoteSocketAddress() +":"+Thread.currentThread().getName() + "]";
						Platform.runLater(()->displayText(message));//���� �޼��� ���
						connections.remove(Client.this);//�ش� Ŭ���̾�Ʈ�� ������
						try {
							socket.close();//��Ĺ�� ������
						} catch (IOException e1) {}
					}
				}
			};
			executorService.submit(runnable);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////
	//javafx�� UI����
	TextArea txtDisplay;
	Button btnStartStop;

	@Override
	public void start(Stage primaryStage) throws Exception {
		BorderPane root = new BorderPane();
		root.setPrefSize(500, 300);
		
		txtDisplay = new TextArea();
		txtDisplay.setEditable(false);
		BorderPane.setMargin(txtDisplay, new Insets(0,0,2,0));
		root.setCenter(txtDisplay);

		btnStartStop = new Button("start");
		btnStartStop.setPrefHeight(30);
		btnStartStop.setMaxWidth(Double.MAX_VALUE);
		btnStartStop.setOnAction(e->{
			if(btnStartStop.getText().equals("start")) {
				startServer(); 
			}else if(btnStartStop.getText().equals("stop")) {
				stopServer();
			}
		});
		root.setBottom(btnStartStop);
		
		Scene scene = new Scene(root);
		//scene.getStylesheets().add(getClass().getResource("app.css").toString());
		primaryStage.setScene(scene);
		primaryStage.setTitle("Server");
		primaryStage.setOnCloseRequest(event->stopServer());
		primaryStage.show();
	}
	
	void displayText(String text) {
		txtDisplay.appendText(text+"\n");
	}
	/////////////////////////////////////////////////////////////////////////////////////////
	
	public static void main(String args[]) {
		launch(args);
	}
}
