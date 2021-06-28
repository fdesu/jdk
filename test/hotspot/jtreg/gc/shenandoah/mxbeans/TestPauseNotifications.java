/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

/*
 * @test TestPauseNotifications
 * @summary Check that MX notifications are reported for all cycles
 * @library /test/lib /
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -Xmx128m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC
 *      TestPauseNotifications
 *
 * @run main/othervm -Xmx128m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC
 *      TestPauseNotifications
 */

/*
 * @test TestPauseNotifications
 * @summary Check that MX notifications are reported for all cycles
 * @library /test/lib /
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -Xmx128m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      TestPauseNotifications
 */

/*
 * @test TestPauseNotifications
 * @summary Check that MX notifications are reported for all cycles
 * @library /test/lib /
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -Xmx128m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      TestPauseNotifications
 */

/*
 * @test TestPauseNotifications
 * @summary Check that MX notifications are reported for all cycles
 * @library /test/lib /
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -Xmx128m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=static
 *      TestPauseNotifications
 */

/*
 * @test TestPauseNotifications
 * @summary Check that MX notifications are reported for all cycles
 * @library /test/lib /
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -Xmx128m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact
 *      TestPauseNotifications
 */

/*
 * @test TestPauseNotifications
 * @summary Check that MX notifications are reported for all cycles
 * @library /test/lib /
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -Xmx128m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      TestPauseNotifications
 *
 * @run main/othervm -Xmx128m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      TestPauseNotifications
 */

import java.util.*;
import java.util.concurrent.atomic.*;
import javax.management.*;
import java.lang.management.*;
import javax.management.openmbean.*;

import jdk.test.lib.Utils;

import com.sun.management.GarbageCollectionNotificationInfo;

public class TestPauseNotifications {

    static final long HEAP_MB = 128;                           // adjust for test configuration above
    static final long TARGET_MB = Long.getLong("target", 2_000); // 2 Gb allocation

    static volatile Object sink;

    public static void main(String[] args) throws Exception {
        final long startTime = System.currentTimeMillis();

        final AtomicLong pausesDuration = new AtomicLong();
        final AtomicLong cyclesDuration = new AtomicLong();

        NotificationListener listener = new NotificationListener() {
            @Override
            public void handleNotification(Notification n, Object o) {
                if (n.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                    GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData) n.getUserData());

                    System.out.println("Received: " + info.getGcName());

                    long d = info.getGcInfo().getDuration();

                    String name = info.getGcName();
                    if (name.contains("Shenandoah")) {
                        if (name.equals("Shenandoah Pauses")) {
                            pausesDuration.addAndGet(d);
                        } else if (name.equals("Shenandoah Cycles")) {
                            cyclesDuration.addAndGet(d);
                        } else {
                            throw new IllegalStateException("Unknown name: " + name);
                        }
                    }
                }
            }
        };

        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            ((NotificationEmitter) bean).addNotificationListener(listener, null, null);
        }

        final int size = 100_000;
        long count = TARGET_MB * 1024 * 1024 / (16 + 4 * size);

        for (int c = 0; c < count; c++) {
            sink = new int[size];
        }

        // Look at test timeout to figure out how long we can wait without breaking into timeout.
        long spentTime = System.currentTimeMillis() - startTime;
        long maxTries = (Utils.adjustTimeout(Utils.DEFAULT_TEST_TIMEOUT) - spentTime) / 1000 / 2;

        long actualPauses = 0;
        long actualCycles = 0;

        // Wait until enough notifications are accrued to match minimum boundary.
        long minExpected = 1;

        long tries = 0;
        while (tries++ < maxTries) {
            actualPauses = pausesDuration.get();
            actualCycles = cyclesDuration.get();
            if (minExpected <= actualPauses && minExpected <= actualCycles) {
                // Wait a little bit to catch the lingering notifications.
                Thread.sleep(5000);
                actualPauses = pausesDuration.get();
                actualCycles = cyclesDuration.get();
                break;
            }
            Thread.sleep(1000);
        }

        String msg = "Pauses expected = [" + minExpected + "; +inf], actual = " + actualPauses + "\n" +
                     "Cycles expected = [" + minExpected + "; +inf], actual = " + actualCycles + "\n" +
                     "Pauses duration (" + actualPauses + ") is expected to be smaller than cycles duration (" + actualCycles + ")";

        if (minExpected <= actualPauses && minExpected <= actualCycles && actualPauses <= actualCycles) {
            System.out.println(msg);
        } else {
            throw new IllegalStateException(msg);
        }
    }
}
