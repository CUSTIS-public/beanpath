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

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.WRAPPER;
import static net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default.NO_CONSTRUCTORS;
import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.isBridge;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/*package-local*/ class MockMaker {
    private MockMaker() {} // static use only

    public interface InvocationCallback {
        Object invoke(Object proxy, Method method, Object[] args) throws Throwable;
    }

    public static <T> T createMock(Class<T> type, InvocationCallback handler) throws InstantiationException {
        checkNotNull(type, "Argument 'type' must not be null");
        checkNotNull(handler, "Argument 'handler' must not be null");

        final Class<? extends T> mockClass = generateClass(type, handler);
        final Object mock = instantiateClass(mockClass);

        return type.cast(mock);
    }

    private static final ByteBuddy buddy = new ByteBuddy().withNamingStrategy(new MockNamingStrategy());

    private static <T> Class<? extends T> generateClass(Class<T> clazzToMock, InvocationCallback handler) {
        return
                buddy
                        .subclass(clazzToMock, NO_CONSTRUCTORS)

                        .method(not(isBridge()))
                        .intercept(to(new InvocationCallbackAdapter(handler)))

                        .method(named("equals").and(returns(boolean.class)).and(takesArguments(Object.class)))
                        .intercept(to(ObjectMethodsHandler.class))

                        .method(named("hashCode").and(returns(int.class)).and(takesArguments(0)))
                        .intercept(to(ObjectMethodsHandler.class))

                        .method(named("toString").and(returns(String.class)).and(takesArguments(0)))
                        .intercept(to(ObjectMethodsHandler.class))

                        .make()
                        .load(MockMaker.class.getClassLoader(), WRAPPER)
                        .getLoaded()
        ;
    }

    private static <T> Object instantiateClass(Class<T> mockClass) throws InstantiationException {
        return StolenUnsafe.getUnsafe().allocateInstance(mockClass);
    }

    private static class MockNamingStrategy implements NamingStrategy {
        private final AtomicLong counter = new AtomicLong(0);

        @Override
        public String name(UnnamedType unnamedType) {
            String prefix = unnamedType.getSuperClass().getTypeName();
            return getClass().getPackage().getName() + ".BeanPathMagicMock_of_" + prefix + "_$" + counter.getAndIncrement();
        }
    }

    @SuppressWarnings("unused")
    public static class InvocationCallbackAdapter {
        private final InvocationCallback callback;

        public InvocationCallbackAdapter(InvocationCallback callback) {
            this.callback = callback;
        }

        @RuntimeType
        public Object defaultHandler(@This Object proxy, @Origin Method method, @AllArguments Object[] args) throws Throwable {
            return callback.invoke(proxy, method, args);
        }
    }

    @SuppressWarnings("unused")
    public static class ObjectMethodsHandler {
        private ObjectMethodsHandler() {}

        public static boolean equalsHandler(@This Object thiz, @Argument(0) Object that) {
            return thiz == that;
        }

        public static int hashCodeHandler(@This Object proxy) {
            return System.identityHashCode(proxy);
        }

        public static String toStringHandler(@This Object proxy) {
            return proxy.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(proxy));
        }
    }
}
