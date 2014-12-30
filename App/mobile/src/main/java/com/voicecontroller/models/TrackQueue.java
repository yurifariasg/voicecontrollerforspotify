package com.voicecontroller.models;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;


public class TrackQueue implements Queue<Track> {

    private LinkedList<Track> mainQueue;
    private Stack<Track> previousStack;

    public TrackQueue() {
        mainQueue = new LinkedList<>();
        previousStack = new Stack<>();
    }

    @Override
    public boolean add(Track track) {
        return mainQueue.add(track);
    }

    public Track previous() {
        if (!previousStack.isEmpty()) {
            mainQueue.addFirst(previousStack.pop());
        }
        return mainQueue.peek();
    }

    @Override
    public boolean offer(Track track) {
        return mainQueue.offer(track);
    }

    @Override
    public Track remove() {
        previousStack.add(mainQueue.remove());
        return previousStack.peek();
    }

    @Override
    public Track poll() {
        Track removedTrack = mainQueue.poll();
        if (removedTrack != null) {
            previousStack.add(removedTrack);
        }
        return removedTrack;
    }

    @Override
    public Track element() {
        return mainQueue.element();
    }

    @Override
    public Track peek() {
        return mainQueue.peek();
    }

    @Override
    public boolean addAll(Collection<? extends Track> collection) {
        return mainQueue.addAll(collection);
    }

    @Override
    public void clear() {
        // What should we do here? clear both ?
        mainQueue.clear();
        previousStack.clear();

    }

    @Override
    public boolean contains(Object object) {
        return mainQueue.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return mainQueue.contains(collection);
    }

    @Override
    public boolean isEmpty() {
        return mainQueue.isEmpty();
    }

    @NonNull
    @Override
    public Iterator<Track> iterator() {
        return mainQueue.iterator();
    }

    @Override
    public boolean remove(Object object) {
        return mainQueue.remove(object);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return mainQueue.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return mainQueue.retainAll(collection);
    }

    @Override
    public int size() {
        return mainQueue.size();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return mainQueue.toArray();
    }

    @NonNull
    @Override
    public <T> T[] toArray(T[] array) {
        return mainQueue.toArray(array);
    }
}
