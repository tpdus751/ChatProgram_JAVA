package chat.client;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

public class ChatClient {
	
	static String chatName;
	Socket socket;
	
	DataInputStream dis;
	DataOutputStream dos;
	FileInputStream fis;
	FileOutputStream fos;
	
	CurrentTimeNow now;

	static List<String> chatLog = new ArrayList<>();
	
	final String quitCommand = "quit";
	
	public void Connect(String serverIP, int portNo) {
		// 서버에 연결
		try {
			socket = new Socket(serverIP, portNo);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Scanner sc = new Scanner(System.in);
	
		try {
			while (true) {
				System.out.print("채팅방에 입장하시려면 대화명(닉네임)을 입력하세요 : ");
				String chatName = sc.nextLine();
				chatLog.add("채팅방에 입장하시려면 대화명(닉네임)을 입력하세요 : " + chatName + "\n");
				
				dis = new DataInputStream(socket.getInputStream());
				dos = new DataOutputStream(socket.getOutputStream());
				
				// 대화명을 저장하고 서버에 대화명을 알려줌
				this.chatName = chatName;
				send(chatName);
				
				boolean canUse = receiveCanUse();
				if (canUse == true) {
					System.out.println("[" + chatName + "] 채팅 서버 연결 성공 (" + serverIP + " : " + portNo + ")");
					chatLog.add("[" + chatName + "] 채팅 서버 연결 성공 (" + serverIP + " : " + portNo + ")\n");
					
					System.out.println("각종 기능을 확인하시려면 \"/option\" 을 입력하십시오.");
					chatLog.add("각종 기능을 확인하시려면 \"/option\" 을 입력하십시오.\n");
					break;
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean receiveCanUse() {
		try {
			String message = dis.readUTF();
			if (message.equals("해당 닉네임은 이미 사용 중 입니다.")) {
				System.out.println("해당 닉네임은 이미 사용 중 입니다.");
				chatLog.add("해당 닉네임은 이미 사용 중 입니다.\n");
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public void send(String message) {
		if (message.equals("/save")) {
			now = new CurrentTimeNow();
			try (
					FileWriter fow = new FileWriter("C:/chatProgramming/java work/java-friday/BasicChapApp/src/chat/client/chatLog_" + now.getCurrentTime() + ".txt");
					BufferedWriter bw = new BufferedWriter(fow);
				){
					for (String chat : chatLog) {
						bw.write(chat);
					}
					
					System.out.println("현재까지 챗로그가 저장되었습니다. [chatLog_" + now.getCurrentTime() + ".txt]");
					chatLog.add("현재까지 챗로그가 저장되었습니다. [chatLog_" + now.getCurrentTime() + ".txt]\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (message.equals("/option")) {
			StringBuilder sb = new StringBuilder();
			sb.append("---------옵션--------\n");
			sb.append("1. /rename 변경할대화명 : 사용자의 대화명을 변경합니다.\n");
			sb.append("2. /to 대화명 보낼메시지 : 해당 대화명 사용자에게만 메시지를 보냅니다.\n");
			sb.append("3. /img 전송할파일명 : 모든 사용자에게 해당 파일을 다운받을 지 확인합니다.\n");
			sb.append("4. /download 다운받을파일명 : 해당 파일을 다운받습니다.\n");
			sb.append("5. /save : 현재까지의 채팅로그를 txt파일로 다운받습니다.\n");
			sb.append("6. /memberList : 귓속말을 보낼 수 있는 대화명 목록을 확인합니다.\n");
			
			System.out.println(sb.toString());
			chatLog.add(sb.toString());
		} else {
		}
			try {
				dos.writeUTF(message);
				dos.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	
	private void sendImage(String fileName) {
	try (FileInputStream fis = new FileInputStream("C:/chatProgramming/java work/java-friday/BasicChapApp/src/chat/client/" + fileName);
		 BufferedInputStream bis = new BufferedInputStream(fis)
		) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			
			while ((bytesRead = bis.read(buffer)) != -1) {
				dos.write(buffer, 0, bytesRead);
			}
			dos.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("[이미지 전송 완료] 파일 이름 : " + fileName);
		System.out.print("> ");
		chatLog.add("[이미지 전송 완료] 파일 이름 : " + fileName + "\n> ");
	}
	
	public void receive() {
		new Thread(() -> {
			try {
				while (true) {
					String message = dis.readUTF();
					
					if (message.length() != 4 && message.contains("/img")) {
						StringTokenizer st = new StringTokenizer(message);
						String command = st.nextToken();
						String fileName = st.nextToken();
						sendImage(fileName);
					} else if (message.equals("/img")) {
						String fileName = dis.readUTF();
						
						receiveImage(fileName);
					} else if (message.startsWith("/notify")) {
						message = message.substring(8);
						
						System.out.println(message);
						System.out.print("> ");
						chatLog.add(message + "\n> ");
					} else {
						System.out.println(message);
						System.out.print("> ");
						chatLog.add(message + "\n> ");
					}
				}
			} catch (Exception e) {
				// 서버가 종료된 경우, dis가 close된 경우
				quit();
				System.exit(0);
			}
		}).start();
	}
	
	private void receiveImage(String fileName) {
		try (
			FileOutputStream fos = new FileOutputStream("C:/chatProgramming/java work/java-friday/BasicChapApp/src/chat/client/" + chatName + "_" + fileName)
			) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			while((bytesRead = dis.read(buffer)) != -1) {
				fos.write(buffer, 0 ,bytesRead);
				if (bytesRead < buffer.length) break; // 파일 전송 종료
			}
			System.out.println("[이미지 수신 완료] 파일 이름 : " + fileName);
			chatLog.add("[이미지 수신 완료] 파일 이름 : " + fileName + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void quit() {
		
		try {
			dis.close();
			dos.close();
			socket.close();
			System.out.println("[" + chatName + "] 채팅 서버 연결 종료");
			chatLog.add("[" + chatName + "] 채팅 서버 연결 종료\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		final String serverIP = "localhost";
		final int portNo = 50005;
		
		
		ChatClient chatClient = new ChatClient();
		
		Scanner sc = new Scanner(System.in);
		
		// 대화명 입력을 받아서 서버 연결 시 전달
			
		// 서버 연결 (connect)
		chatClient.Connect(serverIP, portNo);
		
		
		// 채팅 서버로부터 메시지를 받아서 처리(receive) -> Thread 처리(독립적으로 돌아가야 다음 일을 처리하기 위해)
		chatClient.receive();
		
		// 사용자가 입력한 메시지를 서버로 전송(send)
		while (true) {
			System.out.print("> ");
			String message = sc.nextLine();
			chatLog.add("> " + message + "\n");
			
			chatClient.send(message);
			if (message.equals(chatClient.quitCommand)) break;
		}
		
		// 사용자가 입력한 메시지에 quit command가 있으면 퇴장 처리(quit)
		chatClient.quit();
	}
}
