package concurrentcube;

import org.junit.jupiter.api.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

class CubeTest {

    private static final int TOP = 0;
    private static final int LEFT = 1;
    private static final int FRONT = 2;
    private static final int RIGHT = 3;
    private static final int BACK = 4;
    private static final int BOTTOM = 5;

    private static int test_num = 1;
    private static final int BS_DELAY = 500; // BeforeShow delay
    private static final int AS_DELAY = 1000; // AfterShow delay
    private static final int BR_DELAY = 250; // BeforeRotation delay
    private static final int AR_DELAY = 250; // AfterRotation delay
    private static final int LOW_DELAY = 10; // for large tests, we make all delays very low
    private static final int START_DELAY = 1; // for debugging concurrent rotations, since threads on a list can start
                                                // out of order and mess up test results because of system imperfection
    private static boolean USE_LOW_DELAY = false;
    private static TimeUnit UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
    private static final boolean ASSERT = true;

    static class BeforeShowing implements Runnable {


        @Override
        public void run() {
            try {
                System.out.println("Thread " + Thread.currentThread().getName() + ": Ready to take picture of the cube...");
                UNIT_OF_DELAY.sleep(USE_LOW_DELAY ? LOW_DELAY : BS_DELAY);
            } catch (InterruptedException e) {
                System.out.println("Thread " + Thread.currentThread().getName() + ": Interrupted during BeforeShowing()");
            }
        }
    }

    static class AfterShowing implements Runnable {


        @Override
        public void run() {
            try {
                System.out.println("Thread " + Thread.currentThread().getName() + ": Cube picture taken successfully!");
                UNIT_OF_DELAY.sleep(USE_LOW_DELAY ? LOW_DELAY : AS_DELAY);
            } catch (InterruptedException e) {
                System.out.println("Thread " + Thread.currentThread().getName() + ": Interrupted during AfterShowing()");
            }
        }
    }

    BiConsumer<Integer, Integer> pre_rotate = (side, layer) -> {
        try {
            System.out.println("Thread " + Thread.currentThread().getName() + ": Rotate side " + side + ", layer " + layer + "...");
            UNIT_OF_DELAY.sleep(USE_LOW_DELAY ? LOW_DELAY : BR_DELAY);
        } catch (InterruptedException e) {
            System.out.println("Thread " + Thread.currentThread().getName() + ": Interrupted during beforeRotation()");
        }
    };
    BiConsumer<Integer, Integer> post_rotate = (side, layer) -> {
        try {
            System.out.println("Thread " + Thread.currentThread().getName() + ": Side " + side + ", layer " + layer + " rotated successfully!");
            UNIT_OF_DELAY.sleep(USE_LOW_DELAY ? LOW_DELAY : AR_DELAY);
        } catch (InterruptedException e) {
            System.out.println("Thread " + Thread.currentThread().getName() + ": Interrupted during afterRotation()");
        }
    };
    BeforeShowing beforeShowing = new BeforeShowing();
    AfterShowing afterShowing = new AfterShowing();

    @BeforeAll
    @DisplayName("Starting testing procedure...")
    static void before_testing() {
        System.out.println("ready for some testing?");
    }

    @BeforeEach
    void before_each_test() {
        System.out.println("\nTEST " + test_num++);
    }

