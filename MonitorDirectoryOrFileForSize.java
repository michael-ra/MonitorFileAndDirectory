import sun.misc.Signal;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class MonitorDirectoryOrFileForSize {

    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RESET = "\u001B[0m";

    /**
     * Driver program. Starts monitoring thread for size of dir or file.
     * @param args Argument 1 (position 0) should be full file path or directory path to monitor.
     */
    public static void main(String[] args) {
        if(args.length != 1 || !args[0].matches("((?:[^\\/]*\\/)*)(.*)")) {
            System.out.println("Cannot have less or more than 1 arg. First arg should be file path to be monitored.");
            System.out.println("Usage: First argument should be <Path>. Without quotes. Without spaces. Can be file (.*) or directory (no ending).");
            System.exit(64);
            // #define EX_USAGE	64	/* command line usage error */
            // https://opensource.apple.com/source/Libc/Libc-320/include/sysexits.h
        }
        String path = args[0];

        Signal sg = new Signal("TERM");
        Signal.handle(sg, signal -> {
            System.out.println("Watching dir/file stopped! Program terminates with signal: " + signal.getName());
            System.exit(0);
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("Main running Watcher stopped! Shutting down...")));

        TimerTask task = new ScheduleTask(path);
        Timer tmr = new Timer();

        System.out.println("Watch task running. Listening for changes for '" + path + "'");
        tmr.scheduleAtFixedRate(task, new Date(), 1000);
        Runtime.getRuntime().addShutdownHook(new Thread(ScheduleTask::close));
    }

    /**
     * Sheduled task that searched directory for file sizes and prints if file size differs.
     * If path is of file not directory, instead monitors file size and prints if file size differs.
     */
    static class ScheduleTask extends TimerTask {

        Path path;
        final AtomicLong sizeBefore = new AtomicLong(0);
        final AtomicLong size = new AtomicLong(0);

        /**
         * @param pathString Path to monitor
         */
        public ScheduleTask(String pathString) {
            this.path = Paths.get(pathString);
        }

        /**
         * Prints exiting notice
         */
        public static void close() {
            System.out.println("Watcher of dir/file stopped! Shutting down...");
        }

        /**
         * Monitors directory or file and outputs size of path if size differs from before. Runs every 1 second.
         */
        public void run() {
            if(!Files.exists(path)) {
                System.out.println("Skipping file or directory: '"+path+"' ! "+ANSI_YELLOW + "There is no such file or directory. I cannot search the void, it is too dangerous!" + ANSI_RESET);
                try {
                    throw new IOException("Cannot monitor path/file that doesnt exist!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(Files.isDirectory(path)) {
                try {
                    size.set(0);
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            size.addAndGet(attrs.size());
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            System.out.println("Skipping dir: " + file + " (" + exc + ")");
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                            if (exc != null)
                                System.out.println("Directory is not passable: " + dir + " (" + exc + ")");
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    throw new AssertionError("FileWalker threw exception. This should not happen.");
                }
            } else {
                try {
                    size.set(0);
                    size.addAndGet(Files.size(path));
                } catch (IOException e) {
                    throw new AssertionError("Files.size(path) threw exception. This should not happen. Does the file at your path exist and is accessible?");
                }
            }
            if(sizeBefore.get() != size.get()) {
                System.out.println("File or directory size is now: " + size.get() + " Bytes.");
                sizeBefore.set(size.get());
            }
        }
    }
}
