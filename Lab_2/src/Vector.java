import java.util.*;

public class Vector {

    public class SumException extends Exception {
        public SumException(String errormessage) {
            super(errormessage);
        }
    }

    public class DotException extends Exception {
        public DotException(String errormessage) {
            super(errormessage);
        }
    }

    private static final int INTERVAL_SIZE = 10;

    private int size;
    private ArrayList<Double> elements;

    public Vector (ArrayList<Double> elements) {
        this.size = elements.size();
        this.elements = elements;
    }

    private static class SumHelper implements Runnable {

        private ArrayList<Double> elements1;
        private ArrayList<Double> elements2;
        private ArrayList<Double> result;
        private int beg;
        private int end;

        public SumHelper(ArrayList<Double> elements1, ArrayList<Double> elements2, int beg, int end) {
            this.elements1 = elements1;
            this.elements2 = elements2;
            this.result = new ArrayList<>();
            this.beg = beg;
            this.end = end;
        }

        public double SumGet(int i) {
            return this.result.get(i);
        }

        @Override
        public void run() {
                for (int i = this.beg; i < this.end; i++) {
                        this.result.add(this.elements1.get(i) + this.elements2.get(i));
                }
        }
    }

    private static class MulHelper implements Runnable {
        private ArrayList<Double> elements1;
        private ArrayList<Double> elements2;
        private double result;
        private int beg;
        private int end;

        public MulHelper(ArrayList<Double> elements1, ArrayList<Double> elements2, int beg, int end) {
            this.elements1 = elements1;
            this.elements2 = elements2;
            this.result = 0.0;
            this.beg = beg;
            this.end = end;
        }

        public double MulGet() {
            return this.result;
        }

        @Override
        public void run() {
            for (int i = this.beg; i < this.end; i++) {
                this.result += this.elements1.get(i) * this.elements2.get(i);
            }
        }
    }

    public Vector sum(Vector other) throws SumException {
        if (this.size != other.size) {
            throw new SumException("Vectors can not be added due to different sizes");
        }
        List<Thread> threads = new ArrayList<>();
        List<SumHelper> helpers = new ArrayList<>();

        for (int i = 0; i < this.size; i += INTERVAL_SIZE) {
            SumHelper helper = new SumHelper(this.elements, other.elements, i, Math.min(i + INTERVAL_SIZE, this.size));
            Thread t = new Thread(helper);
            threads.add(t);
            helpers.add(helper);
            t.start();
        }

        ArrayList<Double> sum = new ArrayList<>();
        ArrayList<Boolean> serviced = new ArrayList<>();
        while (sum.size() < this.size) sum.add(0.0);
        while (serviced.size() < threads.size()) serviced.add(false);

        int ID = 0;
        int active_threads = threads.size();

        while (active_threads > 0) {
            Thread t = threads.get(ID);
            SumHelper helper = helpers.get(ID);
            if (!t.isAlive() && !serviced.get(ID)) {
                for (int i = 0; i < helper.result.size(); i++) {
                    sum.set(i + ID * INTERVAL_SIZE, helper.SumGet(i));
                }
                active_threads--;
                serviced.set(ID, true);
                try {
                    System.out.println(t.getName() + " finished computing");
                    t.join();
                }
                catch (InterruptedException e) {
                    for (Thread thread : threads) {
                        thread.interrupt();
                        throw new SumException("Error occured during addition of two vectors");
                    }
                }
            }
            ID = (ID + 1) % threads.size();
        }

        return new Vector(sum);
    }

