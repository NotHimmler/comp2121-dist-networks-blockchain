import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map.Entry;
import java.security.SecureRandom;

public class BlockchainServerRunnable implements Runnable{

    private Socket clientSocket;
    private Blockchain blockchain;
    private ServerStatus serverStatus;
    private String localIP;
    private String remoteIP;

    public BlockchainServerRunnable(Socket clientSocket, Blockchain blockchain, ServerStatus serverStatus) {
        this.clientSocket = clientSocket;
        this.blockchain = blockchain;
        this.serverStatus = serverStatus;
    }

    public void run() {
        try {
            this.localIP = (((InetSocketAddress) clientSocket.getLocalSocketAddress()).getAddress()).toString().replace("/", "");
            this.remoteIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress()).toString().replace("/", "");
            serverHandler(clientSocket.getInputStream(), clientSocket.getOutputStream());
            clientSocket.close();
        } catch (IOException e) {
        } catch(InterruptedException e) {
        }
    }

    public void serverHandler(InputStream clientInputStream, OutputStream clientOutputStream) throws InterruptedException {

        BufferedReader inputReader = new BufferedReader(
                new InputStreamReader(clientInputStream));
        PrintWriter outWriter = new PrintWriter(clientOutputStream, true);

        try {
            while (true) {
                String inputLine = inputReader.readLine();
                if (inputLine == null) {
                    break;
                }
                System.out.printf("%s\n", inputLine);
                String[] tokens = inputLine.split("\\|");
                boolean found = false;
                switch (tokens[0]) {
                    case "tx":
                        if (blockchain.addTransaction(inputLine))
                            outWriter.print("Accepted\n\n");
                        else
                            outWriter.print("Rejected\n\n");
                        outWriter.flush();
                        break;
                    case "pb":
                        outWriter.print(blockchain.toString() + "\n");
                        outWriter.flush();
                        break;
                    case "cc":
                        return;
                    case "hb":
                        if (tokens[2].equals("0")) {
                            ArrayList<Thread> threadArrayList = new ArrayList<>();
                            for (Entry<ServerInfo, Date> entry : serverStatus.getServerInfos().entrySet()) {
                                ServerInfo si = entry.getKey();
                                String ip = si.getHost();
                                int port = si.getPort();
                                if (ip.equals(remoteIP) && port == Integer.parseInt(tokens[1])) {
                                    continue;
                                }
                                Thread thread = new Thread(new HeartBeatClientRunnable(ip, port, String.format("si|%s|%s|%s", clientSocket.getLocalPort(), remoteIP, tokens[1])));
                                threadArrayList.add(thread);
                                thread.start();
                            }
                            for (Thread thread : threadArrayList) {
                                thread.join();
                            }
                            serverStatus.addAddressDetails(remoteIP, Integer.parseInt(tokens[1]));
                        } else {
                            serverStatus.updateDate(remoteIP, Integer.parseInt(tokens[1]));
                        }
                        break;
                    case "si":
                        if (!serverStatus.hasAddressDetails(tokens[2], Integer.parseInt(tokens[3]))) {
                            // relay
                            ArrayList<Thread> threadArrayList = new ArrayList<>();
                            for (Entry<ServerInfo, Date> entry : serverStatus.getServerInfos().entrySet()) {
                                ServerInfo si = entry.getKey();
                                String ip = si.getHost();
                                int port = si.getPort();
                                if ((ip.equals(tokens[2]) && port == Integer.parseInt(tokens[3])) || (ip.equals(remoteIP) && port == Integer.parseInt(tokens[1])) || (ip.equals(localIP) && port == serverStatus.getLocalPort())) {
                                    continue;
                                }
                                Thread thread = new Thread(new HeartBeatClientRunnable(ip, port, String.format("si|%s|%s|%s", serverStatus.getLocalPort(), tokens[2], tokens[3])));
                                threadArrayList.add(thread);
                                thread.start();
                            }
                            for (Thread thread : threadArrayList) {
                                thread.join();
                            }
                            serverStatus.addAddressDetails(tokens[2], Integer.parseInt(tokens[3]));
                        }
                        break;
                    case "lb":
                        Integer port = Integer.parseInt(tokens[1]);
                        Integer peerBcLength = Integer.parseInt(tokens[2]);
                        Integer ourBcLength = blockchain.getLength();
                        String peerHashString = tokens[3];
                        byte[] peerHash = Base64.getDecoder().decode(peerHashString);
                        
                        byte[] ourHash = blockchain.getHead() == null ? new byte[32] : blockchain.getHead().calculateHash();
                        boolean hashSmaller = false;
                        for(int i = 0; i < peerHash.length; i++) {
                            if (ourHash[i] < peerHash[i]) {
                                hashSmaller = true;
                                break;
                            }
                        }
                        if(ourBcLength < peerBcLength || (peerBcLength == ourBcLength && hashSmaller)) {
                            CatchupRunnable cr = new CatchupRunnable(blockchain, remoteIP, port);
                            Thread crt = new Thread(cr);
                            crt.start();
                        }
                        break;
                    case "cu":
                        ObjectOutputStream oos = new ObjectOutputStream(clientOutputStream);

                        SecureRandom randomGenerator = new SecureRandom();
                        int nonce = randomGenerator.nextInt();
                        blockchain.commit(nonce);
                        if(tokens.length == 1) {
                            oos.writeObject(blockchain.getHead());
                        } else if(tokens.length == 2) {
                            Block block = blockchain.getHead();
                            String reqBlockHash = tokens[1];
                            while(!Base64.getEncoder().encodeToString(block.getPreviousHash()).equals(reqBlockHash)) {
                                block = block.getPreviousBlock();
                            }
                            block = block.getPreviousBlock();
                            oos.writeObject(block);
                        }
                        break;
                    default:
                        outWriter.print("Error\n\n");
                        outWriter.flush();
                }
            }
        } catch (IOException e) {
        }
    }
}
