package ru.ifmo.rain.drozdov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Gleb on 12.05.2018
 */

public class HelloUDPClient implements HelloClient {
    private static final int TIMEOUT = 300;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
            ExecutorService executorService = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; i++) {
                int finalI = i;
                executorService.execute(() -> {
                            try (DatagramSocket datagramSocket = new DatagramSocket()) {
                                datagramSocket.setSoTimeout(TIMEOUT);
                                for (int j = 0; j < requests; j++) {
                                    String request = prefix + finalI + "_" + j;
                                    final byte[] bytesOfRequest = request.getBytes(StandardCharsets.UTF_8);
                                    String response;
                                    System.out.println(request);
                                    while (true) {
                                        try {
                                            try {
                                                datagramSocket.send(new DatagramPacket(bytesOfRequest, bytesOfRequest.length, address, port));
                                            } catch (IOException e) {
                                                System.err.println("Error occurred while sending request");
                                                continue;
                                            }
                                            DatagramPacket responsePacket = new DatagramPacket(new byte[datagramSocket.getReceiveBufferSize()],
                                                    0, datagramSocket.getReceiveBufferSize());
                                            datagramSocket.receive(responsePacket);
                                            response = new String(responsePacket.getData(), 0, responsePacket.getLength(), StandardCharsets.UTF_8);
                                            if (response.contains(request)) {
                                                System.out.println(response);
                                                break;
                                            }
                                        } catch (IOException e) {
                                            System.err.println("Error occurred during receiving answer from server");
                                        }
                                    }
                                }
                            } catch (SocketException socketException) {
                                System.err.println("Socket couldn't be created");
                            }
                        }
                );
            }
            executorService.shutdown();
            if (!executorService.awaitTermination(1000, TimeUnit.SECONDS)) {
                System.err.println("Threads didn't finish in 1000 seconds!");
            }
        } catch (UnknownHostException e) {
            System.err.println("Unknown host : " + host);
        } catch (InterruptedException e) {
            System.err.println("Problems with waiting");
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5 || args[0] == null || args[1] == null || args[2] == null || args[3] == null || args[4] == null) {
            System.out.println("Incorrect input");
            return;
        }
        try {
            int port = Integer.parseInt(args[1]);
            int threads = Integer.parseInt(args[3]);
            int requests = Integer.parseInt(args[4]);
            new HelloUDPClient().run(args[0], port, args[2], threads, requests);
        } catch (NumberFormatException e) {
            System.err.println(e.getMessage());
        }
    }
}