    @Test
    @Order(100)
    @DisplayName("Create 1x1 cube")
    void create_1x1() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_1x1 = new Cube(1, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            String cube_1x1_show;
            cube_1x1_show = cube_1x1.show();
            System.out.println(cube_1x1_show);
            if (ASSERT) {
                assert cube_1x1_show.equals("012345");
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }

    @Test
    @Order(101)
    @DisplayName("Rotate 1x1 cube top")
    void rotate_1x1_top() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_1x1 = new Cube(1, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            cube_1x1.rotate(TOP, 0);
            String cube_1x1_show;
            cube_1x1_show = cube_1x1.show();
            System.out.println(cube_1x1_show);
            if (ASSERT) {
                assert cube_1x1_show.equals("023415");
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }

    @Test
    @Order(102)
    @DisplayName("Rotate 1x1 cube front")
    void rotate_1x1_front() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_1x1 = new Cube(1, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            cube_1x1.rotate(FRONT, 0);
            String cube_1x1_show;
            cube_1x1_show = cube_1x1.show();
            System.out.println(cube_1x1_show);
            if (ASSERT) {
                assert cube_1x1_show.equals("152043");
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }

    @Test
    @Order(103)
    @DisplayName("Rotate 1x1 cube multiple")
    void rotate_1x1_multiple() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_1x1 = new Cube(1, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            cube_1x1.rotate(TOP, 0);
            cube_1x1.rotate(FRONT, 0);
            String cube_1x1_show;
            cube_1x1_show = cube_1x1.show();
            System.out.println(cube_1x1_show);
            if (ASSERT) {
                assert cube_1x1_show.equals("253014");
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }

    @Test
    @Order(200)
    @DisplayName("Create 4x4 cube")
    void create_4x4() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            String cube_4x4_show;
            cube_4x4_show = cube_4x4.show();
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert (cube_4x4_show.equals(
                                "0000" +
                                "0000" +
                                "0000" +
                                "0000" +

                                "1111" +
                                "1111" +
                                "1111" +
                                "1111" +

                                "2222" +
                                "2222" +
                                "2222" +
                                "2222" +

                                "3333" +
                                "3333" +
                                "3333" +
                                "3333" +

                                "4444" +
                                "4444" +
                                "4444" +
                                "4444" +

                                "5555" +
                                "5555" +
                                "5555" +
                                "5555")
                );
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }

    @Test
    @Order(201)
    @DisplayName("Rotate 4x4 inner layer")
    void rotate_4x4_inner() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            cube_4x4.rotate(2, 1);
            String cube_4x4_show;
            cube_4x4_show = cube_4x4.show();
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert (cube_4x4_show.equals(
                                "0000" +
                                "0000" +
                                "1111" +
                                "0000" +

                                "1151" +
                                "1151" +
                                "1151" +
                                "1151" +

                                "2222" +
                                "2222" +
                                "2222" +
                                "2222" +

                                "3033" +
                                "3033" +
                                "3033" +
                                "3033" +

                                "4444" +
                                "4444" +
                                "4444" +
                                "4444" +

                                "5555" +
                                "3333" +
                                "5555" +
                                "5555")
                );
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }

    @Test
    @Order(202)
    @DisplayName("Rotate 4x4 top 0-th layer")
    void rotate_4x4_top_0th() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            cube_4x4.rotate(2, 1);
            cube_4x4.rotate(0,0);
            String cube_4x4_show;
            cube_4x4_show = cube_4x4.show();
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert(cube_4x4_show.equals(
                                "0100" +
                                "0100" +
                                "0100" +
                                "0100" +

                                "2222" +
                                "1151" +
                                "1151" +
                                "1151" +

                                "3033" +
                                "2222" +
                                "2222" +
                                "2222" +

                                "4444" +
                                "3033" +
                                "3033" +
                                "3033" +

                                "1151" +
                                "4444" +
                                "4444" +
                                "4444" +

                                "5555" +
                                "3333" +
                                "5555" +
                                "5555")
                );
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }

    @Test
    @Order(203)
    @DisplayName("Rotate 4x4 top last layer")
    void rotate_4x4_top_last() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            cube_4x4.rotate(2, 1);
            cube_4x4.rotate(0,3);
            String cube_4x4_show;
            cube_4x4_show = cube_4x4.show();
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert(cube_4x4_show.equals(
                                "0000" +
                                "0000" +
                                "1111" +
                                "0000" +

                                "1151" +
                                "1151" +
                                "1151" +
                                "2222" +

                                "2222" +
                                "2222" +
                                "2222" +
                                "3033" +

                                "3033" +
                                "3033" +
                                "3033" +
                                "4444" +

                                "4444" +
                                "4444" +
                                "4444" +
                                "1151" +

                                "5355" +
                                "5355" +
                                "5355" +
                                "5355")
                );
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }

