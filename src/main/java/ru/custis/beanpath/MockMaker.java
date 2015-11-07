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

import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.core.Predicate;
import net.sf.cglib.proxy.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/*package-local*/ class MockMaker {
    private MockMaker() {} // static use only

    public static <T> T createMock(Class<T> type, InvocationHandler handler) throws InstantiationException {
        Assert.notNull(type, "type");
        Assert.notNull(handler, "handler");

        final Class mockClass = generateClass(type);
        final Object mock = StolenUnsafe.getUnsafe().allocateInstance(mockClass);

        ((Factory) mock).setCallbacks(new Callback[]{defaultObjectMethodsHandler, NoOp.INSTANCE, handler});

        return type.cast(mock);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> generateClass(Class<T> clazzToMock) {
        final Enhancer enhancer = new Enhancer() {
            @Override protected void filterConstructors(Class sc, List constructors) {
                // Don't filter; because default implementation filters out
                // private constructors, but we are not going to use constructors at all
            }
        };

        enhancer.setUseCache(false);
        enhancer.setUseFactory(true);

        enhancer.setNamingPolicy(mockNamingPolicy);

        // try to fix classloader issue
        enhancer.setClassLoader(MockMaker.class.getClassLoader());

        if (clazzToMock.isInterface()) {
            enhancer.setSuperclass(Object.class);
            enhancer.setInterfaces(new Class[]{clazzToMock});
        } else {
            enhancer.setSuperclass(clazzToMock);
        }

        enhancer.setCallbackFilter(MockCallbackFilter.INSTANCE);
        enhancer.setCallbackTypes(CALLBACK_TYPES);

        return enhancer.createClass();
    }

    private static final AtomicLong counter = new AtomicLong(0);

    private static final NamingPolicy mockNamingPolicy = new NamingPolicy() {
        @Override
        public String getClassName(String prefix, String source, Object key, Predicate names) {
            return getClass().getPackage().getName() + ".BeanPathMagicMock_of_" + prefix + "_$" + counter.getAndIncrement();
        }
    };

    private static final Class[] CALLBACK_TYPES = new Class[]{InvocationHandler.class, NoOp.class, InvocationHandler.class};

    private static class MockCallbackFilter implements CallbackFilter {
        public static final MockCallbackFilter INSTANCE = new MockCallbackFilter();

        @Override
        public int accept(Method method) {
            if (MethodsGuru.isEqualsHashCodeOrToStringMethod(method)) {
                return 0; // -> ObjectMethodsHandler.INSTANCE
            } else if (method.isBridge()) {
                return 1; // -> NoOp.INSTANCE
            } else {
                return 2; // -> InvocationHandler
            }
        }
    }

    private static final InvocationHandler defaultObjectMethodsHandler = new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (MethodsGuru.isEqualsMethod(method)) {
                return (proxy == args[0]);
            } else if (MethodsGuru.isHashCodeMethod(method)) {
                return System.identityHashCode(proxy);
            } else if (MethodsGuru.isToStringMethod(method)) {
                return proxy.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(proxy));
            }
            throw new RuntimeException("Impossible occurred: Not a java.lang.Object method: " + method);
        }
    };

    private abstract static class MethodsGuru {
        public static boolean isEqualsHashCodeOrToStringMethod(Method method) {
            return isEqualsMethod(method) || isHashCodeMethod(method) || isToStringMethod(method);
        }

        public static boolean isEqualsMethod(Method method) {
            return method.getParameterTypes().length == 1
                   && method.getName().equals("equals")
                   && method.getParameterTypes()[0] == Object.class;
        }

        public static boolean isHashCodeMethod(Method method) {
            return method.getParameterTypes().length == 0
                   && method.getName().equals("hashCode");
        }

        public static boolean isToStringMethod(Method method) {
            return method.getParameterTypes().length == 0
                   && method.getName().equals("toString");
        }
    }
}
