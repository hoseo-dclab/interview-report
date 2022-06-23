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
	ExecutorService executorService; //스레드 풀생성
	ServerSocket serverSocket; //클라이언트 연결요청을 받기위해 생성
	List<Client> connections = new Vector<Client>(); //생성된 클라이언트를 리스트에 저장하여 관리
	
	//서버 시작 코드
	void startServer() {
		executorService = Executors.newFixedThreadPool( //괄호안의 있는 값만큼 스레드풀을 생성
				Runtime.getRuntime().availableProcessors() //cpu의 코어의 수를 얻어냄
				);
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress("localhost",5001));//localhost ip의 5001번에서 서버소캣이 바인딩함
			
		}catch(Exception e) {
			if(!serverSocket.isClosed())
				stopServer(); //서버소캣이 닫혀있지 않으면 서버를 닫음
			return; //startServer메소드를 종료
		}
		
		Runnable runnable = new Runnable() { //연결수락 작업객체 생성
			@Override
			public void run() {
				//UI생성 변경은 javafx Application 스레드만 할수잇음
				Platform.runLater(()->{ //위의 이유로 해당메소드를 사용하여 javafx에게 변경요청을함
					displayText("[서버 시작]"); //UI에 서버시작을 출력
					btnStartStop.setText("stop");//start 버튼을 누를시 stop으로 변경됌
				});
				
				while(true) { //클라이언트의 연결 수락작업을함
					try {
						Socket socket = serverSocket.accept(); //클라이언트의 연결요청을 대기함
						//->연결요청이 들어오면 수락하고 통신용 소캣을 생성
						String message = "[연결 수락:" + socket.getRemoteSocketAddress() + ":" + Thread.currentThread().getName() + "]";
						//message = 클라이언트의 IP와 포트번호 + 스레드풀의 담당이름 
						Platform.runLater(()->{
							displayText(message);
						});
						
						Client client = new Client(socket); //생성된 소캣을 생성자 매개값으로 전달
						connections.add(client); //리스트에 해당 클라이언트 추가
						Platform.runLater(()->{
							displayText("[연결 갯수: "+ connections.size() + "]");
						});
					} catch (IOException e) {
						if(!serverSocket.isClosed())
							stopServer();
						break;
						//예외발생시 소컛을 닫고 무한루프를 빠져나감
					}
										
				}
			}
		};
		executorService.submit(runnable); //큐에 넣는 작업처리요청
	}
	
	//서버 종료 코드 (클라이언트 제거->서버소캣 종료->스레드 풀 종료)
	void stopServer() {
		try {
			Iterator<Client> iterator = connections.iterator(); //connections의 리스트를 iterator메소들 사용하여 접근함
			while(iterator.hasNext()) { //iterator의 리스트요소를 마지막까지 확인하겠음(hasNext는 boolean타입임)
				Client client = iterator.next(); //확인된 요소를 client에 대입
				client.socket.close(); //대입된 client socket을 종료
				iterator.remove(); //대입된 client를 제거함
			}
			if(serverSocket !=null && !serverSocket.isClosed()) { //serverSocket이 null이 아니며 닫혀있지 않는다면
				serverSocket.close(); //serverSocket을 종료
			}
			if(executorService != null && !executorService.isShutdown()) {//executorService가 null이 아니며 닫혀있지 않는다면
				executorService.shutdown();//executorService를 종료
			}
			Platform.runLater(()->{
				displayText("[서버 멈춤]");
				btnStartStop.setText("start");
			});
		}catch(Exception e) {}
	}
	
	//데이터 통신 코드
	class Client{
		Socket socket; //socket을 필드로 가짐
		Client(Socket socket){ //Client 생성자 받은 socket을 필드에 대입해줌
			this.socket = socket;
			receive();//client가 생성될때 호출
		}
		
		void receive() { //client에게 받은 데이터를 받음
			Runnable runnable = new Runnable() { //작업객체 생성
				@Override
				public void run() {
					try {
						while(true) {//client에게 받은 메세지를 무한히 받을 수 있게함
							byte[] byteArr = new byte[100]; //읽을 데이터크기 설정
							InputStream inputStream = socket.getInputStream();//소캣으로부터 InputSream을 얻음
							
							//byteArr의 크기만큼 데이터를 읽어 byteArr에 넘겨주고 그 크기를 readByteCount에 넣어준다
							int readByteCount = inputStream.read(byteArr);//1.정상적으로 데이터를 받은 경우
							//2.Clinet가 정상적으로 종료했을 경우 3.Client가 비정상적으로 종료했을 경우
							if(readByteCount == -1) {//더이상 읽을 바이트가 없다면
								throw new IOException(); //예외를 발생하게 함
							}
							
							String message = "[요청 처리" + socket.getRemoteSocketAddress() + ":" + Thread.currentThread().getName() + "]";
							Platform.runLater(()->displayText(message));//위의 메세지를 UI에 출력
							
							String data = new String(byteArr,0,readByteCount,"UTF-8"); //문자열로 변경하여 data 넣음
							//연결된 모든 client에 데이터를 보냄
							for(Client clinet : connections) {
								clinet.send(data);
							}
						}
					}catch(Exception e) { //예외 발생시
						connections.remove(Client.this); //모든 클라이언트를 제거함
						String message = "[클라이언트 통신 안됨:"+socket.getRemoteSocketAddress() +":"+Thread.currentThread().getName() + "]";
						Platform.runLater(()->displayText(message)); //위의 메세지를 출력
						try {
							socket.close(); //연결을 종료함
						} catch (IOException e1) {}
					}
				}
			};
			executorService.submit(runnable); //위에 있는 runnable 큐에 넣고 작업처리요청
		} 
		
		void send(String data) {//client에 데이터를 보냄(문자열을 매개값을 받아 client로 전송함)
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						byte[] byteArr = data.getBytes("UTF-8"); //받은 data를 byteArr에 넣음
						OutputStream outputStream = socket.getOutputStream();//소캣으로부터 OutputStream을 받음
						outputStream.write(byteArr);//byteArr의 모든 바이트를 출력 스트림으로 보냄
						outputStream.flush();//버퍼에 남아있는 데이터를 모두 출력시키고 버퍼를 비움
					} catch (Exception e) {//예외 발생시
						String message = "[클라이언트 통신 안됨:"+socket.getRemoteSocketAddress() +":"+Thread.currentThread().getName() + "]";
						Platform.runLater(()->displayText(message));//위의 메세지 출력
						connections.remove(Client.this);//해당 클라이언트를 제거함
						try {
							socket.close();//소캣을 종료함
						} catch (IOException e1) {}
					}
				}
			};
			executorService.submit(runnable);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////
	//javafx로 UI생성
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
