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

import java.util.Formatter;

public class BeanPathMagicException extends RuntimeException {

    /*package-local*/ BeanPathMagicException(String message) {
        super(message);
    }

    /*package-local*/ BeanPathMagicException(String message, Object... args) {
        super(format(message, args), extractIfAny(args, Throwable.class));
    }

    private static String format(String format, Object... args) {
        if (format == null) {
            return "";
        }
        if (args == null || args.length == 0) {
            return format;
        } else {
            return new Formatter().format(format, args).toString();
        }
    }

    private static <T> T extractIfAny(Object[] args, Class<T> type) {
        if (type != null && args != null) {
            for (Object arg : args) {
                if (type.isInstance(arg)) {
                    return type.cast(arg);
                }
            }
        }
        return null;
    }
}
