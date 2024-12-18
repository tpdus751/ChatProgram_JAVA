package chat.server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.StringTokenizer;

public class ClientService {
	
	ChatServer chatServer;
	Socket socket;
	
	DataInputStream dis;
	DataOutputStream dos;
	
	String clientIP;
	String chatName;
	String displayName;
	
	final String quitCommand = "quit";
	
	public ClientService(ChatServer chatServer, Socket socket) throws Exception {
		
		// 필요 자료 구조 초기화
		this.chatServer = chatServer;
		this.socket = socket;
		
		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());
		
		// 클라이언트 정보 수집 -> 서버한테 알려주기
		clientIP = socket.getInetAddress().getHostName(); // 정보 중에 클라이언트 아이피 얻어오기
		while (true) {
			this.chatName = dis.readUTF();
			
			boolean isUse = chatServer.isAlreadyUse(this, chatName);
			if (isUse) {
				break;
			}
		}
		
		displayName = chatName + "@" + clientIP;
		
		chatServer.addClientInfo(this);
		
		CurrentTimeNow now = new CurrentTimeNow(); 
		
		// 해당 클라이언트에게 입장 되었음 알림
		send("[채팅방에 입장하였습니다. : " + chatName);
		
		// 입장 알림 -> ChatServer에게 요청
		chatServer.sendToAll(this, "[입장]" + "[" + chatName + "] (" + now.getCurrentTime() + ")"); 
		
		// 클라이언트가 보낸 메시지를 다른 채팅 참여자에게 전송 -> ChatServer에게 요청 -> Thread(Daemon) 자동 데몬 이미 클라이언트서비스가 데몬스레드로 생성되었기 때문에
		receive();
	}

	private void receive() {
		
		new Thread(() -> {
			StringTokenizer st;
			try {
				while (true) {
					CurrentTimeNow now = new CurrentTimeNow(); 
					
					String message = dis.readUTF();
					
					boolean command = false;
					boolean isCan = false;
					String thisCommand = "";
					String beforeName = "";
					String newChatName = "";
					
					if (message.startsWith("/rename")) {
						command = true;
						
						st = new StringTokenizer(message);
						
						thisCommand = st.nextToken();
						beforeName = this.chatName;
						newChatName = st.nextToken();
						if (this.chatName.equals(newChatName)) {
							send("변경할 닉네임과 현재 닉네임이 같습니다.");
						} else {
							isCan = chatServer.ChangeChatName(beforeName, newChatName, this);
							if (isCan) {
								send("닉네임이 " + newChatName + "으로 바뀌었습니다.");
							} else {
								send("해당 닉네임으로 바꿀 수 없습니다. (중복 닉네임)");
							}
						}
					} else if (message.startsWith("/to")) {
						command = true;
						
						st = new StringTokenizer(message);
						
						thisCommand = st.nextToken();
						String nickNameToSend = st.nextToken();
						String sendMessage = message.substring(thisCommand.length() + 1 + nickNameToSend.length() + 1);
						sendMessage = "[귓속말] [" + chatName + "] (" + now.getCurrentTime() + ") : " + sendMessage;
						
						isCan = chatServer.sendToOne(this, nickNameToSend, sendMessage);
						if (isCan) {
							send(nickNameToSend + "에게 귓속말을 보냈습니다.");
						} else {
							send("해당 사용자는 현재 오프라인 입니다.");
						}
					} else if (message.startsWith("/img")) {
						command = true;
						
						st = new StringTokenizer(message);
						
						thisCommand = st.nextToken();
						String imageFileName = st.nextToken();
						
						send("/img " + imageFileName);
						
						receiveImage(imageFileName);
					} else if (message.startsWith("/download")) {
						command = true;
						
						String fileName = message.substring(10);
						
						sendImage(fileName);
					} else if (message.equals("/memberList")) {
						command = true;
						
						StringBuilder sb = chatServer.searchOnlineMember(this);
						send(sb.toString());
					}
					
					if (!command) {
						System.out.println("[" + displayName + "] (" + now.getCurrentTime() + ") : " + message);
					}
					if (message.equals(quitCommand)) break;
					if (!command) {
						chatServer.sendToAll(this, "[" + chatName + "] (" + now.getCurrentTime() + ") : " + message);
					} else if (command && isCan) {
						if (thisCommand.equals("/rename")) {
							chatServer.sendToAll(this, "[닉네임 변경]" + beforeName + " -> " + newChatName);
						} 
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			quit();
		}).start();
	}
	
	private void receiveImage(String imageFileName) {
		
		try {
	        System.out.println(chatName + " : [이미지 수신 시작] 파일 이름: " + imageFileName);
	        
	        // 파일 데이터 저장
	        try (
	        	FileOutputStream fos = new FileOutputStream("C:/chatProgramming/java work/java-friday/BasicChapApp/src/chat/server/" + "server_" + imageFileName)
	        	) {
	        	byte[] buffer = new byte[4096];
	        	int bytesRead;
	        	while ((bytesRead = dis.read(buffer)) != -1) {
	        		fos.write(buffer, 0, bytesRead);
	        		if (bytesRead < buffer.length) break;
	        	}
	        } 
	        System.out.println(chatName + " : [이미지 수신 완료] 파일 이름 : " + imageFileName);

	        chatServer.sendToAll(this, "/notify " + chatName + "님이 [" + imageFileName + "] 을 보냈습니다 다운 받으시려면 \"/download " + imageFileName + "\"를 입력하세요!");
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send(String message) {
		try {
			dos.writeUTF(message);
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendImage(String fileName) {
		// 서버에서 파일 읽기
		try (FileInputStream fis = new FileInputStream("C:/chatProgramming/java work/java-friday/BasicChapApp/src/chat/server/" + "server_" + fileName);
		     BufferedInputStream bis = new BufferedInputStream(fis)) {
		    
		    // 클라이언트에게 파일 전송 시작 알림
		    dos.writeUTF("/img"); // 이미지 전송 명령
		    dos.writeUTF(fileName); // 파일 이름 전송
		    
		    // 파일 데이터 전송
		    byte[] buffer = new byte[4096];
		    int bytesRead;
		    while ((bytesRead = bis.read(buffer)) != -1) {
		        dos.write(buffer, 0, bytesRead);
		    }
		    dos.flush();
		    System.out.println("[이미지 전송 완료] 파일 이름: " + fileName + " -> " + displayName);

		} catch (IOException e) {
		    System.err.println("[이미지 전송 오류] 파일 이름: " + fileName + " -> " + displayName);
		    e.printStackTrace();
		}
	}
	

	private void quit() {
		// 다른 참여 클라이언트들에게 퇴장 정보를 전달
		chatServer.sendToAll(this, "[퇴장] : " + displayName);
		
		// 서버에 저장된 클라이언트 정보 삭제
		chatServer.removeClientInfo(this);
		
		// 할당 받은 자원 close
		close();
	}
	
	public void close() {
		try {
			dis.close();
			dos.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
