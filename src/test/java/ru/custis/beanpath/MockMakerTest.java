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

import net.sf.cglib.proxy.InvocationHandler;
import org.testng.annotations.Test;
import ru.custis.beanpath.beans.Person;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static org.testng.Assert.*;

public class MockMakerTest {
    private static final InvocationHandler errorThrowingHandler = new InvocationHandler() {
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
        final Person mock = MockMaker.createMock(Person.class, errorThrowingHandler);

        final String mockClassName = mock.getClass().getName();
        assertTrue(mockClassName.startsWith("ru.custis.beanpath.BeanPathMagicMock_of_" + Person.class.getName()), mockClassName);
    }

    public static class MyStringCallable implements Callable<String> {
        @Override public String call() throws Exception {
            return null;
        }
    }

    @Test
    public void bridgeMethodDelegation() throws Exception {
        final boolean[] wasInvoked = new boolean[]{false};

        final MyStringCallable mock = MockMaker.createMock(MyStringCallable.class, new InvocationHandler() {
            @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                assertEquals(method.getDeclaringClass(), MyStringCallable.class);

                final StackTraceElement bridgeMethodStackTraceElement = new Exception().getStackTrace()[2];
                assertEquals(bridgeMethodStackTraceElement.getClassName(), MyStringCallable.class.getName());
                assertEquals(bridgeMethodStackTraceElement.getMethodName(), "call");

                wasInvoked[0] = true;

                return null;
            }
        });

        ((Callable<?>) mock).call(); // cast to the interface to cause bridge method invocation

        assertTrue(wasInvoked[0]);
    }

    @SuppressWarnings({"ObjectEqualsNull", "EqualsWithItself"})
    @Test
    public void equalsMethodImplementation() throws Exception {
        final Person mock = MockMaker.createMock(Person.class, errorThrowingHandler);

        assertTrue(mock.equals(mock));
        assertFalse(mock.equals(null));
        assertFalse(mock.equals(new Object()));
    }

    @Test
    public void hashCodeMethodImplementation() throws Exception {
        final Person mock = MockMaker.createMock(Person.class, errorThrowingHandler);

        assertEquals(mock.hashCode(), mock.hashCode());
    }

    @Test
    public void toStringMethodImplementation() throws Exception {
        final Person mock = MockMaker.createMock(Person.class, errorThrowingHandler);

        assertTrue(mock.toString().startsWith("ru.custis.beanpath.BeanPathMagicMock_of_" + Person.class.getName()));
        assertTrue(mock.toString().contains("@"));
    }
}