    @Test
    @Order(300)
    @DisplayName("Rotate 1x1 cube full loop")
    void rotate_1x1_full_loop() {
        USE_LOW_DELAY = true;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_1x1 = new Cube(1, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            for (int i = 0; i < 3; i++) {
                cube_1x1.rotate(TOP, 0);
                cube_1x1.rotate(FRONT, 0);
            }
            String cube_1x1_show;
            cube_1x1_show = cube_1x1.show();
            System.out.println(cube_1x1_show);
            if (ASSERT) {
                assert cube_1x1_show.equals("012345");
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }

    @Test
    @Order(301)
    @DisplayName("Rotate 3x3 cube full loop")
    void rotate_3x3_full_loop() {
        USE_LOW_DELAY = true;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_3x3 = new Cube(3, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            for (int i = 0; i < 105; i++) {
                cube_3x3.rotate(TOP, 0);
                cube_3x3.rotate(FRONT, 0);
            }
            String cube_3x3_show;
            cube_3x3_show = cube_3x3.show();
            System.out.println(cube_3x3_show);
            if (ASSERT) {
                assert (cube_3x3_show.equals(
                                "000" +
                                "000" +
                                "000" +

                                "111" +
                                "111" +
                                "111" +

                                "222" +
                                "222" +
                                "222" +

                                "333" +
                                "333" +
                                "333" +

                                "444" +
                                "444" +
                                "444" +

                                "555" +
                                "555" +
                                "555")
                );
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }

    @Test
    @Order(302)
    @DisplayName("Rotate 4x4 cube full loop")
    void rotate_4x4_full_loop() {
        USE_LOW_DELAY = true;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            for (int i = 0; i < 105; i++) { // for size >= 3, it should always be the same no. of rotations
                cube_4x4.rotate(TOP, 0);
                cube_4x4.rotate(FRONT, 0);
            }
            String cube_4x4_show;
            cube_4x4_show = cube_4x4.show();
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert (cube_4x4_show.equals(
                                "0000" +
                                "0000" +
                                "0000" +
                                "0000" +

                                "1111" +
                                "1111" +
                                "1111" +
                                "1111" +

                                "2222" +
                                "2222" +
                                "2222" +
                                "2222" +

                                "3333" +
                                "3333" +
                                "3333" +
                                "3333" +

                                "4444" +
                                "4444" +
                                "4444" +
                                "4444" +

                                "5555" +
                                "5555" +
                                "5555" +
                                "5555")
                );
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }

    @Test
    @Order(400)
    @DisplayName("Rotate 3x3 dot pattern")
    void rotate_3x3_dot() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_3x3 = new Cube(3, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            ArrayList<Integer> sides = new ArrayList<>(Arrays.asList(RIGHT, TOP, FRONT, BOTTOM, LEFT, BACK));
            for (var side : sides) {
                cube_3x3.rotate(side, 1);
            }
            String cube_3x3_show;
            cube_3x3_show = cube_3x3.show();
            System.out.println(cube_3x3_show);
            if (ASSERT) {
                assert (cube_3x3_show.equals(
                                "000" +
                                "030" +
                                "000" +

                                "111" +
                                "121" +
                                "111" +

                                "222" +
                                "252" +
                                "222" +

                                "333" +
                                "343" +
                                "333" +

                                "444" +
                                "404" +
                                "444" +

                                "555" +
                                "515" +
                                "555")
                );
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }

    @Test
    @Order(401)
    @DisplayName("Rotate 3x3 flower pattern")
    void rotate_3x3_flower() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_3x3 = new Cube(3, pre_rotate, post_rotate, beforeShowing, afterShowing);
        try {
            ArrayList<Integer> sides = new ArrayList<>(Arrays.asList(RIGHT, RIGHT, TOP, TOP, FRONT, FRONT));
            for (var side : sides) {
                cube_3x3.rotate(side, 1);
            }
            String cube_3x3_show;
            cube_3x3_show = cube_3x3.show();
            System.out.println(cube_3x3_show);
            if (ASSERT) {
                assert (cube_3x3_show.equals(
                                "050" +
                                "505" +
                                "050" +

                                "131" +
                                "313" +
                                "131" +

                                "242" +
                                "424" +
                                "242" +

                                "313" +
                                "131" +
                                "313" +

                                "424" +
                                "242" +
                                "424" +

                                "505" +
                                "050" +
                                "505")
                );
            }
        } catch (InterruptedException e) {
            System.out.println("TEST " + test_num + " threw interrupted exception!");
        }
    }


    static class Rotor implements Runnable {
        private final Cube cube;
        private final int side;
        private final int layer;

        public Rotor(Cube cube, int side, int layer) {
            this.cube = cube;
            this.side = side;
            this.layer = layer;
        }

        @Override
        public void run() {
            try {
                cube.rotate(side, layer);
            } catch (InterruptedException e) {
                System.out.println("Thread " + Thread.currentThread().getName() + " : Interrupted when rotating side " + side + ", layer " + layer);
            }
        }
    }

    static class Show implements Runnable {
        private final Cube cube;
        public String result;

        public Show(Cube cube) {
            this.cube = cube;
        }

        @Override
        public void run() {
            try {
                result = cube.show();
            } catch (InterruptedException e) {
                System.out.println("Thread " + Thread.currentThread().getName() + " : Interrupted when showing cube");
            }
        }
    }

    @Test
    @Order(500)
    @DisplayName("Concurrent 4x4 no collisions no repeats")
    public void concurrent_4x4_no_collisions_no_repeats() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        Show object_show = new Show(cube_4x4);
        Thread show_thread = new Thread(object_show);
        ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            threads.add(new Thread(new Rotor(cube_4x4, FRONT, i)));
        }

        try {
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                thread.interrupt();
            }
            System.out.println("Rotor thread interrupted");
        }
        try {
            String cube_4x4_show;
            show_thread.start();
            show_thread.join();
            cube_4x4_show = object_show.result;
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert (cube_4x4_show.equals(
                                "1111" +
                                "1111" +
                                "1111" +
                                "1111" +

                                "5555" +
                                "5555" +
                                "5555" +
                                "5555" +

                                "2222" +
                                "2222" +
                                "2222" +
                                "2222" +

                                "0000" +
                                "0000" +
                                "0000" +
                                "0000" +

                                "4444" +
                                "4444" +
                                "4444" +
                                "4444" +

                                "3333" +
                                "3333" +
                                "3333" +
                                "3333")
                );
            }
        } catch (InterruptedException e) {
            show_thread.interrupt();
            System.out.println("Show thread interrupted");
        }
    }

