package b.smolarczyk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ManyThreads {

    private static void printName() {
        System.out.println(Thread.currentThread());
    }

    private static ArrayList<Thread> ACTIVE_THREADS = new ArrayList<>();

    private static class Creator implements Runnable {

        private static final int THREADS_COUNT = 10;
        private static final int LOOP_ITERATIONS = 10000000;

        private final int id;

        public Creator(int id) {
            this.id = id + 1;
        }

        @Override
        public void run() {
            if (this.id == THREADS_COUNT) {
                return;
            }
            Thread t = new Thread(new Creator(this.id));
            t.start();
            ACTIVE_THREADS.add(t);
            for (int i = 0; i < LOOP_ITERATIONS; i++) {
                //double a = Math.random();
                double b = ThreadLocalRandom.current().nextDouble(1.0);
            }
            printName();
            ACTIVE_THREADS.remove(t);
        }
    }

    public static void main(String args[]) {

        Thread t = new Thread(new Creator(0));

        System.out.println("START");
        t.start();
        ACTIVE_THREADS.add(t);
        printName();
        while (t.isAlive()) {
            //pusta
        }
        ACTIVE_THREADS.remove(t);

        while (!ACTIVE_THREADS.isEmpty()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {

            }
        }

        System.out.println("KONIEC");
    }

}
