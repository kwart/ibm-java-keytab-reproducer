package cz.cacek.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KeyTab;

import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Reproducer for Keytab implementation issue on IBM Java 8. 
 */
public class KeytabTest {

    @ClassRule
    public static volatile TemporaryFolder tempDir = new TemporaryFolder();

    static final String PRINCIPAL_NAME = "hz/127.0.0.1@HAZELCAST.COM";
    static final int EXPECTED_KEY_COUNT = 2;

    static final String JAVA_VERSION = System.getProperty("java.version");
    static final int JAVA_VERSION_MAJOR;

    static {
        String[] versionParts = JAVA_VERSION.split("\\.");
        String majorStr = versionParts[0];
        if ("1".equals(majorStr)) {
            majorStr = versionParts[1];
        }
        // early access builds could contain "-ea" suffix, we have to remove it
        majorStr = majorStr.split("-")[0];
        JAVA_VERSION_MAJOR = Integer.parseInt(majorStr);
    }

    volatile Throwable failed;

    @BeforeClass
    public static void beforeClass() {
        assertTrue("This test is intended for IBM Java 8", isIbmJvm() && JAVA_VERSION_MAJOR == 8);
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
        File keytabFile = tempDir.newFile("test-" + threadCount + ".keytab");
        int generatedCount = createKeytab(PRINCIPAL_NAME, "secret", keytabFile);
        System.out.println("Threads: " + threadCount);
        System.out.println("Generated key count in keytab: " + generatedCount);
        System.out.println("Expected count (loaded): " + EXPECTED_KEY_COUNT);
        assertTrue(EXPECTED_KEY_COUNT <= generatedCount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> readKeytab(keytabFile));
        }
        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);
        if (failed != null) {
            throw failed;
        }
    }

    /**
     * Try to read the given keytab file.
     */
    private void readKeytab(File keytabFile) {
        try {
            KerberosPrincipal principal = new KerberosPrincipal(PRINCIPAL_NAME);
            KerberosKey[] keys = KeyTab.getInstance(keytabFile).getKeys(principal);
            assertEquals(EXPECTED_KEY_COUNT, keys.length);
        } catch (AssertionError e) {
            failed = e;
        }
    }

    /**
     * Creates a keytab file for given principal.
     */
    public static int createKeytab(final String principalName, final String passPhrase, final File keytabFile)
            throws IOException {
        final KerberosTime timeStamp = new KerberosTime();
        int count = 0;

        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(keytabFile))) {
            dos.write(Keytab.VERSION_0X502_BYTES);

            for (Map.Entry<EncryptionType, EncryptionKey> keyEntry : KerberosKeyFactory
                    .getKerberosKeys(principalName, passPhrase).entrySet()) {
                final EncryptionKey key = keyEntry.getValue();
                final byte keyVersion = (byte) key.getKeyVersion();
                // entries.add(new KeytabEntry(principalName, principalType, timeStamp, keyVersion, key));

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // handle principal name
                String[] spnSplit = principalName.split("@");
                String nameComponent = spnSplit[0];
                String realm = spnSplit[1];

                String[] nameComponents = nameComponent.split("/");
                try (DataOutputStream entryDos = new DataOutputStream(baos)) {
                    // increment for v1
                    entryDos.writeShort((short) nameComponents.length);
                    entryDos.writeUTF(realm);
                    // write components
                    for (String component : nameComponents) {
                        entryDos.writeUTF(component);
                    }

                    entryDos.writeInt(1); // principal type: KRB5_NT_PRINCIPAL
                    entryDos.writeInt((int) (timeStamp.getTime() / 1000));
                    entryDos.write(keyVersion);

                    entryDos.writeShort((short) key.getKeyType().getValue());

                    byte[] data = key.getKeyValue();
                    entryDos.writeShort((short) data.length);
                    entryDos.write(data);
                }
                final byte[] entryBytes = baos.toByteArray();
                dos.writeInt(entryBytes.length);
                dos.write(entryBytes);
                count++;
            }
        }
        return count;
    }

    public static boolean isIbmJvm() {
        String vendor = System.getProperty("java.vendor");
        return vendor.startsWith("IBM");
    }

}
