import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.ArrayList;
import java.util.Map.Entry;

public class ServerStatus {
	private ConcurrentHashMap<ServerInfo, Date> serverInfos;
	private int localPort;
	public ServerStatus() {
		serverInfos = new ConcurrentHashMap<ServerInfo, Date>();
	}

	public ServerStatus(String host, Integer port, Integer localPort) {
		serverInfos = new ConcurrentHashMap<ServerInfo, Date>();
		this.addAddressDetails(host, port);
		this.localPort = localPort;
	}

	public void addAddressDetails(String host, int port) {
		serverInfos.put(new ServerInfo(host, port), new Date());
	}

	public boolean hasAddressDetails(String host, int port) {
		for(Entry<ServerInfo, Date> entry : serverInfos.entrySet()) {
			ServerInfo key = entry.getKey();
			if(key.getHost().equals(host) && key.getPort() == port) return true;
		}

		return false;
	}

	public void removeAddressDetails(String host, int port){
		for(Entry<ServerInfo, Date> entry : serverInfos.entrySet()) {
			ServerInfo key = entry.getKey();
			if(key.getHost().equals(host) && key.getPort() == port) {
				serverInfos.remove(new ServerInfo(key.getHost(), key.getPort()));
				return;
			} 
		}
		System.out.println(serverInfos.isEmpty());
	}

	public int getLocalPort() {
		return localPort;
	}

	public int getNumServers() {
		return serverInfos.size();
	}

	public ArrayList<ServerInfo> getServerArray() {
		return new ArrayList<ServerInfo>(serverInfos.keySet());
	}

	public ConcurrentHashMap<ServerInfo, Date> getServerInfos() {
		return serverInfos;
	}

	public void updateDate(String host, int port) {
		for(Entry<ServerInfo, Date> entry : serverInfos.entrySet()) {
			ServerInfo key = entry.getKey();
			if(key.getHost().equals(host) && key.getPort() == port) serverInfos.put(key, new Date());
		}
	}
}