package kr.or.ddit.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultiChatServer {
    // 대화명, 클라이언트의 Socket을 저장하기 위한 Map변수 선언
    Map<String, Socket> clients;

    public MultiChatServer() {
        // 동기화 처리가 가능하도록 Map객체 생성하기
        clients = Collections.synchronizedMap(new HashMap<String, Socket>());
    }

    // 서버 시작 메서드
    public void serverStart() {
        ServerSocket server = null;
        Socket socket = null;

        try {
            System.out.println("서버가 시작되었습니다.");

            server = new ServerSocket(7777);

            while (true) {

                // 클라이언트의 접속을 대기한다.
                socket = server.accept();

                System.out.println("[" + socket.getInetAddress() + " : " + socket.getPort() + "] 에서 접속하였습니다.");

                // 메세지 전송 처리를 하는 스레드 객체 생성 및 실행

                ServerReceiver receiver = new ServerReceiver(socket);
                receiver.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 서버소켓 닫기
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    /**
     * 대화방 즉, Map에 저장된 전체 유저에게 안내메시지를 전송하는 메서드
     *
     * @param msg 안내 메시지
     */
    public void sendMessage(String msg) {
        Iterator<String> it = clients.keySet().iterator();
        DataOutputStream dos = null;
        while (it.hasNext()) {
            try {
                String name = it.next(); // 대화명(Key) 구하기

                // 대화명에 해당하는 Socket 객체에서 OutputStream 객체 구하기
                dos = new DataOutputStream(clients.get(name).getOutputStream());
                dos.writeUTF(msg);

            } catch (IOException e) {
                System.err.println("클라이언트 Disconnect 감지");
            }
        }
    }

    /**
     * 대화방 즉, Map에 저장된 전체 유저에게 안내메시지를 전송하는 메서드
     *
     * @param msg  안내 메시지
     * @param from 유저 정보
     */
    public void sendMessage(String msg, String from) {
        sendMessage("[전체] " + from + ": " + msg);
    }

    public boolean searchingUser(String user) {
        Iterator<String> it = clients.keySet().iterator();
        boolean result = false;
        while (it.hasNext()) {
            String name = it.next(); // 대화명(Key) 구하기
            if (user.equals(name)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * 대화방 즉, Map에 저장된 특정 유저에게 안내메시지를 전송하는 메서드
     *
     * @param msg  송신유저에게 수신받은 메세지
     * @param from 송신한 유저
     */
    public void sendDirectMessage(String msg, String from) {
        Iterator<String> it = clients.keySet().iterator();

        try {
            DataOutputStream dos = null;
            String userAndContent = msg.substring(3); // /w 명령어 제거

            int msgIdx = msg.indexOf(" ");
            int msgIdxTest = userAndContent.indexOf(" "); // 오류 많이나던거
            // ("/w a"처럼 공백조차 없이 입력이 될경우 오류가 발생. 이를 해결해기위해 userAndContent의
            // indexOf를 사용한 변수를 따로 지정하여 -1를 반환할 경우 content를 초기화 하지 않고 공백으로 남기도록 함)

            String content = "";
            if (msgIdxTest > 0 || msgIdx > 0) {
                content = userAndContent.substring(msgIdx); // 수신자의 이름을 제외한 내용만을
            }


            String user = userAndContent.split(" ")[0]; // 수신자 추출
            String sendMsg = "[" + from + "]: " + content; // 보낼 내용
            String sendMsgFrom = "[" + user + "에게] : " + content; // 송신자에게 보낼 내용


            if (!searchingUser(user)) {
                dos = new DataOutputStream(clients.get(from).getOutputStream());
                dos.writeUTF("입력한 유저는 존재하지 않거나 올바르지 않은 형식입니다.");
            } else {
                if (user.equals(from)) {
                    dos = new DataOutputStream(clients.get(from).getOutputStream());
                    dos.writeUTF("자신에게는 귓속말을 할 수 없습니다.");
                } else {
                    // 대화명에 해당하는 Socket 객체에서 OutputStream 객체 구하기
                    dos = new DataOutputStream(clients.get(user).getOutputStream());
                    dos.writeUTF(sendMsg);

                    dos = new DataOutputStream(clients.get(from).getOutputStream());
                    dos.writeUTF(sendMsgFrom);
                }
            }

        } catch (SocketException e) {
            System.err.println("클라이언트 Disconnect 감지");
        } catch (IOException e) {
            System.out.println();
        }
    }

    /**
     * 서버에서 클라이언트로 메세지를 전송할 Thread 클래스를 Inner클래스로 정의한다. (Inner클래스에서는 부모 클래스의 멤버들을 직접
     * 사용할 수 있다.)
     */

    class ServerReceiver extends Thread {

        private Socket socket;
        private DataInputStream dis;
        private String name;

        public ServerReceiver(Socket socket) {
            this.socket = socket;

            try {
                dis = new DataInputStream(socket.getInputStream());

            } catch (SocketException e) {
                System.err.println("클라이언트 Disconnect 감지");
            } catch (IOException e) {
                System.out.println();
            }
        }

        @Override
        public void run() {
            try {
                // 서버에서는 클라이언트가 보내는 최초의 메시지 즉, 대화명을 수신해야한다.
                name = dis.readUTF();

                // 대화명을 받아서 다른 모든 클라이언트에게 대화방 참여
                // 메세지를 보낸다.
                sendMessage("#" + name + "님이 입장했습니다.");

                // 대화명과 소켓정보를 Map에 저장한다.
                clients.put(name, socket);

                System.out.println("현재 서버 접속자 수는 " + clients.size() + "명 입니다");

                // 이 후의 메세지 처리는 반복문으로 처리한다.
                // 한 클라이언트가 보낸 메세지를 다른 모든 클라이언트에게 보내준다.
                while (dis != null) {
                    String content = dis.readUTF();
                    if (content.split(" ")[0].equals("/w")) {
                        sendDirectMessage(content, name);
                    } else {
                        sendMessage(content, name);
                    }
                }
            } catch (SocketException e) {
                System.err.println("클라이언트 Disconnect 감지");
            } catch (IOException e) {
                System.out.println();
            } finally {
                // 이 finally 영역이 실행된다는 의미는 클라이언트의 접속이 종료되었다는 의미이다.

                sendMessage(name + "님이 나가셨습니다.");

                // Map에서 해당 대화명을 삭제한다.
                clients.remove(name);

                System.out.println("[" + socket.getInetAddress() + " : " + socket.getPort() + ", 대화명: " + name + "] 에서 접속을 종료했습니다.");
                System.out.println("현재 접속자 수는 " + clients.size() + "명 입니다.");
            }
        }
    }

    public static void main(String[] args) {
        new MultiChatServer().serverStart();

    }
}