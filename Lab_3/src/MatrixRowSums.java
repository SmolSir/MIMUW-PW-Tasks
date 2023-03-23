import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.function.IntBinaryOperator;

public class MatrixRowSums {

    private static final int ROWS = 10;
    private static final int COLUMNS = 100;

    private static class Matrix {

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
    }

    public static void main(String[] args) {
        Matrix matrix = new Matrix(ROWS, COLUMNS, (row, column) -> {
            int a = 2 * column + 1;
            return (row + 1) * (a % 4 - 2) * a;
        });

        int[] rowSums = matrix.rowSums();
        System.out.println("Linear:");
        for (int i = 0; i < rowSums.length; i++) {
            System.out.println(i + " -> " + rowSums[i]);
        }

        int[] rowSumsConcurrent = matrix.rowSumsConcurrent();
        System.out.println("\nConcurrent:");
        for (int i = 0; i < rowSumsConcurrent.length; i++) {
            System.out.println(i + " -> " + rowSumsConcurrent[i]);
        }
    }

}
