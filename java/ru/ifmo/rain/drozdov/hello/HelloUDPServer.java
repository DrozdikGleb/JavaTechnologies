package ru.ifmo.rain.drozdov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Gleb on 12.05.2018
 */
public class HelloUDPServer implements HelloServer {
    private ExecutorService executorService;
    private DatagramSocket receivingSocket;

    @Override
    public void start(int port, int threads) {
        executorService = Executors.newFixedThreadPool(threads);
        try {
            receivingSocket = new DatagramSocket(port);
            for (int i = 0; i < threads; i++) {
                executorService.execute(() -> {
                    try {
                        while (!receivingSocket.isClosed()) {
                            DatagramPacket receivingPacket = new DatagramPacket(new byte[receivingSocket.getReceiveBufferSize()],
                                    0, receivingSocket.getReceiveBufferSize());
                            receivingSocket.receive(receivingPacket);
                            String receivedData = new String(receivingPacket.getData(), 0, receivingPacket.getLength());
                            byte[] answer = ("Hello, " + receivedData).getBytes(StandardCharsets.UTF_8);
                            receivingSocket.send(new DatagramPacket(answer, 0, answer.length, receivingPacket.getAddress(), receivingPacket.getPort()));
                        }
                    } catch (IOException e) {
                        if (!receivingSocket.isClosed()) {
                            System.err.println("Error occurs during work with datagram packets");
                        }
                    }
                });
            }
        } catch (SocketException e) {
            System.err.println("Problems with socket");
        }
    }

    @Override
    public void close() {
        receivingSocket.close();
        executorService.shutdownNow();
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Incorrect input");
            return;
        }
        try {
            int port = Integer.parseInt(args[0]);
            int threads = Integer.parseInt(args[1]);
            new HelloUDPServer().start(port, threads);
        } catch (NumberFormatException e) {
            System.err.println(e.getMessage());
        }
    }
}
