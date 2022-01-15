import socketserver
import threading

HOST = '192.168.55.24' # 서버의 ip
PORT = 9009				 # 포트번호
lock = threading.Lock()  #쓰레드에서 lock함수 가져오기
                         #스레드에서 데이터(자원) 경쟁을 막기위해 사용


class UserManager:  
# /***********************
#    * 작성자: 김선민
#    * 작성일: 2022-01-9
#    *
#    * Param: 생성자
#    * Description: 사용자의 등록 정보를 담을 딕셔너리 생성
#    * Return: 등록정보 딕셔너리
#    ************************/
    def __init__(self):#생성자
        self.users = {}  # 사용자의 등록 정보를 담을 딕셔너리

    # /***********************
#    * 작성자: 김선민
#    * 작성일: 2022-01-12
#    *
#    * Param: 유저이름,소켓,주소
#    * Description: 사용자 ID를 self.users에 추가하는 함수
#    * Return: 유저이름
#    ************************/
    def addUser(self, username, client_socket, addr):  # 사용자 ID를 self.users에 추가하는 함수
        # 이미 등록된 사용자라면
        if username in self.users:  
            client_socket.send('!!!이미 등록된 사용자입니다.\n'.encode())
            return None

        # 새로운 사용자를 등록함
        lock.acquire()  # 스레드 동기화를 막기위한 락
        self.users[username] = (client_socket, addr) #사용자 ID를 소켓, 주소 정보와 함께 users에 추가
        lock.release()  # 업데이트 후 락 해제

        self.sendMessageToAll('[%s]님이 입장했습니다.' % username)
        print('---> 대화 참여자 수 [%d]' % len(self.users))

        return username

# /***********************
#    * 작성자: 김선민
#    * 작성일: 2022-01-13
#    *
#    * Param: 유저이름
#    * Description: 사용자를 제거하는 함수
#    * Return: 
#    ************************/
    def removeUser(self, username):  # 사용자를 제거하는 함수
        if username not in self.users:  #등록되지않았다면 그냥 나감
            return

        lock.acquire()  # 스레드 동기화를 막기위한 락
        del self.users[username]    # users에 등록된 사용자 정보 삭제
        lock.release()  # 업데이트 후 락 해제

        self.sendMessageToAll('[%s]님이 퇴장했습니다.' % username)
        print('---> 대화 참여자 수 [%d]' % len(self.users))



# /***********************
#    * 작성자: 김선민
#    * 작성일: 2022-01-13
#    *
#    * Param: 유저이름,메세지
#    * Description: 전송한 message처리하는 함수 
#    * Return: 참,거짓
#    ************************/
    def messageHandler(self, username, message):  # 전송한 message 처리하는 부분
        if message[0] != '/':  # 보낸 메세지의 첫문자가 '/'가 아니면
            self.sendMessageToAll('[%s] %s' % (username, message))  #메세지 출력하는거
            return

        if message.strip() == '/quit':  #strip=문자열 공백 없애는거
            # 보낸 메세지가 'quit'이면 나가게함
            self.removeUser(username)   
            return -1



# /***********************
#    * 작성자: 김선민
#    * 작성일: 2022-01-13
#    *
#    * Param: 메세지
#    * Description: 모든 사용자에게 메시지를 전송하는 함수
#    * Return: 
#    ************************/
    def sendMessageToAll(self, message):    
        # 사용자 수만큼 반복하여 모든 사용자에게 메시지 전송
        for client_socket, addr in self.users.values():  #딕셔너리의 밸류값=사용자 수
            client_socket.send(message.encode()) #encode=메세지를 다른곳에다 보냄


class MyTcpHandler(socketserver.BaseRequestHandler):    
    #socketserver.BaseRequestHandler=>소켓서버에 클라이언트 접속요청을 처리
    userman = UserManager()


    # 클라이언트의 요청을 처리
    # - 상속받은 BaseRequestHandler의 handle 메소드를 재정의
    def handle(self):  # 클라이언트가 접속시 클라이언트 주소 출력
        print('[%s] 연결됨' % self.client_address[0]) #self.client_address=참여자 IP주소

        #예외처리
        try:
            username = self.registerUsername()
            message = self.request.recv(1024)   #메세지 수신  
            
            #메세지 수신한 경우
            while message:
                print(message.decode()) #decode 받은 데이터를 형태를 바꾸는거
                if self.userman.messageHandler(username, message.decode()) == -1:   #값이 -1될때까지 채팅유지
                    self.request.close()
                    break
                message = self.request.recv(1024)   #메세지 수신


        except Exception as e: #에러발생시 문구
            print('에러가발생했습니다', e)


        print('[%s] 접속종료' % self.client_address[0])
        self.userman.removeUser(username)   #유저 삭제

# /***********************
#    * 작성자: 김선민
#    * 작성일: 2022-01-11
#    *
#    * Param: 
#    * Description: ID설정 하는 함수
#    * Return: 유저이름
#    ************************/
    def registerUsername(self): #ID설정 함수
        while True:
            self.request.send('로그인ID:'.encode())
            username = self.request.recv(1024)  #메세지 수신
            username = username.decode().strip()    
            
            #새로운 사용자로 등록된경우
            if self.userman.addUser(username, self.request, self.client_address):
                return username


class ChatingServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
        # - ThreadingMixIn : 독립된 스레드로 처리하도록 접속할 때마다 새로운 스레드 생성(여러 개의 클라이언트 요청 처리)
        # - TCPServer : 클라이언트의 연결 요청을 처리
    pass


# /***********************
#    * 작성자: 김선민
#    * 작성일: 2022-01-10
#    *
#    * Param: 
#    * Description: 채팅을 시작하는 함수
#    * Return: 
#    ************************/
def runServer():
    print('----채팅 서버를 시작합니다----')
    print('----채텅 서버를 끝내려면 Ctrl-C를 누르세요----')

    try:    #실행할 코드
        server = ChatingServer((HOST, PORT), MyTcpHandler)
        server.serve_forever()
    except KeyboardInterrupt:   #예외발생(ctrl+c)시 코드
        print('--- 채팅 서버를 종료합니다 ---')
        server.shutdown()       #소켓을 종료하는 코드
        server.server_close()   #서버를 종료하는 코드

runServer()