/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.threaddump;

final class ThreadDumpTestData {

    private ThreadDumpTestData() {
    }

    static final String SAMPLE_DUMP_1 = """
        2026-02-18 10:00:00
        Full thread dump OpenJDK 64-Bit Server VM (25+):

        "main" #1 prio=5 os_prio=0 cpu=12.00ms elapsed=1.20s tid=0x000000000001 nid=0x1 runnable [0x000000000001]
           java.lang.Thread.State: RUNNABLE
                at dev.demo.Main.loop(Main.java:42)
                - locked <0x0000000011111111> (a java.lang.Object)
                at dev.demo.Main.main(Main.java:12)

        "worker-1" #22 daemon prio=5 os_prio=0 cpu=80.00ms elapsed=1.10s tid=0x000000000022 nid=0x16 waiting on condition [0x000000000002]
           java.lang.Thread.State: WAITING (parking)
                at jdk.internal.misc.Unsafe.park(Native Method)
                - parking to wait for <0x0000000012121212> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
                at java.util.concurrent.locks.LockSupport.park(LockSupport.java:211)

        "worker-2" #23 daemon prio=5 os_prio=0 cpu=20.00ms elapsed=1.00s tid=0x000000000023 nid=0x17 blocked on monitor enter [0x000000000003]
           java.lang.Thread.State: BLOCKED (on object monitor)
                at dev.demo.Queue.take(Queue.java:88)
                - waiting to lock <0x0000000013131313> (a java.lang.Object)
                at dev.demo.Service.run(Service.java:33)

        JNI global refs: 10, weak refs: 1
        """;

    static final String SAMPLE_DUMP_2 = """
        2026-02-18 10:00:05
        Full thread dump OpenJDK 64-Bit Server VM (25+):

        "main" #1 prio=5 os_prio=0 cpu=18.00ms elapsed=6.00s tid=0x000000000001 nid=0x1 waiting on condition [0x000000000001]
           java.lang.Thread.State: WAITING (parking)
                at java.lang.Object.wait(Native Method)
                - waiting on <0x0000000011111111> (a java.lang.Object)
                at dev.demo.Main.main(Main.java:18)

        "worker-2" #23 daemon prio=5 os_prio=0 cpu=55.00ms elapsed=5.50s tid=0x000000000023 nid=0x17 runnable [0x000000000003]
           java.lang.Thread.State: RUNNABLE
                at dev.demo.Queue.poll(Queue.java:93)
                at dev.demo.Service.run(Service.java:35)

        "http-acceptor" #40 daemon prio=5 os_prio=0 cpu=4.00ms elapsed=5.20s tid=0x000000000040 nid=0x28 runnable [0x000000000004]
           java.lang.Thread.State: RUNNABLE
                at sun.nio.ch.ServerSocketChannelImpl.accept0(Native Method)
                at sun.nio.ch.ServerSocketChannelImpl.accept(ServerSocketChannelImpl.java:521)

        JNI global refs: 11, weak refs: 1
        """;
}
