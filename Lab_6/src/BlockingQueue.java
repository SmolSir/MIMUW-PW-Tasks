package przyklady06;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Semaphore;

public class BlockingQueue<T> {

    private T zero_queue;
    private final ArrayList<T> list;
    private final int capacity;
    private int front;
    private int count;
    private boolean take_waiting;
    private boolean put_waiting;


    public BlockingQueue(int capacity) {
        this.list = new ArrayList<>(Collections.nCopies(capacity, null));
        this.capacity = capacity;
        this.front = 0;
        this.count = 0;
        this.zero_queue = null;
    }

    public synchronized T take() throws InterruptedException {
    /*    if (capacity == 0) {
            System.out.println("take");
            try {
                take_waiting = true;
                notifyAll();
                while(put_waiting == false) {
                    wait();
                }
                T value = zero_queue;
                zero_queue = null;
                take_waiting = false;
            } catch (InterruptedException e) {
                throw new InterruptedException();
            }
            notifyAll();
            return value;
        }*/
        try {
            while (count == 0) {
                wait();
            }
        } catch (InterruptedException e) {
            throw new InterruptedException();
        }

        T value = list.get(front);
        front = (front + 1) % capacity;
        count--;

        notifyAll();
        return value;
    }

    public synchronized void put(T item) throws InterruptedException {
    /*    if (capacity == 0) {
            System.out.println("put");
            try {
                while(take_waiting == false) {
                    wait();
                }
                zero_queue = item;
                put_waiting = true;
                notifyAll();
            } catch (InterruptedException e) {
                throw new InterruptedException();
            }
        }*/
        try {
            while (count == capacity) {
                wait();
            }
        } catch (InterruptedException e) {
            throw new InterruptedException();
        }

        list.set((front + count) % capacity, item);
        count++;

        notifyAll();
    }

    public synchronized int getSize() {
        return count;
    }

    public int getCapacity() {
        return capacity;
    }
}