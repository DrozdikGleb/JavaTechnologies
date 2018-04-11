package ru.ifmo.rain.drozdov.arrayset;

import java.util.AbstractList;
import java.util.List;

/**
 * Created by Gleb on 28.02.2018
 */
public class DescendingSet<E> extends AbstractList<E> {
    private final List<E> descendingList;

    public DescendingSet(List<E> descendingList) {
        this.descendingList = descendingList;
    }

    @Override
    public int size() {
        return descendingList.size();
    }

    @Override
    public E get(int index) {
        return descendingList.get(size() - index - 1);
    }
}
