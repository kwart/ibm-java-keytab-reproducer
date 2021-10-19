# IBM Java KeyTab implementation is not thread-safe

[Reproducer](src/test/java/cz/cacek/test/KeyTabTest.java) for problematic `javax.security.auth.kerberos.KeyTab` behavior on IBM Java 8.

We hit it by a Kerberos authentication test in Hazelcast Enterprise ([#3704](https://github.com/hazelcast/hazelcast-enterprise/issues/3704)).

## Environment

Reproduced on Linux x86_64 with the following Java 8 version (up-to-date as of 2021-10-19):

```
java version "1.8.0_301"
Java(TM) SE Runtime Environment (build 8.0.6.36 - pxa6480sr6fp36-20210824_02(SR6 FP36))
IBM J9 VM (build 2.9, JRE 1.8.0 Linux amd64-64-Bit Compressed References 20210824_12036 (JIT enabled, AOT enabled)
OpenJ9   - 7bb9176
OMR      - 820a5aa
IBM      - dc8f23e)
JCL - 20210806_01 based on Oracle jdk8u301-b09
```

We saw the issue in the older IBM Java 8 updates too.

## Show me the code

The piece of code which fails is the following:

```java
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KeyTab;

KerberosPrincipal principal = new KerberosPrincipal(PRINCIPAL_NAME);
KerberosKey[] keys = KeyTab.getInstance(keytabFile).getKeys(principal);
```

When it's executed from several threads it often fails to load proper count of keys.
When debug messages are enabled for the JGSS, it usually reports the **"Keytab is corrupted"**.

### Hazelcast logs
```
[KRB_DBG_KTAB] KeyTab:hz._hzInstance_8_testAuthenticationWithCanonicalHost[simplifiedConfig:false].priority-generic-operation.thread-0:   >>> KeyTab: exception Keytab is corrupted
[KRB_DBG_KTAB] KeyTab:hz._hzInstance_11_testAuthenticationWithDefaultName[simplifiedConfig:false].priority-generic-operation.thread-0:   >>> KeyTab: exception Keytab is corrupted
```

### Reproducer logs

```
KRB_DBG_KTAB] KeyTab:main:   >>> KeyTab: trying to load keytab file /tmp/junit3636416910052042896/expected.keytab
[KRB_DBG_KTAB] KeyTab:mainLoading the keytab file ...   >>> KeyTab: load() entry length: 53
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:mainLoading the keytab file ...   >>> KeyTab: load() entry length: 61
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:mainLoading the keytab file ...   >>> KeyTab: load() entry length: 61
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:mainLoading the keytab file ...   >>> KeyTab: load() entry length: 77
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:mainLoading the keytab file ...   >>> KeyTab: load() entry length: 69
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:main:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_CFG] Config:main:   readKrb5Properties: defaultRealm = null
[KRB_DBG_CFG] Config:main:   readKrb5Properties: defaultKDC   = null
[KRB_DBG_CFG] Config:main:   Java config file '/home/kwart/java/ibm-java-x86_64-80/jre/lib/security/krb5.conf' does not exist.
[KRB_DBG_CFG] Config:main:   Java config file: null
[KRB_DBG_CFG] Config:main:   Native config name: /etc/krb5.conf
[KRB_DBG_CFG] Config:main:   Failed to load/parse config file: java.io.FileNotFoundException: /etc/krb5.conf (No such file or directory)
[KRB_DBG_CFG] Config:main:   Using builtin default etypes for default_tkt_enctypes
[KRB_DBG_CFG] Config:main:   Default etypes for default_tkt_enctypes: 18 17 16 23.
[KRB_DBG_KDC] EncryptionKey:main:   >>> EncryptionKey: config default key type is aes256-cts-hmac-sha1-96
[KRB_DBG_KTAB] KeyTab:main:   Added key: 16  version: 0
[KRB_DBG_KTAB] KeyTab:main:   Added key: 18  version: 0
[KRB_DBG_KTAB] KeyTab:main:   Added key: 17  version: 0
[KRB_DBG_KTAB] KeyTab:main:   Added key: 23  version: 0
[KRB_DBG_KTAB] KeyTab:main:   Found unsupported keytype (3) for hz/127.0.0.1@HAZELCAST.COM
[KRB_DBG_KTAB] KeyTab:main:   Ordering keys wrt default_tkt_enctypes list
[KRB_DBG_CFG] Config:main:   Using builtin default etypes for default_tkt_enctypes
[KRB_DBG_CFG] Config:main:   Default etypes for default_tkt_enctypes: 18 17 16 23.
Threads: 1
Expected count (loaded): 4
[KRB_DBG_KTAB] KeyTab:pool-1-thread-1:   >>> KeyTab: trying to load keytab file /tmp/junit3636416910052042896/test-1.keytab
[KRB_DBG_KTAB] KeyTab:pool-1-thread-1Loading the keytab file ...   >>> KeyTab: load() entry length: 53
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:pool-1-thread-1Loading the keytab file ...   >>> KeyTab: load() entry length: 61
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:pool-1-thread-1Loading the keytab file ...   >>> KeyTab: load() entry length: 61
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:pool-1-thread-1Loading the keytab file ...   >>> KeyTab: load() entry length: 77
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:pool-1-thread-1Loading the keytab file ...   >>> KeyTab: load() entry length: 69
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-1-thread-1:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:pool-1-thread-1:   Added key: 16  version: 0
[KRB_DBG_KTAB] KeyTab:pool-1-thread-1:   Added key: 18  version: 0
[KRB_DBG_KTAB] KeyTab:pool-1-thread-1:   Added key: 17  version: 0
[KRB_DBG_KTAB] KeyTab:pool-1-thread-1:   Added key: 23  version: 0
[KRB_DBG_KTAB] KeyTab:pool-1-thread-1:   Found unsupported keytype (3) for hz/127.0.0.1@HAZELCAST.COM
[KRB_DBG_KTAB] KeyTab:pool-1-thread-1:   Ordering keys wrt default_tkt_enctypes list
[KRB_DBG_CFG] Config:main:  pool-1-thread-1 Using builtin default etypes for default_tkt_enctypes
[KRB_DBG_CFG] Config:main:  pool-1-thread-1 Default etypes for default_tkt_enctypes: 18 17 16 23.
Threads: 5
Expected count (loaded): 4
[KRB_DBG_KTAB] KeyTab:pool-2-thread-2:   >>> KeyTab: trying to load keytab file /tmp/junit3636416910052042896/test-5.keytab
[KRB_DBG_KTAB] KeyTab:pool-2-thread-2Loading the keytab file ...   >>> KeyTab: load() entry length: 53
[KRB_DBG_KTAB] KeyTab:pool-2-thread-4:   >>> KeyTab: trying to load keytab file /tmp/junit3636416910052042896/test-5.keytab
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTab:pool-2-thread-4Loading the keytab file ...   >>> KeyTab: load() entry length: 53
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-4:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-4:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-4:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:pool-2-thread-2:   >>> KeyTab: exception Keytab is corrupted
[KRB_DBG_KTAB] KeyTab:pool-2-thread-3:   >>> KeyTab: trying to load keytab file /tmp/junit3636416910052042896/test-5.keytab
[KRB_DBG_KTAB] KeyTab:pool-2-thread-4:   >>> KeyTab: exception Keytab is corrupted
[KRB_DBG_KTAB] KeyTab:pool-2-thread-3Loading the keytab file ...   >>> KeyTab: load() entry length: 53
[KRB_DBG_KTAB] KeyTab:pool-2-thread-5:   >>> KeyTab: trying to load keytab file /tmp/junit3636416910052042896/test-5.keytab
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-3:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTab:pool-2-thread-5Loading the keytab file ...   >>> KeyTab: load() entry length: 53
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-3:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-3:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-5:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTab:pool-2-thread-3Loading the keytab file ...   >>> KeyTab: load() entry length: 131085
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-5:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-5:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:pool-2-thread-5:   Found unsupported keytype (3) for hz/127.0.0.1@HAZELCAST.COM
[KRB_DBG_KTAB] KeyTab:pool-2-thread-1:   Found unsupported keytype (3) for hz/127.0.0.1@HAZELCAST.COM
HAZELCAST.COMhzK127.0.0.1anb ���ex��v�m62-thread-3:   >>> KeyTabInputStream, readName(): LCAST.COMhz    127.0.0.1anb���`l�)gzD��5?�=
HAZELCAST.COMhz 127.0.0.1anb �sQ��/�����C|�����4v�v�R�a v�E
[KRB_DBG_KTAB] KeyTab:pool-2-thread-3:   >>> KeyTab: exception Illegal character in realm name; one of: '/', ':', '\0'
[KRB_DBG_KTAB] KeyTab:pool-2-thread-3:   Found unsupported keytype (3) for hz/127.0.0.1@HAZELCAST.COM
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.464 s <<< FAILURE! - in cz.cacek.test.KeyTabTest
[ERROR] when_multipleThreads(cz.cacek.test.KeyTabTest)  Time elapsed: 0.097 s  <<< FAILURE!
java.lang.AssertionError: expected:<4> but was:<0>
    at cz.cacek.test.KeyTabTest.assertKeyCount(KeyTabTest.java:84)
    at cz.cacek.test.KeyTabTest.lambda$testReadingKeytabByGivenThreadCount$0(KeyTabTest.java:73)
```

Sometimes threads cycle with the following messages:

```
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 
...
```
