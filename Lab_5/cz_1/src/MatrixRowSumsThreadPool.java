package przyklady05;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.IntBinaryOperator;


public class MatrixRowSumsThreadPool {
    private static final int ROWS = 10000000;
    private static final int COLUMNS = 10;
    private static final int MAX_ROWS_PER_THREAD = 1000000;
    private static final int SENSIBLE_PRINT_LIMIT = 100007;
    private static final int MOD = 1000;

    public static class Matrix {

        private final int rows;
        private final int columns;
        private final IntBinaryOperator definition;

        public Matrix(int rows, int columns, IntBinaryOperator definition) {
            this.rows = rows;
            this.columns = columns;
            this.definition = definition;
        }


        public int[] rowSums() {
            int[] rowSums = new int[rows];
            for (int row = 0; row < rows; ++row) {
                int sum = 0;
                for (int column = 0; column < columns; ++column) {
                    sum += definition.applyAsInt(row, column);
                }
                rowSums[row] = sum;
            }
            return rowSums;
        }

        public int[] rowSumsConcurrent() {

            int[] rowSums = new int[rows];
            int[] buffer = new int[columns];
            Thread[] threads = new Thread[columns];

            class BufferReader implements Runnable {
                private int row = 0;

                @Override
                public void run() {
                    for (int i : buffer) {
                        rowSums[row] += i;
                    }
                    row++;
                }
            }

            CyclicBarrier barrier = new CyclicBarrier(columns, new BufferReader());

            class Helper implements Runnable {

                private final int column;

                public Helper(int column) {
                    this.column = column;
                }

                @Override
                public void run() {
                    try {
                        for (int row = 0; row < rows; ++row) {
                            buffer[column] = definition.applyAsInt(row, column);
                            barrier.await();
                        }
                    } catch (InterruptedException | BrokenBarrierException e) {
                        for (Thread t : threads) {
                            t.interrupt();
                        }
                        System.err.println("Exception of thread " + Thread.currentThread().getName());
                    }
                }
            }

            for (int column = 0; column < columns; ++column) {
                threads[column] = new Thread(new Helper(column), "column" + column);
                threads[column].start();
            }

            try {
                for (Thread t : threads) {
                    t.join();
                }
            } catch (InterruptedException e) {
                for (Thread t : threads) {
                    t.interrupt();
                }
                System.err.println("Exception of thread Main");
            }


            return rowSums;
        }

        public int[] rowSumsThreadsafe() {
            ConcurrentHashMap<Integer, LongAdder> rowSums = new ConcurrentHashMap<>();
            ArrayList<Thread> threads = new ArrayList<>();

            class Helper implements Runnable {
                private final int column;
                private final int first_row;
                private final int last_row;

                public Helper (int column, int f_row, int l_row) {
                    this.column = column;
                    this.first_row = f_row;
                    this.last_row = l_row;
                }

                @Override
                public void run() {
                    try {
                        for (int row = first_row; row < last_row; ++row) {
                            int result = definition.applyAsInt(row, column);
                            rowSums.computeIfAbsent(row, value -> new LongAdder()).add(result);
                        }
                    } catch (Exception e) {
                        for (Thread t : threads) {
                            t.interrupt();
                        }
                        System.out.println("Interruption of thread no. " + column + " during computing");
                    }
                }
            }

            for (int column = 0; column < columns; ++column) {
                for (int row = 0; row <= rows; row += MAX_ROWS_PER_THREAD) {
                    Thread t = new Thread(new Helper(column, row, Math.min(row + MAX_ROWS_PER_THREAD, rows + 1)));
                    threads.add(t);
                    t.start();
                }
            }

            try {
                for (Thread t : threads) {
                    t.join();
                }
            } catch (InterruptedException e) {
                for (Thread t : threads) {
                    t.interrupt();
                }
                System.err.println("Exception of thread Main");
            }

            int[] result = new int[rows];
            for (int i = 0; i < rows; ++i) {
                result[i] = rowSums.get(i).intValue();
            }

            return result;
        }

