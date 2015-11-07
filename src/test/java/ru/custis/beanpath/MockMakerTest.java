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

import org.testng.annotations.Test;
import ru.custis.beanpath.MockMaker.InvocationCallback;
import ru.custis.beanpath.beans.Person;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.*;

public class MockMakerTest {
    private static final InvocationCallback errorThrowingHandler = new InvocationCallback() {
        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            throw new AssertionError();
        }
    };

    @Test
    public void creatingSeveralMocksOfSameType() throws Exception {
        MockMaker.createMock(Person.class, errorThrowingHandler);
        MockMaker.createMock(Person.class, errorThrowingHandler);
    }

    @Test
    public void namingPolicy() throws Exception {
        Person mock = MockMaker.createMock(Person.class, errorThrowingHandler);

        String mockClassName = mock.getClass().getName();
        assertTrue(mockClassName.startsWith("ru.custis.beanpath.BeanPathMagicMock_of_" + Person.class.getName()), mockClassName);
    }

    public static class MyStringCallable implements Callable<String> {
        @Override public String call() {
            return null;
        }
    }

    @Test
    public void bridgeMethodDelegation() throws Exception {
        final AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        final Method expectedMethod = MyStringCallable.class.getDeclaredMethod("call");

        final MyStringCallable mock = MockMaker.createMock(MyStringCallable.class, new InvocationCallback() {
            @Override public Object invoke(Object proxy, Method method, Object[] args) {
                assertEquals(method.getDeclaringClass(), MyStringCallable.class);
                assertEquals(method, expectedMethod);

                callbackInvoked.set(true);

                return null;
            }
        });

        assertFalse(callbackInvoked.get());

        ((Callable<?>) mock).call(); // cast to the interface to cause bridge method invocation

        assertTrue(callbackInvoked.get());
    }

    @SuppressWarnings({"ObjectEqualsNull", "EqualsWithItself"})
    @Test
    public void equalsMethodImplementation() throws Exception {
        Person mock1 = MockMaker.createMock(Person.class, errorThrowingHandler);
        Person mock2 = MockMaker.createMock(Person.class, errorThrowingHandler);

        assertTrue(mock1.equals(mock1));
        assertFalse(mock1.equals(null));
        assertFalse(mock1.equals(new Object()));
        assertFalse(mock1.equals(mock2));
    }

    @Test
    public void hashCodeMethodImplementation() throws Exception {
        Person mock1 = MockMaker.createMock(Person.class, errorThrowingHandler);
        Person mock2 = MockMaker.createMock(Person.class, errorThrowingHandler);

        assertEquals(mock1.hashCode(), mock1.hashCode());
        assertNotEquals(mock1.hashCode(), mock2.hashCode());
    }

    @Test
    public void toStringMethodImplementation() throws Exception {
        Person mock = MockMaker.createMock(Person.class, errorThrowingHandler);

        String str = mock.toString();
        assertNotNull(str);
        assertTrue(str.startsWith(mock.getClass().getName()));
        assertTrue(str.contains("@"));
    }

    public abstract static class Uninstantaible {
        private Uninstantaible() {
            throw new AssertionError();
        }
    }

    @Test
    public void mockUninstantaible() throws Exception {
        // The Framework is able to mock classes without constructor invocation
        Uninstantaible mock = MockMaker.createMock(Uninstantaible.class, errorThrowingHandler);

        assertNotNull(mock);
        assertTrue(Uninstantaible.class.isAssignableFrom(mock.getClass()));
    }

    public interface SomeInterface {
    }

    @Test
    public void mockInterface() throws Exception {
        // The Framework is able to mock interfaces
        SomeInterface mock = MockMaker.createMock(SomeInterface.class, errorThrowingHandler);

        assertNotNull(mock);
        assertTrue(SomeInterface.class.isAssignableFrom(mock.getClass()));
    }
}
