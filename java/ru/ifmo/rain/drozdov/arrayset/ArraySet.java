package ru.ifmo.rain.drozdov.arrayset;


import java.util.*;

/**
 * Created by Gleb on 18.02.2018
 */
public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private List<E> collection;
    private Comparator<? super E> comparator;

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        this.collection = new ArrayList<>(collection);
        this.comparator = comparator;
        this.collection.sort(comparator);
        if (comparator != null) {
            List<E> uniqueList = new ArrayList<>();
            for (E e : this.collection) {
                if (uniqueList.size() == 0 || this.comparator.compare(uniqueList.get(uniqueList.size() - 1), e) != 0) {
                    uniqueList.add(e);
                }
            }
            this.collection = uniqueList;
        }
    }

    public ArraySet() {
        this(new ArrayList<>(), null);
    }

    public ArraySet(Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this(new ArrayList<>(), comparator);
    }

    private ArraySet(List<E> navigableSet, Comparator<? super E> comparator) {
        this.collection = navigableSet;
        this.comparator = comparator;
    }

    //0 - inclusive
    //1 - exclusive
    private int leftBinarySearch(E e, int isInclusive) {
        int index = Collections.binarySearch(collection, e, comparator);
        return index < 0 ? ~index - 1 : index - isInclusive;
    }

    private int rightBinarySearch(E e, int isInclusive) {
        int index = Collections.binarySearch(collection, e, comparator);
        return index < 0 ? ~index : index + isInclusive;
    }

    private boolean indexInside(int index) {
        return (index >= 0) && (index < size());
    }

    private E getElement(E e, int isInclusive, boolean isLower) {
        int index = isLower ? leftBinarySearch(e, isInclusive) : rightBinarySearch(e, isInclusive);
        return indexInside(index) ? collection.get(index) : null;
    }

    @Override
    public E lower(E e) {
        return getElement(e, 1, true);
    }

    @Override
    public E floor(E e) {
        return getElement(e, 0, true);
    }

    @Override
    public E ceiling(E e) {
        return getElement(e, 0, false);
    }

    @Override
    public E higher(E e) {
        return getElement(e, 1, false);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("ArraySet is immutable");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("ArraySet is immutable");
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return collection.size() == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(collection, (E) o, comparator) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(collection).iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        List<E> descendingSet = new DescendingSet<>(collection);
        return new ArraySet<>(descendingSet, comparator.reversed());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new Iterator<E>() {
            int it = collection.size() - 1;

            @Override
            public boolean hasNext() {
                return it != -1;
            }

            @Override
            public E next() {
                return collection.get(it--);
            }
        };
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        int from = rightBinarySearch(fromElement, fromInclusive ? 0 : 1);
        int to = leftBinarySearch(toElement, toInclusive ? 0 : 1);
        if (from > to) {
            return new ArraySet<>(new ArrayList<>(), comparator);
        }
        return new ArraySet<>(collection.subList(from, to + 1), comparator);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return subSet(size() == 0 ? null : first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return subSet(fromElement, inclusive, size() == 0 ? null : last(), true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        if (collection.size() == 0) {
            throw new NoSuchElementException();
        } else {
            return collection.get(0);
        }
    }

    @Override
    public E last() {
        if (collection.size() == 0) {
            throw new NoSuchElementException();
        } else {
            return collection.get(collection.size() - 1);
        }
    }
}