    public double dot(Vector other) throws DotException {
        if (this.size != other.size) {
            throw new DotException("Vectors can not be added due to different sizes");
        }
        List<Thread> threads = new ArrayList<>();
        List<MulHelper> helpers = new ArrayList<>();

        for (int i = 0; i < this.size; i += INTERVAL_SIZE) {
            MulHelper helper = new MulHelper(this.elements, other.elements, i, Math.min(i + INTERVAL_SIZE, this.size));
            Thread t = new Thread(helper);
            threads.add(t);
            helpers.add(helper);
            t.start();
        }

        double result = 0.0;
        int ID = 0;
        int active_threads = threads.size();
        ArrayList<Boolean> serviced = new ArrayList<>();
        while (serviced.size() < threads.size()) serviced.add(false);

        while (active_threads > 0) {
            Thread t = threads.get(ID);
            if (!t.isAlive() && !serviced.get(ID)) {
                result += helpers.get(ID).result;
                active_threads--;
                serviced.set(ID, true);
                try {
                    System.out.println(t.getName() + " finished computing");
                    t.join();
                }
                catch (InterruptedException e) {
                    for (Thread thread : threads) {
                        thread.interrupt();
                        throw new DotException("Error occured during dotting of two vectors");
                    }
                }
            }
            ID = (ID + 1) % threads.size();
        }

        return result;
    }

    public Vector linearSum(Vector other) {
        ArrayList<Double> sum = new ArrayList<>();
        for (int i = 0; i < this.size; i++) {
            sum.add(this.elements.get(i) + other.elements.get(i));
        }
        return new Vector(sum);
    }

    public double linearDot(Vector other) {
        double dot = 0.0;
        for (int i = 0; i < this.size; i++) {
            dot += this.elements.get(i) * other.elements.get(i);
        }

        return dot;
    }

    public static void main(String args[]) {

        Vector v1 = new Vector(new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0)));
        Vector v2 = new Vector(new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0)));
        try {
            Vector v1_2 = v1.sum(v2);
            Vector v1_2lin = v1.linearSum(v2);
            System.out.println("vector 1: " + v1.elements);
            System.out.println("vector 2: " + v2.elements);
            System.out.println("vector 1 + 2 thread: " + v1_2.elements);
            System.out.println("vector 1 + 2 linear: " + v1_2lin.elements + "\n");
        }
        catch (SumException e) {
            System.out.println("Error occured during addition of two vectors");
        }

        try {
            double v1dot3 = v1.dot(v2);System.out.println("vector 1: " + v1.elements);
            double v1dot3lin = v1.linearDot(v2);
            System.out.println("vector 2: " + v2.elements);
            System.out.println("vector 1 * 2 thread: " + v1dot3);
            System.out.println("vector 1 * 2 linear: " + v1dot3lin + "\n\n");
        }
        catch (DotException e) {
            System.out.println("Error occured during addition of two vectors");
        }

        ArrayList<Double> creator1 = new ArrayList<>();
        ArrayList<Double> creator2 = new ArrayList<>();
        Random r = new Random();
        int rangeMin = -1000;
        int rangeMax = 1000;
        for (int i = 0; i < 45; i++) {
            double d = r.nextDouble();
            d = Math.round(d * 100.0) / 100.0;
            creator1.add(d);
            d = r.nextDouble();
            d = Math.round(d * 100.0) / 100.0;
            creator2.add(d);
        }

        Vector v3 = new Vector(creator1);
        Vector v4 = new Vector(creator2);

        try {
            Vector v3_4 = v3.sum(v4);
            Vector v3_4lin = v3.linearSum(v4);
            //System.out.println("vector 3: " + v3.elements);
            //System.out.println("vector 4: " + v4.elements);
            //System.out.println("vector 3 + 4 thread: " + v3_4.elements);
            //System.out.println("vector 3 + 4 linear: " + v3_4lin.elements + "\n");
        }
        catch (SumException e) {
            System.out.println("Error occured during addition of two vectors");
        }

        try {
            double v3dot4 = v3.dot(v4);
            //System.out.println("vector 3: " + v3.elements);
            double v3dot4lin = v3.linearDot(v4);
            //System.out.println("vector 4: " + v4.elements);
            System.out.println("vector 3 * 4 thread: " + v3dot4);
            System.out.println("vector 3 * 4 linear: " + v3dot4lin);
        }
        catch (DotException e) {
            System.out.println("Error occured during addition of two vectors");
        }
    }
}
