package kr.or.ddit.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

public class MultiChatClient {

    // 시작 메서드
    public void clientStart() {

        Socket socket = null;

        try {
            socket = new Socket("192.168.141.26", 7777);

            System.out.println("대화방 서버에 연결되었습니다.");
            System.out.println("(\"/?\" 명령어로 명령어 목록 확인이 가능합니다)");
            System.out.println();

            // 송신용 스레드 생성
            ClientSender sender = new ClientSender(socket);

            // 수신용 스레드 생성
            ClientReceiver receiver = new ClientReceiver(socket);

            sender.start();
            receiver.start();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // 메시지를 전송하는 스레드 클래스
    class ClientSender extends Thread {
        private DataOutputStream dos;
        private Scanner scanner;


        public ClientSender(Socket socket) {
            scanner = new Scanner(System.in);

            try {
                dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        public void userCommand(String cmd) throws IOException {


            String[] split = cmd.split(" ");

            String cmdToLower = split[0].toLowerCase(Locale.ROOT);
            if (split.length == 1) {
                System.err.println("올바른 명령어가 아닙니다.");
            } else {
                switch (cmdToLower) {

                    // 종료
                    case "/exit":
                        System.out.println("대화방을 종료합니다.");
                        System.exit(0);
                        break;
                    case "/?":
                        System.out.println("======================= 명령어 ======================");
                        System.out.println("/w \t: 귓속말을 전송합니다. (/w 사용자 메세지)");
                        System.out.println("/? \t: 명령어 목록을 출력합니다.");
                        System.out.println("/exit \t: 대화방을 나갑니다.");
                        System.out.println("=====================================================");
                        break;
                    case "/w":
                        dos.writeUTF(cmd);
                        break;
                    default:
                        System.err.println("올바른 명령어가 아닙니다.");
                }
            }
        }

        @Override
        public void run() {
            try {
                if (dos != null) {
                    // 시작하자 마자 자신의 대화명을 서버로 전송한다.
                    String name = "";
                    System.out.println("----------------------------------");
                    System.out.println("대화명에는 특수문자를 사용할 수 없습니다.");
                    System.out.println("----------------------------------");
                    while (true) {
                        Thread.sleep(50);
                        System.out.print("대화명 >> ");
                        name = scanner.nextLine();

                        if (name.equals("")) {
                            System.out.println();
                            System.err.println("대화명을 입력하여 주십시오");
                        } else {
                            String pattern = "^[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힣]*$";
                            String slash = name.substring(0, 1);
                            if (slash.equals("/")) { // 슬래쉬 입력의 경우
                                userCommand(name); // 커맨드 메소드 실행
                            } else {
                                if (!Pattern.matches(pattern, name)) {
                                    System.out.println();
                                    System.err.println("공백 혹은 특수문자는 입력할 수 없습니다.");
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                    dos.writeUTF(name);
                    System.out.println("=====================================");
                    System.out.println("\t" + name + "님 환영합니다.");
                    System.out.println("=====================================");
                }

                while (dos != null) {
                    // 키보드로 입력받은 메시지를 서버로 전송

                    try {
                        String contents = scanner.nextLine();
                        // 요소는 /부분과 명령어부분 명령어의 속성부분과 내용으로 이루어짐.
                        // 예시. /w 사용자 내용
                        String slash = contents.substring(0, 1);

                        if (slash.equals("/")) { // 슬래쉬 입력의 경우
                            userCommand(contents); // 커맨드 메소드 실행
                        } else {
                            dos.writeUTF(contents); // 메세지 전송
                        }
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (Exception e) { // 명령어가 맞지 않을경우의 오류 발생
                        System.err.println("콘솔 명령어가 올바르지 않습니다.");
                    }
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (InterruptedException e) {
                System.err.println("스레드 오류");
            }
        }

    }

    // 메시지를 수신하는 스레드 클래스
    class ClientReceiver extends Thread {
        private DataInputStream dis;

        public ClientReceiver(Socket socket) {
            try {
                dis = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (dis != null) {
                try {
                    // 서버로부터 수신한 메세지를 콘솔에 출력
                    String message = dis.readUTF();
                    if (message.split(" ")[0].equals("[전체]")) {
                        System.out.println(message);
                    } else {
                        System.err.println(message);
                    }

                } catch (IOException e) {
                    System.out.println();
                    System.err.println("서버가 종료되었습니다.");
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        new MultiChatClient().clientStart();
    }
}
