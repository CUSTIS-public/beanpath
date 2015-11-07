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

import com.google.common.reflect.TypeToken;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Used to capture type parameterization, otherwise lost by type erasure.
 * <p/>
 * Consider next example.
 * <pre>{@code
 *     final List<String> listOfString = root(List.class);
 * }</pre>
 * Here the actual parameter of {@code List} is not known at runtime and The Framework
 * cannot mock it properly (namely it does not know what type to return form get() method).
 * <p/>
 * The proper way to express your intention is to use {@code TypeLiteral},
 * that can capture type parameter and provide it to The Framework when necessary:
 * <pre>{@code
 *     final List<String> listOfString = root(new TypeLiteral<List<String>>() {});
 * }</pre>
 * Notice implicit subclassing of {@code TypeLiteral}, only in that case actual parameter type
 * embeds in type hierarchy and become available at runtime.
 */
public abstract class TypeLiteral<T> {
    private final Type capturedType;

    protected TypeLiteral() {
        final Type genericSuperclass = getClass().getGenericSuperclass();

        if (genericSuperclass instanceof Class) {
            throw new IllegalArgumentException("Missing type argument");
        }

        capturedType = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
    }

    @SuppressWarnings("unchecked")
    /*package-local*/ TypeToken<T> toTypeToken() {
        return (TypeToken<T>) TypeToken.of(capturedType);
    }
}