    @Test
    @Order(501)
    @DisplayName("Concurrent 4x4 no collisions full loop shuffled order")
    public void concurrent_4x4_no_collisions_full_loop_shuffled_order() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        Show object_show = new Show(cube_4x4);
        Thread show_thread = new Thread(object_show);
        ArrayList<Thread> threads = new ArrayList<>();
        List<Integer> layers = Arrays.asList(0, 1, 2, 3);
        for (int i = 0; i < 4; i++) {
            Collections.shuffle(layers);
            for (int layer : layers) {
                threads.add(new Thread(new Rotor(cube_4x4, FRONT, layer)));
            }
        }

        try {
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                thread.interrupt();
            }
            System.out.println("Rotor thread interrupted");
        }
        try {
            String cube_4x4_show;
            show_thread.start();
            show_thread.join();
            cube_4x4_show = object_show.result;
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert (cube_4x4_show.equals(
                                "0000" +
                                "0000" +
                                "0000" +
                                "0000" +

                                "1111" +
                                "1111" +
                                "1111" +
                                "1111" +

                                "2222" +
                                "2222" +
                                "2222" +
                                "2222" +

                                "3333" +
                                "3333" +
                                "3333" +
                                "3333" +

                                "4444" +
                                "4444" +
                                "4444" +
                                "4444" +

                                "5555" +
                                "5555" +
                                "5555" +
                                "5555")
                );
            }
        } catch (InterruptedException e) {
            show_thread.interrupt();
            System.out.println("Show thread interrupted");
        }
    }

    @Test
    @Order(502)
    @DisplayName("Concurrent 4x4 no collisions countering rotates shuffled order")
    public void concurrent_4x4_no_collisions_countering_rotates_shuffled_order() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        Show object_show = new Show(cube_4x4);
        Thread show_thread = new Thread(object_show);
        ArrayList<Thread> threads = new ArrayList<>();
        List<Integer> faces = Arrays.asList(FRONT, FRONT, FRONT, FRONT, BACK, BACK, BACK, BACK);
        for (int layer = 0; layer < 4; layer++) {
            Collections.shuffle(faces);
            for (int face : faces) {
                threads.add(new Thread(new Rotor(cube_4x4, face, layer)));
            }
        }

        try {
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                thread.interrupt();
            }
            System.out.println("Rotor thread interrupted");
        }
        try {
            String cube_4x4_show;
            show_thread.start();
            show_thread.join();
            cube_4x4_show = object_show.result;
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert (cube_4x4_show.equals(
                                "0000" +
                                "0000" +
                                "0000" +
                                "0000" +

                                "1111" +
                                "1111" +
                                "1111" +
                                "1111" +

                                "2222" +
                                "2222" +
                                "2222" +
                                "2222" +

                                "3333" +
                                "3333" +
                                "3333" +
                                "3333" +

                                "4444" +
                                "4444" +
                                "4444" +
                                "4444" +

                                "5555" +
                                "5555" +
                                "5555" +
                                "5555")
                );
            }
        } catch (InterruptedException e) {
            show_thread.interrupt();
            System.out.println("Show thread interrupted");
        }
    }

    @Test
    @Order(503)
    @DisplayName("Concurrent 4x4 full tumble loop shuffled order")
    public void concurrent_4x4_full_tumble_loop_shuffled_order() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        Show object_show = new Show(cube_4x4);
        Thread show_thread = new Thread(object_show);
        ArrayList<Thread> threads = new ArrayList<>();
        List<Integer> faces = Arrays.asList(RIGHT, RIGHT, BACK, BACK, LEFT, LEFT, FRONT, FRONT);
        List<Integer> layers = Arrays.asList(0, 1, 2, 3);
        for (int face : faces) {
            Collections.shuffle(layers);
            for (int layer : layers) {
                threads.add(new Thread(new Rotor(cube_4x4, face, layer)));
            }
        }

        try {
            for (Thread thread : threads) {
                thread.start();
                TimeUnit.MILLISECONDS.sleep(START_DELAY);
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                thread.interrupt();
            }
            System.out.println("Rotor thread interrupted");
        }
        try {
            String cube_4x4_show;
            show_thread.start();
            show_thread.join();
            cube_4x4_show = object_show.result;
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert (cube_4x4_show.equals(
                                "0000" +
                                "0000" +
                                "0000" +
                                "0000" +

                                "1111" +
                                "1111" +
                                "1111" +
                                "1111" +

                                "2222" +
                                "2222" +
                                "2222" +
                                "2222" +

                                "3333" +
                                "3333" +
                                "3333" +
                                "3333" +

                                "4444" +
                                "4444" +
                                "4444" +
                                "4444" +

                                "5555" +
                                "5555" +
                                "5555" +
                                "5555")
                );
            }
        } catch (InterruptedException e) {
            show_thread.interrupt();
            System.out.println("Show thread interrupted");
        }
    }

    @Test
    @Order(504)
    @DisplayName("Concurrent 4x4 half loop")
    public void concurrent_4x4_half_loop() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        Show object_show = new Show(cube_4x4);
        Thread show_thread = new Thread(object_show);
        ArrayList<Thread> threads = new ArrayList<>();
        List<Integer> faces = Arrays.asList(FRONT, TOP, TOP, FRONT, BOTTOM, BOTTOM);
        List<Integer> layers = Arrays.asList(0, 1, 2, 3);
        for (int face : faces) {
            Collections.shuffle(layers);
            for (int layer : layers) {
                threads.add(new Thread(new Rotor(cube_4x4, face, layer)));
            }
        }

        try {
            for (Thread thread : threads) {
                thread.start();
                TimeUnit.MILLISECONDS.sleep(START_DELAY);
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                thread.interrupt();
            }
            System.out.println("Rotor thread interrupted");
        }
        try {
            String cube_4x4_show;
            show_thread.start();
            show_thread.join();
            cube_4x4_show = object_show.result;
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert (cube_4x4_show.equals(
                                "0000" +
                                "0000" +
                                "0000" +
                                "0000" +

                                "1111" +
                                "1111" +
                                "1111" +
                                "1111" +

                                "2222" +
                                "2222" +
                                "2222" +
                                "2222" +

                                "3333" +
                                "3333" +
                                "3333" +
                                "3333" +

                                "4444" +
                                "4444" +
                                "4444" +
                                "4444" +

                                "5555" +
                                "5555" +
                                "5555" +
                                "5555")
                );
            }
        } catch (InterruptedException e) {
            show_thread.interrupt();
            System.out.println("Show thread interrupted");
        }
    }

    @Test
    @Order(600)
    @DisplayName("Concurrent 4x4 security")
    public void concurrent_4x4_security() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        Show object_show = new Show(cube_4x4);
        Thread show_thread = new Thread(object_show);
        // This can cause all axis to rotate at once if the implementation lacks security
        ArrayList<Thread> threads_with_intervals = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            threads_with_intervals.add(new Thread(new Rotor(cube_4x4, FRONT, 0)));
        }
        try {
            for (Thread thread : threads_with_intervals) {
                thread.start();
                TimeUnit.MILLISECONDS.sleep(100);
            }
            for (Thread thread : threads_with_intervals) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads_with_intervals) {
                thread.interrupt();
            }
            System.out.println("Rotor thread interrupted");
        }

        ArrayList<Thread> threads = new ArrayList<>();
        List<Integer> faces = Arrays.asList(FRONT, TOP, TOP, FRONT, BOTTOM, BOTTOM);
        List<Integer> layers = Arrays.asList(0, 1, 2, 3);
        for (int face : faces) {
            Collections.shuffle(layers);
            for (int layer : layers) {
                threads.add(new Thread(new Rotor(cube_4x4, face, layer)));
            }
        }

        try {
            for (Thread thread : threads) {
                thread.start();
                TimeUnit.MILLISECONDS.sleep(START_DELAY);
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                thread.interrupt();
            }
            System.out.println("Rotor thread interrupted");
        }
        try {
            String cube_4x4_show;
            show_thread.start();
            show_thread.join();
            cube_4x4_show = object_show.result;
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert (cube_4x4_show.equals(
                                "0000" +
                                "0000" +
                                "0000" +
                                "0000" +

                                "1111" +
                                "1111" +
                                "1111" +
                                "1111" +

                                "2222" +
                                "2222" +
                                "2222" +
                                "2222" +

                                "3333" +
                                "3333" +
                                "3333" +
                                "3333" +

                                "4444" +
                                "4444" +
                                "4444" +
                                "4444" +

                                "5555" +
                                "5555" +
                                "5555" +
                                "5555")
                );
            }
        } catch (InterruptedException e) {
            show_thread.interrupt();
            System.out.println("Show thread interrupted");
        }
    }

    @Test
    @Order(601)
    @DisplayName("Concurrent 4x4 exception handling - interrupt waiting threads")
    public void concurrent_4x4_exception_handling_waiting_threads() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        Show object_show = new Show(cube_4x4);
        Thread show_thread = new Thread(object_show);
        ArrayList<Thread> threads = new ArrayList<>();
        for (int layer = 0; layer < 4; layer++) {
            for (int i = 0; i < 13; i++) {
                threads.add(new Thread(new Rotor(cube_4x4, FRONT, layer)));
            }
        }

        try {
            for (Thread thread : threads) {
                thread.start();
                TimeUnit.MILLISECONDS.sleep(START_DELAY);
            }
            // now we interrupt one random thread other than 1st rotating each layer
            Random r = new Random();
            for (int i = 0; i < threads.size(); i += 13) {
                int target = i + r.nextInt(11) + 1;
                threads.get(target).interrupt();
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                if (thread.isInterrupted()) {
                    System.out.println("Rotor " + thread.getName() + " interrupted");
                }
            }

        }
        try {
            String cube_4x4_show;
            show_thread.start();
            show_thread.join();
            cube_4x4_show = object_show.result;
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert (cube_4x4_show.equals(
                                "0000" +
                                "0000" +
                                "0000" +
                                "0000" +

                                "1111" +
                                "1111" +
                                "1111" +
                                "1111" +

                                "2222" +
                                "2222" +
                                "2222" +
                                "2222" +

                                "3333" +
                                "3333" +
                                "3333" +
                                "3333" +

                                "4444" +
                                "4444" +
                                "4444" +
                                "4444" +

                                "5555" +
                                "5555" +
                                "5555" +
                                "5555")
                );
            }
        } catch (InterruptedException e) {
            show_thread.interrupt();
            System.out.println("Show thread interrupted");
        }
    }

    @Test
    @Order(602)
    @DisplayName("Concurrent 4x4 exception handling - interrupt running threads")
    public void concurrent_4x4_exception_handling_running_threads() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        Show object_show = new Show(cube_4x4);
        Thread show_thread = new Thread(object_show);
        ArrayList<Thread> threads = new ArrayList<>();
        for (int layer = 0; layer < 4; layer++) {
            for (int i = 0; i < 12; i++) {
                threads.add(new Thread(new Rotor(cube_4x4, FRONT, layer)));
            }
        }

        try {
            for (Thread thread : threads) {
                thread.start();
                TimeUnit.MILLISECONDS.sleep(START_DELAY);
            }
            // now we interrupt 1st thread rotating each of the layers - the error message now says we already
            // started, so we won't stop
            for (int i = 0; i < threads.size(); i += 12) {
                threads.get(i).interrupt();
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                if (thread.isInterrupted()) {
                    System.out.println("Rotor " + thread.getName() + " interrupted");
                }
            }

        }
        try {
            String cube_4x4_show;
            show_thread.start();
            show_thread.join();
            cube_4x4_show = object_show.result;
            System.out.println(cube_4x4_show);
            if (ASSERT) {
                assert (cube_4x4_show.equals(
                                "0000" +
                                "0000" +
                                "0000" +
                                "0000" +

                                "1111" +
                                "1111" +
                                "1111" +
                                "1111" +

                                "2222" +
                                "2222" +
                                "2222" +
                                "2222" +

                                "3333" +
                                "3333" +
                                "3333" +
                                "3333" +

                                "4444" +
                                "4444" +
                                "4444" +
                                "4444" +

                                "5555" +
                                "5555" +
                                "5555" +
                                "5555")
                );
            }
        } catch (InterruptedException e) {
            show_thread.interrupt();
            System.out.println("Show thread interrupted");
        }
    }

    @Test
    @Order(700)
    @DisplayName("Concurrent 4x4 multiple show() calls")
    public void concurrent_4x4_multiple_show() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        Cube cube_4x4 = new Cube(4, pre_rotate, post_rotate, beforeShowing, afterShowing);
        ArrayList<Integer> sides = new ArrayList<>(Arrays.asList(RIGHT, TOP, FRONT, BOTTOM, LEFT, BACK));
        ArrayList<Thread> threads = new ArrayList<>();
        for (int side : sides) {
            threads.add(new Thread(new Rotor(cube_4x4, side, 1)));
            threads.add(new Thread(new Rotor(cube_4x4, side, 2)));
        }

        try {
            for (Thread thread : threads) {
                thread.start();
                TimeUnit.MILLISECONDS.sleep(START_DELAY);
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                if (thread.isInterrupted()) {
                    System.out.println("Rotor " + thread.getName() + " interrupted");
                }
            }

        }
        ArrayList<Thread> show_threads = new ArrayList<>();
        ArrayList<Show> show_objects = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Show show = new Show(cube_4x4);
            show_objects.add(show);
            show_threads.add(new Thread(show));
        }
        try {
            for (Thread show_thread : show_threads) {
                show_thread.start();
            }
            for (Thread show_thread : show_threads) {
                show_thread.join();
            }
            for (Show show : show_objects) {
                String cube_4x4_show = show.result;
                System.out.println(cube_4x4_show);
                if (ASSERT) {
                    assert (cube_4x4_show.equals(
                                    "0000" +
                                    "0330" +
                                    "0330" +
                                    "0000" +

                                    "1111" +
                                    "1221" +
                                    "1221" +
                                    "1111" +

                                    "2222" +
                                    "2552" +
                                    "2552" +
                                    "2222" +

                                    "3333" +
                                    "3443" +
                                    "3443" +
                                    "3333" +

                                    "4444" +
                                    "4004" +
                                    "4004" +
                                    "4444" +

                                    "5555" +
                                    "5115" +
                                    "5115" +
                                    "5555")
                    );
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Show thread interrupted");
        }
    }

    // for these tests we will use larger cube as they are equally likely to have subsequences of the same
    // axis rotations, however they are less likely to have the same layers subsequences - we want to test
    // efficiency of the concurrent implementation
    @Test
    @Order(800)
    @DisplayName("Random 10x10 correctness small")
    public void random_10x10_correctness_small() {
        USE_LOW_DELAY = false;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        int ROTATIONS = 25;
        Cube cube_10x10_concurrent = new Cube(10, pre_rotate, post_rotate, beforeShowing, afterShowing);
        Cube cube_10x10_linear = new Cube(10, pre_rotate, post_rotate, beforeShowing, afterShowing);
        ArrayList<Integer> sides = new ArrayList<>();
        ArrayList<Integer> layers = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();
        Random r = new Random();
        for (int i = 0; i < ROTATIONS; i++) {
            sides.add(r.nextInt(6));
            layers.add(r.nextInt(10));
            threads.add(new Thread(new Rotor(cube_10x10_concurrent, sides.get(i), layers.get(i))));
        }

        Instant concurrent_start_time = Clock.systemUTC().instant();
        try {
            for (Thread thread : threads) {
                thread.start();
                UNIT_OF_DELAY.sleep(START_DELAY);
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                if (thread.isInterrupted()) {
                    System.out.println("Rotor " + thread.getName() + " interrupted");
                }
            }
        }
        Instant concurrent_end_time = Clock.systemUTC().instant();

        Instant linear_start_time = Clock.systemUTC().instant();
        for (int i = 0; i < ROTATIONS; i++) {
            try {
                cube_10x10_linear.rotate(sides.get(i), layers.get(i));
            } catch (InterruptedException e) {
                System.out.println("Linear rotations interrupted");
            }
        }
        Instant linear_end_time = Clock.systemUTC().instant();
        if (ASSERT) {
            try {
                String cube_10x10_concurrent_show = cube_10x10_concurrent.show();
                String cube_10x10_linear_show = cube_10x10_linear.show();
                assert(cube_10x10_concurrent_show.equals(cube_10x10_linear_show));
            } catch (InterruptedException e) {
                System.out.println("TEST " + test_num + " threw interrupted exception!");
            }
        }

        Duration concurrent_duration = Duration.between(concurrent_start_time, concurrent_end_time);
        Duration linear_duration = Duration.between(linear_start_time, linear_end_time);
        double magnitude_faster = (double)linear_duration.multipliedBy(100).dividedBy(concurrent_duration) / 100;

        System.out.println("\nconcurrent has taken " +
                concurrent_duration.toString().replace("PT", "") +
                " to complete");
        System.out.println("\nlinear has taken " +
                linear_duration.toString().replace("PT", "") +
                " to complete");
        System.out.println("\nconcurrent has been " + magnitude_faster + "x faster than linear");
    }

    @Test
    @Order(801)
    @DisplayName("Random 10x10 correctness medium")
    public void random_10x10_correctness_medium() {
        USE_LOW_DELAY = true;
        UNIT_OF_DELAY = TimeUnit.MILLISECONDS;
        int ROTATIONS = 500;
        Cube cube_10x10_concurrent = new Cube(10, pre_rotate, post_rotate, beforeShowing, afterShowing);
        Cube cube_10x10_linear = new Cube(10, pre_rotate, post_rotate, beforeShowing, afterShowing);
        ArrayList<Integer> sides = new ArrayList<>();
        ArrayList<Integer> layers = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();
        Random r = new Random();
        for (int i = 0; i < ROTATIONS; i++) {
            sides.add(r.nextInt(6));
            layers.add(r.nextInt(10));
            threads.add(new Thread(new Rotor(cube_10x10_concurrent, sides.get(i), layers.get(i))));
        }

        Instant concurrent_start_time = Clock.systemUTC().instant();
        try {
            for (Thread thread : threads) {
                thread.start();
                UNIT_OF_DELAY.sleep(START_DELAY);
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                if (thread.isInterrupted()) {
                    System.out.println("Rotor " + thread.getName() + " interrupted");
                }
            }
        }
        Instant concurrent_end_time = Clock.systemUTC().instant();

        Instant linear_start_time = Clock.systemUTC().instant();
        for (int i = 0; i < ROTATIONS; i++) {
            try {
                cube_10x10_linear.rotate(sides.get(i), layers.get(i));
            } catch (InterruptedException e) {
                System.out.println("Linear rotations interrupted");
            }
        }
        Instant linear_end_time = Clock.systemUTC().instant();
        if (ASSERT) {
            try {
                String cube_10x10_concurrent_show = cube_10x10_concurrent.show();
                String cube_10x10_linear_show = cube_10x10_linear.show();
                assert(cube_10x10_concurrent_show.equals(cube_10x10_linear_show));
            } catch (InterruptedException e) {
                System.out.println("TEST " + test_num + " threw interrupted exception!");
            }
        }

        Duration concurrent_duration = Duration.between(concurrent_start_time, concurrent_end_time);
        Duration linear_duration = Duration.between(linear_start_time, linear_end_time);
        double magnitude_faster = (double)linear_duration.multipliedBy(100).dividedBy(concurrent_duration) / 100;

        System.out.println("\nconcurrent has taken " +
                concurrent_duration.toString().replace("PT", "") +
                " to complete");
        System.out.println("\nlinear has taken " +
                linear_duration.toString().replace("PT", "") +
                " to complete");
        System.out.println("\nconcurrent has been " + magnitude_faster + "x faster than linear");
    }

    @Test
    @Order(802)
    @DisplayName("Random 10x10 correctness large")
    public void random_10x10_correctness_large() {
        USE_LOW_DELAY = true;
        UNIT_OF_DELAY = TimeUnit.MICROSECONDS;
        int ROTATIONS = 10000;
        Cube cube_10x10_concurrent = new Cube(10, pre_rotate, post_rotate, beforeShowing, afterShowing);
        Cube cube_10x10_linear = new Cube(10, pre_rotate, post_rotate, beforeShowing, afterShowing);
        ArrayList<Integer> sides = new ArrayList<>();
        ArrayList<Integer> layers = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();
        Random r = new Random();
        for (int i = 0; i < ROTATIONS; i++) {
            sides.add(r.nextInt(6));
            layers.add(r.nextInt(10));
            threads.add(new Thread(new Rotor(cube_10x10_concurrent, sides.get(i), layers.get(i))));
        }

        Instant concurrent_start_time = Clock.systemUTC().instant();
        try {
            for (Thread thread : threads) {
                thread.start();
                UNIT_OF_DELAY.sleep(START_DELAY);
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                if (thread.isInterrupted()) {
                    System.out.println("Rotor " + thread.getName() + " interrupted");
                }
            }
        }
        Instant concurrent_end_time = Clock.systemUTC().instant();

        Instant linear_start_time = Clock.systemUTC().instant();
        for (int i = 0; i < ROTATIONS; i++) {
            try {
                cube_10x10_linear.rotate(sides.get(i), layers.get(i));
            } catch (InterruptedException e) {
                System.out.println("Linear rotations interrupted");
            }
        }
        Instant linear_end_time = Clock.systemUTC().instant();
        if (ASSERT) {
            try {
                String cube_10x10_concurrent_show = cube_10x10_concurrent.show();
                String cube_10x10_linear_show = cube_10x10_linear.show();
                assert(cube_10x10_concurrent_show.equals(cube_10x10_linear_show));
            } catch (InterruptedException e) {
                System.out.println("TEST " + test_num + " threw interrupted exception!");
            }
        }

        Duration concurrent_duration = Duration.between(concurrent_start_time, concurrent_end_time);
        Duration linear_duration = Duration.between(linear_start_time, linear_end_time);
        double magnitude_faster = (double)linear_duration.multipliedBy(100).dividedBy(concurrent_duration) / 100;

        System.out.println("\nconcurrent has taken " +
                concurrent_duration.toString().replace("PT", "") +
                " to complete");
        System.out.println("\nlinear has taken " +
                linear_duration.toString().replace("PT", "") +
                " to complete");
        System.out.println("\nconcurrent has been " + magnitude_faster + "x faster than linear");
    }

    @AfterAll
    @DisplayName("Testing procedure finished!")
    static void after_testing() {
        System.out.println("All tests finished!");
    }
}