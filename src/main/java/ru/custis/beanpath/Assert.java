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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/*package-local*/ class Assert {
    private Assert() {} // static use only

    public static @Nonnull <T> T notNull(@Nullable T argument, String claimOrArgName) {
        if (argument == null) {
            if (claimOrArgName == null) {
                claimOrArgName = "Argument must not be null";
            } else if (!claimOrArgName.contains(" ")) {
                claimOrArgName = "Argument '" + claimOrArgName + "' must not be null";
            }
            throw new IllegalArgumentException(claimOrArgName);
        } else {
            return argument;
        }
    }
}
