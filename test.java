import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;

public class test {
	public static void main(String[] args) {
		ConcurrentHashMap<ServerInfo, Date> chm = new ConcurrentHashMap<ServerInfo, Date>();
		chm.put(new ServerInfo("192.168.0.1", 8192), new Date());

		System.out.println(chm.isEmpty());

		for(ServerInfo si : chm.keySet()){
			chm.remove(si);
		}

		System.out.println(chm.isEmpty());
	}
}