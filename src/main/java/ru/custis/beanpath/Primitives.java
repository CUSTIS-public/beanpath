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

import javax.annotation.Nullable;

final class Primitives {
    private Primitives() {}

    private static final Prm[] PRIMITIVES = Prm.values();

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getWrapperClass(Class<T> type) {
        return (Class<T>) findPrm(type).getWrapperClass();
    }

    @SuppressWarnings("unchecked")
    public static @Nullable <T> T getDefaultValue(Class<T> type) {
        return (T) findPrm(type).getDefaultValue();
    }

    private static Prm findPrm(Class<?> type) {
        for (Prm p : PRIMITIVES) {
            if (p.getType() == type) {
                return p;
            }
        }
        throw new IllegalArgumentException(
                (type != null) ? "Not a primitive: " + type
                               : "Parameter 'type' must not be null"
        );
    }

    @SuppressWarnings("unused")
    private enum Prm {
        BOOLEAN(boolean.class, Boolean.class, false),
        CHAR(char.class, Character.class, '\u0000'),
        BYTE(byte.class, Byte.class, (byte) 0),
        SHORT(short.class, Short.class, (short) 0),
        INT(int.class, Integer.class, 0),
        LONG(long.class, Long.class, 0L),
        FLOAT(float.class, Float.class, 0f),
        DOUBLE(double.class, Double.class, 0D),
        VOID(void.class, Void.class, null);

        private final Class<?> type;
        private final Class<?> wrapperClass;
        private final Object defaultValue;

        Prm(Class<?> type, Class<?> wrapperClass, Object defaultValue) {
            this.type = type;
            this.wrapperClass = wrapperClass;
            this.defaultValue = defaultValue;
        }

        Class<?> getType() {
            return type;
        }

        Class<?> getWrapperClass() {
            return wrapperClass;
        }

        Object getDefaultValue() {
            return defaultValue;
        }
    }
}
