package chat.server;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;

public class ChatServer {
	
	final String quitCommand = "quit";
	ServerSocket serverSocket;
	Map<String, ClientService> chatClientInfo = new Hashtable<>(); 
	
	public void start(int portNo) {
		
		try {
			serverSocket = new ServerSocket(portNo);
			System.out.println("[ 채팅서버 ] 시작 (" + InetAddress.getLocalHost() + " : " + portNo + ")");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// 클라이언트 연결 요청을 받아서 채팅 서비스 제공 -> Daemon 스레드 처리 (connectClient)
	public void connectClient() {
		
		Thread thread = new Thread(() -> {
			try {
				while (true) {
					// 클라이언트 연결 요청에 대해 통신을 할 socket 생성
					Socket socket = serverSocket.accept();
					new ClientService(this, socket); // this는 서버
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		thread.setDaemon(true); // 서버가 죽을 시 해당 스레드도 죽어야 하니 데몬으로
		thread.start();
	}
	
	public void stop() {
		try {
			serverSocket.close();
			System.out.println("[ 채팅서버 ] 종료");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void addClientInfo(ClientService clientService) {

		chatClientInfo.put(clientService.chatName, clientService);
		System.out.println("[입장] : " + clientService.displayName + "(채팅 참여자 수 : " + chatClientInfo.size() + ")");
	}
	
	public void sendToAll(ClientService clientService, String message) {
		
		// 해당 클라이언트가 보낸 메시지를 다른 채팅 참여자에게 전송하는 함수
		for (ClientService cs : chatClientInfo.values()) {
			if (cs != clientService) {
				cs.send(message);
			} 
		}
	}
	
	public void removeClientInfo(ClientService clientService) {
		chatClientInfo.remove(clientService.displayName);
		System.out.println("[퇴장] : " + clientService.displayName + "(채팅 참여자 수 : " + chatClientInfo.size() + ")");
	}
	
	public boolean isAlreadyUse(ClientService clientService, String chatName) {
		boolean isUse = true;
		
		for (ClientService cs : chatClientInfo.values()) {
			if (cs.chatName.equals(clientService.chatName)) {
				System.out.println("[중복 닉네임 시도]  [닉네임 : " + clientService.chatName + "] - IP : " + clientService.clientIP);
				clientService.send("해당 닉네임은 이미 사용 중 입니다.");
				isUse = false;
			}
		}
		return isUse;
	}
	
	public boolean ChangeChatName(String beforeName, String newChatName, ClientService clientService) {
		boolean isCan = true;
		
		for (ClientService cs : chatClientInfo.values()) {
			if (cs.chatName.equals(newChatName)) {
				isCan = false;
			}
		}
		
		if (isCan) {
			chatClientInfo.remove(beforeName, clientService);
			chatClientInfo.put(newChatName, clientService);
			clientService.chatName = newChatName;
			clientService.displayName = clientService.chatName + "@" + clientService.clientIP;
			System.out.println("[닉네임 변경] : " + beforeName + " -> " + newChatName + " - " + clientService.clientIP);
		}
		return isCan;
	}
	
	public boolean sendToOne(ClientService clientService, String nickNameToSend, String sendMessage) {
		boolean isCan = false;
		
		for (ClientService cs : chatClientInfo.values()) {
			if (cs.chatName.equals(nickNameToSend)) {
				isCan = true;
				cs.send(sendMessage);
			}
		}
		
		return isCan;
	}
	
	public void sendImageToAll(ClientService clientService, String fileName) {
		for (ClientService cs : chatClientInfo.values()) {
	        if (cs != clientService) {
	            cs.sendImage(fileName);
	        }
	    }
	}
	
	public StringBuilder searchOnlineMember(ClientService clientService) {
		StringBuilder sb = new StringBuilder();
		
		if (chatClientInfo.size() > 1) {
			sb.append("귓속말 보낼 수 있는 사용자 대화명 목록\n");
			
			for (String chatName : chatClientInfo.keySet()) {
				if (!(chatName.equals(clientService.chatName))) {
		            sb.append(chatName + "\n");
		        }
			}
		} else {
			sb.append("귓속말을 보낼 수 있는 사용자가 없습니다.");
		}
		
		return sb;
	}
	
	public static void main(String[] args) {
		
		final int portNo = 50005;
		
		ChatServer chatServer = new ChatServer();
		
		// 채팅 서버를 시작 (start)
		chatServer.start(portNo);
		
		// 클라이언트 연결 요청을 받아서 채팅 서비스 제공 -> Daemon 스레드 처리 (connectClient)
		chatServer.connectClient();
		
		// 종료 command 처리 (stop)
		while (true) {
			System.out.println("서버를 종료하려면 quit을 입력하고 Enter를 치세요");
			Scanner sc = new Scanner(System.in);
			String command = sc.nextLine();
			if (command.equalsIgnoreCase(chatServer	.quitCommand)) break;
		}
		
		chatServer.stop();
	}

}
	