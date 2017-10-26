/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nv.application;

/**
 *
 * @author nareen
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class Server {

    static int port = 3000;
    public static long lastTime = System.currentTimeMillis();
    public static long diff = 1000 * 60;
    public static int NoofClients = 0;

    public static void main(String[] args) {
        System.out.println("Server is running on port: " + port);
        try {
            new Timer().start();
            ServerSocket listener = new ServerSocket(port);
            while (true) {
                new ClientHandler(listener.accept()).start();
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}

class ClientHandler extends Thread {

    static HashMap<String, PrintWriter> clients = new HashMap<String, PrintWriter>();
    static HashMap<String, String> connections = new HashMap<String, String>();
    static Semaphore semaphore = new Semaphore(1, true);
    String senderName;
    String receiverName;
    Socket socket;
    BufferedReader reader;
    PrintWriter senderWriter;
    PrintWriter receiverWriter;
    int present = 0;

    public ClientHandler(Socket sock) {
        this.socket = sock;
    }

    @Override
    public void run() {
        try {
            System.out.println("Got new Connection");
            reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            senderWriter = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                senderWriter.println("Enter unique ID or your Name");
                senderName = reader.readLine();
                if (senderName == null) {
                    senderWriter.println("invalid Name or ID");
                } else {
                    semaphore.acquire();
                    int present = 0;
                    for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
                        if (entry.getKey().equals(senderName)) {
                            present = 1;
                            senderWriter.println("Name or ID already taken");
                            break;
                        }
                    }
                    semaphore.release();
                    if (present == 0) {
                        semaphore.acquire();
                        clients.put(senderName, senderWriter);
                        semaphore.release();
                        break;
                    }
                }
            }
            int connected = 0;
            for (Map.Entry<String, String> entry : connections.entrySet()) {
                if (entry.getValue().equals(senderName)) {
                    receiverName = entry.getKey();
                    connected = 1;
                    for (Map.Entry<String, PrintWriter> entry1 : clients.entrySet()) {
                        if (entry1.getKey().equals(receiverName)) {
                            receiverWriter = entry1.getValue();
                            break;
                        }
                    }
                    break;
                }
            }

            while (true && connected == 0) {
                senderWriter.println("Enter unique ID of person you want to connect to:");
                receiverName = reader.readLine();
                if (receiverName == null) {
                    senderWriter.println("invalid Name or ID of Client");
                } else {
                    connections.put(senderName, receiverName);
                    present = 0;
                    senderWriter.println(receiverName + " is not yet connected to the server. Please wait.");
                    while (true) {
                        semaphore.acquire();
                        for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
                            if (entry.getKey().equals(receiverName)) {
                                present = 1;
                                receiverWriter = entry.getValue();
                                senderWriter.println("Connected");
                                break;
                            }
                        }
                        semaphore.release();
                        if (present == 1) {
                            break;
                        }
                    }
                    if (present == 1) {
                        break;
                    }
                }
            }
            senderWriter.println("Connected");
            senderWriter.println("Key Exchange");
            Server.NoofClients = 2;

            while (true) {
                String input = reader.readLine();
                if (input.equals("disconnect")) {
                    senderWriter.println("Disconnected");
                    receiverWriter.println("Disconnected");
                    Server.NoofClients = 0;
                    connected = 0;
                    return;
                }
                receiverWriter.println(input);
            }
        } catch (Exception e) {
            System.out.println(e + ":" + senderName);
        } finally {
            try {
                connections.remove(senderName);
                clients.remove(senderName);
                socket.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}

class Timer extends Thread {

    @Override
    public void run() {
        while (true) {
            if ((System.currentTimeMillis() - Server.lastTime) >= Server.diff && Server.NoofClients == 2) {
                Server.lastTime = System.currentTimeMillis();
                try {
                    ClientHandler.semaphore.acquire();
                    for (Map.Entry<String, PrintWriter> entry : ClientHandler.clients.entrySet()) {
                        entry.getValue().println("Key Exchange");
                    }
                    ClientHandler.semaphore.release();
                } catch (Exception e) {
                }
            }
        }
    }
}
