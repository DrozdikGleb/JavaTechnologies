package ru.ifmo.rain.drozdov.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by Gleb on 02.04.2018
 */
public class ParallelMapperImpl implements ParallelMapper {
    private int threads;
    private Thread[] threadsArray;
    private final LinkedList<Runnable> queue;

    private class Counter {
        private int counter = 0;

        private int getCounter() {
            return counter;
        }

        private void increment() {
            counter++;
        }
    }

    public ParallelMapperImpl(int threads) {
        this.threads = threads;
        threadsArray = new Thread[threads];
        queue = new LinkedList<>();
        for (int i = 0; i < threads; i++) {
            threadsArray[i] = new Thread(() -> {
                Runnable runnable;
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (queue) {
                        while (queue.isEmpty()) {
                            try {
                                queue.wait();
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                        runnable = queue.removeFirst();
                    }
                    runnable.run();
                }
            });
            threadsArray[i].start();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<R> answer = new ArrayList<>(args.size());
        final Counter counter = new Counter();
        for (int i = 0; i < args.size(); i++) {
            answer.add(null);
        }
        for (int i = 0; i < args.size(); i++) {
            int currentI = i;
            Runnable runnable = () -> {
                answer.set(currentI, f.apply(args.get(currentI)));
                synchronized (counter) {
                    counter.increment();
                    if (counter.getCounter() == args.size()) {
                        counter.notify();
                    }
                }
            };
            synchronized (queue) {
                queue.add(runnable);
                queue.notify();
            }
        }
        synchronized (counter) {
            while (counter.getCounter() < args.size()) {
                counter.wait();
            }
        }
        return answer;
    }

    @Override
    public void close() {
        for (int i = 0; i < threads; i++) {
            threadsArray[i].interrupt();
        }
        for (int i = 0; i < threads; i++) {
            try {
                threadsArray[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
