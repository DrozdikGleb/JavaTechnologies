package ru.ifmo.rain.drozdov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by Gleb on 25.03.2018
 */
public class IterativeParallelism implements ListIP {
    private ParallelMapper parallelMapper;

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    public IterativeParallelism() {
        parallelMapper = null;
    }

    private <T> List<List<? extends T>> splitList(int threads, List<? extends T> list) {
        int elementsInThread = list.size() / threads;
        int mod = list.size() % threads;
        List<List<? extends T>> lists = new ArrayList<>();
        int prevPointer = 0;
        for (int i = 0; i < threads; i++) {
            int size = elementsInThread + (mod > 0 ? 1 : 0);
            lists.add(list.subList(prevPointer, prevPointer + size));
            prevPointer += size;
            mod--;
        }
        return lists;
    }

    private <T, R> List<R> delegateWork(int threads, List<? extends T> values, Function<List<? extends T>, R> function) throws InterruptedException {
        threads = Math.min(threads, values.size());
        List<List<? extends T>> lists = splitList(threads, values);
        if (parallelMapper != null) {
            return parallelMapper.map(function, lists);
        } else {
            Thread[] threadsArray = new Thread[threads];
            List<R> answerList = new ArrayList<>(threads);
            for (int i = 0; i < threads; i++) {
                answerList.add(null);
            }
            System.out.println(answerList.size() + " " + threads);
            for (int i = 0; i < threads; i++) {
                List<? extends T> curList = lists.get(i);
                int finalI = i;
                threadsArray[i] = new Thread(() -> answerList.set(finalI, function.apply(curList)));
                threadsArray[i].start();
            }
            for (int i = 0; i < threads; i++) {
                threadsArray[i].join();
            }
            return answerList;
        }
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        List<T> currentValues = delegateWork(threads, values, ts -> ts.stream().max(comparator).orElse(null));
        return currentValues.stream().max(comparator).orElse(null);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        List<Boolean> currentValues = delegateWork(threads, values, ts -> ts.stream().allMatch(predicate));
        return currentValues.stream().allMatch(bool -> bool);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        List<String> currentValues = delegateWork(threads, values, ts ->
                ts.stream().map(Object::toString).collect(Collectors.joining()));
        return currentValues.stream().map(Object::toString).collect(Collectors.joining());
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        List<List<T>> currentValues = delegateWork(threads, values, ts -> ts.stream().filter(predicate).collect(Collectors.toList()));
        return currentValues.stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        List<List<U>> currentValues = delegateWork(threads, values, ts -> ts.stream().map(f).collect(Collectors.toList()));
        return currentValues.stream().flatMap(Collection::stream).collect(Collectors.toList());
    }
}