        public int[] RowSumsThreadPool() {
            ConcurrentHashMap<Integer, LongAdder> rowSums = new ConcurrentHashMap<>();

            class ColumnCounter implements Callable<Void> {
                private final int column;

                private ColumnCounter(int column) {
                    this.column = column;
                }

                @Override
                public Void call() throws InterruptedException {
                    try {
                        for (int row = 0; row < rows; row++) {
                            int result = definition.applyAsInt(row, column);
                            rowSums.computeIfAbsent(row, value -> new LongAdder()).add(result);
                        }
                    } catch (Exception e) {
                        throw new InterruptedException();
                    }
                    return null;
                }
            }

            ExecutorService ThreadPool = Executors.newFixedThreadPool(4);
            ArrayList<ColumnCounter> tasks = new ArrayList<>();

            try {
                for (int col = 0; col < columns; col++) {
                    tasks.add(new ColumnCounter(col));
                }
                ThreadPool.invokeAll(tasks);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("computing stopped");
            } finally {
                ThreadPool.shutdown();
            }

            int[] result = new int[rows];
            for (int i = 0; i < rows; ++i) {
                result[i] = rowSums.get(i).intValue();
            }

            return result;
        }
    }

    public static void main(String[] args) {
        Matrix matrix = new Matrix(ROWS, COLUMNS, (row, column) -> {
            int a = 2 * column + 1;
            return (row + 1) * (a % 4 - 2) * a;
        });

        String[] times = new String[10];

        Instant timeWhenStarted = Clock.systemUTC().instant();
        int[] rowSums = matrix.rowSums();
        times[0] = Duration.between(timeWhenStarted, Clock.systemUTC().instant()).toString().replace("PT", "");

        System.out.println("Linear:");
        for (int i = 0; i < rowSums.length; i++) {
            if (i < SENSIBLE_PRINT_LIMIT || i % MOD == 0) {
                System.out.println(i + " -> " + rowSums[i]);
            }
        }

        if (ROWS < SENSIBLE_PRINT_LIMIT) {
            timeWhenStarted = Clock.systemUTC().instant();
            int[] rowSumsConcurrent = matrix.rowSumsConcurrent();
            times[1] = Duration.between(timeWhenStarted, Clock.systemUTC().instant()).toString().replace("PT", "");

            System.out.println("\nConcurrent:");
            for (int i = 0; i < rowSumsConcurrent.length; i++) {
                System.out.println(i + " -> " + rowSumsConcurrent[i]);
            }
        }

        timeWhenStarted = Clock.systemUTC().instant();
        int[] rowSumsThreadsafe = matrix.rowSumsThreadsafe();
        times[2] = Duration.between(timeWhenStarted, Clock.systemUTC().instant()).toString().replace("PT", "");


        System.out.println("\nThreadsafe:");
        for (int i = 0; i < rowSumsThreadsafe.length; i++) {
            if (i < SENSIBLE_PRINT_LIMIT || i % MOD == 0) {
                System.out.println(i + " -> " + rowSumsThreadsafe[i]);
            }
        }

        timeWhenStarted = Clock.systemUTC().instant();
        int[] rowSumsThreadPool = matrix.RowSumsThreadPool();
        times[3] = Duration.between(timeWhenStarted, Clock.systemUTC().instant()).toString().replace("PT", "");

        System.out.println("\nThreadPool:");
        for (int i = 0; i < rowSumsThreadPool.length; i++) {
            if (i < SENSIBLE_PRINT_LIMIT || i % MOD == 0) {
                System.out.println(i + " -> " + rowSumsThreadPool[i]);
            }
        }

        System.out.println("TIMES:");
        System.out.println("Linear:\t\t" + times[0]);
        System.out.println("Concurrent:\t" + times[1]);
        System.out.println("Threadsafe:\t" + times[2]);
        System.out.println("ThreadPool:\t" + times[3]);
    }

}
