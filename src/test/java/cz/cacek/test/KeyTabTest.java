package cz.cacek.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KeyTab;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Reproducer for {@link KeyTab} implementation issue on IBM Java 8.
 */
public class KeyTabTest {

    @ClassRule
    public static volatile TemporaryFolder tempDir = new TemporaryFolder();

    static final String PRINCIPAL_NAME = "hz/127.0.0.1@HAZELCAST.COM";
    static volatile int EXPECTED_KEY_COUNT;

    volatile Throwable failed;

    @BeforeClass
    public static void beforeClass() throws IOException {
        File file = copyKeytab("expected.keytab");
        EXPECTED_KEY_COUNT = getKerberosKeyCount(file);
    }

    /**
     * Load keytab file from a single thread. (This one passes.)
     */
    @Test
    public void when_singleThread() throws Throwable {
        testReadingKeytabByGivenThreadCount(1);
    }

    /**
     * Load keytab file from a single thread. (This one fails.)
     */
    @Test
    public void when_multipleThreads() throws Throwable {
        testReadingKeytabByGivenThreadCount(5);
        // just in case we don't hit the thread-safety issue with the first run, let's repeat with another thread counts
        testReadingKeytabByGivenThreadCount(10);
        testReadingKeytabByGivenThreadCount(20);
    }

    /**
     * Given: keytab is created with name "test-[threadCout].keytab"<br>
     * When: [threadCount] number of threads tries to access given keytab<br>
     * Then: all the threads see the proper count of keys in the keytab.<br>
     */
    private void testReadingKeytabByGivenThreadCount(int threadCount) throws Throwable {
        failed = null;
        File keytabFile = copyKeytab("test-" + threadCount + ".keytab");
        System.out.println("Threads: " + threadCount);
        System.out.println("Expected count (loaded): " + EXPECTED_KEY_COUNT);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> assertKeyCount(keytabFile));
        }
        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);
        if (failed != null) {
            throw failed;
        }
    }

    private void assertKeyCount(File keytabFile) {
        try {
            assertEquals(EXPECTED_KEY_COUNT, getKerberosKeyCount(keytabFile));
        } catch (AssertionError e) {
            failed = e;
        }
    }

    /**
     * Try to read the given keytab file.
     */
    private static int getKerberosKeyCount(File keytabFile) {
        KerberosPrincipal principal = new KerberosPrincipal(PRINCIPAL_NAME);
        KerberosKey[] keys = KeyTab.getInstance(keytabFile).getKeys(principal);
        return keys.length;
    }

    private static File copyKeytab(String fileName) throws IOException {
        File file = tempDir.newFile(fileName);
        try (FileOutputStream fos = new FileOutputStream(file);
                InputStream is = KeyTabTest.class.getResourceAsStream("/test.keytab")) {
            byte[] buffer = new byte[4096];
            int n;
            while (-1 != (n = is.read(buffer))) {
                fos.write(buffer, 0, n);
            }
        }
        return file;
    }

}
