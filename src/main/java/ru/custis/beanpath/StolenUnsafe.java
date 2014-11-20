/**
 * Copyright (C) 2014 CUSTIS (http://www.custis.ru/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.custis.beanpath;

import sun.misc.Unsafe;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;

/*package-local*/ class StolenUnsafe {
    private StolenUnsafe() {} // static use only

    /*
     * sun.misc.Unsafe.getUnsafe() has runtime check to be accessed
     * only from system classloader, but we can steal it via reflection
     */

    private static Unsafe unsafe;

    public static @Nonnull Unsafe getUnsafe() {
        if (unsafe == null) {
            unsafe = steal();
        }
        return unsafe;
    }

    private static Unsafe steal() {
        try {
            final Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            try {
                return (Unsafe) theUnsafeField.get(null);
            } finally {
                theUnsafeField.setAccessible(false);
            }
        } catch (Exception x) {
            throw new RuntimeException("Failed to steal sun.misc.Unsafe via reflection", x);
        }
    }
}
