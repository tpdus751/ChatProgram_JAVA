package chat.server;

import java.time.LocalDateTime;

public class CurrentTimeNow {
	
	private String currentTime;
	
	public CurrentTimeNow() {
		LocalDateTime now = LocalDateTime.now();
		
		int year = now.getYear();
		int monthValue = now.getMonthValue();
		int dayOfMonth = now.getDayOfMonth();
		int hour = now.getHour();
		int minute = now.getMinute();
		int seconde = now.getSecond();
		
		this.currentTime = year + "-" + monthValue + "-" + dayOfMonth + " " + hour + "시" + minute + "분";
	}

	public String getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(String currentTime) {
		this.currentTime = currentTime;
	}
	
	
}
