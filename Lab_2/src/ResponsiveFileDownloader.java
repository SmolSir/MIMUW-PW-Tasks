package przyklady02;

import java.io.IOException;
import java.time.Clock;

public class ResponsiveFileDownloader {

    private static volatile int progress = 0;
    private static final int PROGRESS_MAX = 100;

    private static class Downloader implements Runnable {

        @Override
        public void run() {
            try {
                while (progress < PROGRESS_MAX) {
                    Thread.sleep(50);
                    progress++;
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
            }
            catch (InterruptedException e) {
                progress = 0;
                System.out.println("Download interrupted");
            }
        }
    }

    public static void main(String[] args) {
        boolean working = false;
        Thread t = new Thread(new Downloader());
        while (true) {
            // This clears the console
            System.out.print("\033[H\033[2J");
            System.out.flush();

            if (progress == 0) {
                System.out.println("Press enter to start downloading");
            } else if (progress == 100) {
                System.out.println("Download complete");
                working = false;
                progress = 0;
            }
            System.out.println("Time: " + Clock.systemDefaultZone().instant().toString());
            System.out.println("Progress: " + progress + " / " + PROGRESS_MAX);
            try {
                // Check if user pressed enter
                if (System.in.available() > 0 && System.in.read() == '\n') {
                    if (!working) {
                        t = new Thread(new Downloader());
                        working = true;
                        t.start();
                    }
                    else {
                        working = false;
                        t.interrupt();
                    }
                } else {
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException e) {
                return;
            }

        }
    }
}


