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
import net.sf.cglib.proxy.InvocationHandler;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Character.isUpperCase;

/**
 * This is where all the magic resides :-)
 */
public class BeanPathMagic {
    private BeanPathMagic() {} // static use only

    @SuppressWarnings("unchecked")
    public static @Nonnull <T> T root(@Nonnull Class<T> clazz) {
        Assert.notNull(clazz, "clazz");
        return (T) Mocker.mock(TypeToken.of(clazz));
    }

    @SuppressWarnings("unchecked")
    public static @Nonnull <T> T root(@Nonnull TypeLiteral<T> type) {
        Assert.notNull(type, "type");
        return (T) Mocker.mock(type.toTypeToken());
    }

    @SuppressWarnings({"unchecked", "UnusedParameters"})
    public static @Nonnull <T> BeanPath<T> $(T callChain) {
        final BeanPath<?> path = CurrentPath.evict();
        if (path == null) {
            throw new BeanPathMagicException("No current path. Probably your call chain contains a final method.");
        }
        return (BeanPath<T>) path;
    }

    public static @Nonnull String $$(Object callChain) {
        return $(callChain).toDotDelimitedString();
    }

    // --------------------------------------------------------- Implementation

    private static class Mocker {

        private static final Map<TypeToken, Object> cache = new ConcurrentHashMap<TypeToken, Object>();
        private static final Object mockCreationGuard = new Object();

        @SuppressWarnings("unchecked")
        public static <T> T mock(TypeToken type) {
            Object mock = cache.get(type);
            if (mock == null) {
                synchronized (mockCreationGuard) { // we do not want to generate a mock twice
                    mock = cache.get(type);
                    if (mock == null) {
                        try {
                            mock = MockMaker.createMock(type.getRawType(), new MockInvocationHandler(type));
                        } catch (Exception x) {
                            throw new BeanPathMagicException("Failed to mock type [%s]", type, x);
                        }
                        cache.put(type, mock);
                    }
                }
            }
            return (T) mock;
        }

        private static class MockInvocationHandler implements InvocationHandler {

            // rawMockType can be inferred from mockType,
            // but TypeToken.getRawType() is relatively slow,
            // so avoid it in time critical invoke()

            private final TypeToken mockType;
            private final Class rawMockType;

            private MockInvocationHandler(TypeToken mockType) {
                this.mockType = mockType;
                this.rawMockType = mockType.getRawType();
            }

            @Override
            public Object invoke(Object target, Method method, Object[] args) throws Throwable {

                CurrentPath.initIfNotAlready(rawMockType);

                final Type genericReturnType = method.getGenericReturnType();
                Class rawReturnType = method.getReturnType();
                TypeToken returnType;

                // again, TypeToken.getRawType() is slow, avoid it in simple cases
                if (genericReturnType == rawReturnType) {
                    returnType = TypeToken.of(rawReturnType);
                } else {
                    returnType = mockType.resolveType(genericReturnType);
                    rawReturnType = returnType.getRawType();
                }

                final String name = NameUtils.stripGetIsPrefixIfAny(method.getName());
                final Class type = rawReturnType.isPrimitive() ? Primitives.getWrapperClass(rawReturnType) : rawReturnType;

                CurrentPath.append(name, type);

                if (rawReturnType.isPrimitive()) {
                    // including void.class, that makes no sense,
                    // but anyway we can handle it
                    return Primitives.getDefaultValue(rawReturnType);
                } else if (Modifier.isFinal(rawReturnType.getModifiers())) {
                    // for String, primitive wrappers, enums and arrays,
                    // that we can't proxy, but must handle
                    // when they close property chain
                    return null;
                } else {
                    return mock(returnType);
                }
            }
        }
    }

    private static class CurrentPath {

        private static final ThreadLocal<BeanPath<?>> currentPathTL = new ThreadLocal<BeanPath<?>>();

        public static void initIfNotAlready(Class<?> clazz) {
            if (currentPathTL.get() == null) {
                currentPathTL.set(BeanPath.root(clazz));
            }
        }

        public static void append(String name, Class<?> type) {
            final BeanPath<?> path = currentPathTL.get();
            assert (path != null);
            currentPathTL.set(path.append(name, type));
        }

        public static BeanPath<?> evict() {
            final BeanPath<?> path = currentPathTL.get();
            currentPathTL.set(null);
            return path;
        }
    }

    private static class NameUtils {

        private static final String IS = "is", GET = "get";

        public static String stripGetIsPrefixIfAny(final String name) {

            assert (name != null);

            if (name.length() > GET.length() && name.startsWith(GET) && isUpperCase(name.charAt(GET.length()))) {
                return stripAndDecapitalize(name, GET);
            } else if (name.length() > IS.length() && name.startsWith(IS) && isUpperCase(name.charAt(IS.length()))) {
                return stripAndDecapitalize(name, IS);
            }
            return name;
        }

        private static String stripAndDecapitalize(String name, String prefix) {

            final int nameLength = name.length();
            final int prefixLength = prefix.length();
            final int i = prefixLength + 1;

            if (nameLength <= i || !isUpperCase(name.charAt(i))) {
                final char chars[] = name.toCharArray();
                chars[prefixLength] = Character.toLowerCase(chars[prefixLength]);
                return new String(chars, prefixLength, nameLength - prefixLength);
            } else {
                return name.substring(prefixLength);
            }
        }
    }
}
