# IBM Java KeyTab implementation is not thread-safe

[Reproducer](src/test/java/cz/cacek/test/KeytabTest.java) for problematic `javax.security.auth.kerberos.KeyTab` behavior on IBM Java 8.

We hit it by a Kerberos authentication test in Hazelcast Enterprise ([#3704](https://github.com/hazelcast/hazelcast-enterprise/issues/3704)).

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
When debug messages are enabled for the JGSS, it reports the "Keytab is corrupted".

### Hazelcast logs
```
[KRB_DBG_KTAB] KeyTab:hz._hzInstance_8_testAuthenticationWithCanonicalHost[simplifiedConfig:false].priority-generic-operation.thread-0:   >>> KeyTab: exception Keytab is corrupted
[KRB_DBG_KTAB] KeyTab:hz._hzInstance_11_testAuthenticationWithDefaultName[simplifiedConfig:false].priority-generic-operation.thread-0:   >>> KeyTab: exception Keytab is corrupted
```

### Reproducer logs

```
Threads: 5
Generated key count in keytab: 3
Expected count (loaded): 2
[KRB_DBG_KTAB] KeyTab:pool-2-thread-1:   >>> KeyTab: trying to load keytab file /tmp/junit1272728960580168905/test-5.keytab
[KRB_DBG_KTAB] KeyTab:pool-2-thread-1Loading the keytab file ...   >>> KeyTab: load() entry length: 61
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-1:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTab:pool-2-thread-4:   >>> KeyTab: trying to load keytab file /tmp/junit1272728960580168905/test-5.keytab
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-1:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-1:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:pool-2-thread-5:   >>> KeyTab: trying to load keytab file /tmp/junit1272728960580168905/test-5.keytab
[KRB_DBG_KTAB] KeyTab:pool-2-thread-3:   >>> KeyTab: trying to load keytab file /tmp/junit1272728960580168905/test-5.keytab
[KRB_DBG_KTAB] KeyTab:pool-2-thread-1Loading the keytab file ...   >>> KeyTab: load() entry length: 77
[KRB_DBG_KTAB] KeyTab:pool-2-thread-4Loading the keytab file ...   >>> KeyTab: load() entry length: 61
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-1:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-4:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTab:pool-2-thread-5Loading the keytab file ...   >>> KeyTab: load() entry length: 61
[KRB_DBG_KTAB] KeyTab:pool-2-thread-3Loading the keytab file ...   >>> KeyTab: load() entry length: 61
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-1:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-4:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-5:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-3:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-4:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-1:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-5:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTab:pool-2-thread-1:   >>> KeyTab: exception Keytab is corrupted
[KRB_DBG_KTAB] KeyTab:pool-2-thread-4:   >>> KeyTab: exception Keytab is corrupted
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-3:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-3:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-5:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:pool-2-thread-3:   >>> KeyTab: exception Keytab is corrupted
[KRB_DBG_KTAB] KeyTab:pool-2-thread-5:   >>> KeyTab: exception Keytab is corrupted
[KRB_DBG_KTAB] KeyTab:pool-2-thread-2:   >>> KeyTab: trying to load keytab file /tmp/junit1272728960580168905/test-5.keytab
[KRB_DBG_KTAB] KeyTab:pool-2-thread-2Loading the keytab file ...   >>> KeyTab: load() entry length: 61
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:pool-2-thread-2Loading the keytab file ...   >>> KeyTab: load() entry length: 77
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:pool-2-thread-2Loading the keytab file ...   >>> KeyTab: load() entry length: 53
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): HAZELCAST.COM
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): hz
[KRB_DBG_KTAB] KeyTableInputStream:pool-2-thread-2:   >>> KeyTabInputStream, readName(): 127.0.0.1
[KRB_DBG_KTAB] KeyTab:pool-2-thread-2:   Found unsupported keytype (3) for hz/127.0.0.1@HAZELCAST.COM
[KRB_DBG_KTAB] KeyTab:pool-2-thread-2:   Added key: 18  version: 0
[KRB_DBG_KTAB] KeyTab:pool-2-thread-2:   Added key: 17  version: 0
[KRB_DBG_KTAB] KeyTab:pool-2-thread-2:   Ordering keys wrt default_tkt_enctypes list
[KRB_DBG_CFG] Config:main:  pool-2-thread-2 Using builtin default etypes for default_tkt_enctypes
[KRB_DBG_CFG] Config:main:  pool-2-thread-2 Default etypes for default_tkt_enctypes: 18 17 16 23.
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.874 s <<< FAILURE! - in cz.cacek.test.KeytabTest
[ERROR] when_multipleThreads(cz.cacek.test.KeytabTest)  Time elapsed: 0.056 s  <<< FAILURE!
java.lang.AssertionError: expected:<2> but was:<0>
    at cz.cacek.test.KeytabTest.readKeytab(KeytabTest.java:114)
    at cz.cacek.test.KeytabTest.lambda$testReadingKeytabByGivenThreadCount$0(KeytabTest.java:98)
```

Some threads sometimes cycle with the following messages:

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