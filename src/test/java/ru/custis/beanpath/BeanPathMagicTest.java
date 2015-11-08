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


import org.junit.Test;
import ru.custis.beanpath.beans.Document;
import ru.custis.beanpath.beans.Gender;
import ru.custis.beanpath.beans.Identified;
import ru.custis.beanpath.beans.NamesBean;
import ru.custis.beanpath.beans.Person;
import ru.custis.beanpath.beans.PrimitiveBean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static ru.custis.beanpath.BeanPathMagic.$;
import static ru.custis.beanpath.BeanPathMagic.$$;
import static ru.custis.beanpath.BeanPathMagic.root;

public class BeanPathMagicTest {
    /*
     * Basic usage scenario
     */

    @Test
    public void basicUsage() {
        final Person person = root(Person.class);

        // String property
        assertEquals(BeanPath.root(Person.class).append("name", String.class), $(person.getName()));

        // nested property
        assertEquals(BeanPath.root(Person.class).append("document", Document.class).append("number", String.class), $(person.getDocument().getNumber()));

        // primitive property
        assertEquals(BeanPath.root(Person.class).append("age", Integer.class), $(person.getAge()));

        // enum property
        assertEquals(BeanPath.root(Person.class).append("gender", Gender.class), $(person.getGender()));

        // recursive property
        assertEquals(BeanPath.root(Person.class).append("bestFriend", Person.class).append("name", String.class), $(person.getBestFriend().getName()));

        // double-dollar shortcut
        assertEquals("document.issuedBy", $$(person.getDocument().getIssuedBy()));
    }

    @Test
    public void mockCaching() {
        // The Framework caches mock instances on its type basis for best performance

        assertSame(root(Person.class), root(Person.class));
        assertSame(root(Document.class), root(Document.class));
        assertSame(root(new TypeLiteral<Identified<?>>() {}), root(new TypeLiteral<Identified<?>>() {}));
    }

    public abstract static class Uninstantaible {
        private Uninstantaible() {
            throw new AssertionError();
        }
    }

    @Test
    public void allocatingWithOutConstructorInvocation() {
        // The Framework allocates mock using sun.misc.Unsafe.allocateObject()
        // without constructor invocation
        root(Uninstantaible.class);
    }

    /*
     * Naming and type conversion concepts
     */

    @Test
    public void propertyNamingConcepts() {
        // Java Bean style properties converts appropriately

        final NamesBean names = root(NamesBean.class);
        assertEquals("property", $(names.getProperty()).getName()); // 'get' prefix
        assertEquals("property", $(names.isProperty()).getName()); // 'is' prefix
        assertEquals("a", $(names.getA()).getName()); // notice lowercase 'a'
        assertEquals("UTC", $(names.getUTC()).getName()); // notice 'UTC' in uppercase

        // otherwise lives unchanged

        assertEquals("property", $(names.property()).getName()); // no prefix
        assertEquals("is", $(names.is()).getName());  // 'prefix' only
        assertEquals("get", $(names.get()).getName()); // 'prefix' only
        assertEquals("getting", $(names.getting()).getName()); // looks like prefix but it's not
        assertEquals("isabel", $(names.isabel()).getName()); // looks like prefix but it's not
    }

    @Test
    public void primitiveTypesWrapping() {
        // Primitive types promotes to their corresponding wrapper types

        final PrimitiveBean primitives = root(PrimitiveBean.class);
        assertEquals(Boolean.class, $(primitives.getBoolean()).getType());
        assertEquals(Character.class, $(primitives.getChar()).getType());
        assertEquals(Byte.class, $(primitives.getByte()).getType());
        assertEquals(Short.class, $(primitives.getShort()).getType());
        assertEquals(Integer.class, $(primitives.getInt()).getType());
        assertEquals(Long.class, $(primitives.getLong()).getType());
        assertEquals(Float.class, $(primitives.getFloat()).getType());
        assertEquals(Double.class, $(primitives.getDouble()).getType());
        primitives.getVoid();
        assertEquals(Void.class, $((Void) null).getType());
    }

    /*
     * Examples with generics
     */

    @Test
    public void generics_actualTypeParameterResolution() {
        final Person person = root(Person.class);

        // The Framework is smart enough to resolve actual type parameter
        assertEquals(Long.class, $(person.getId()).getType());
    }

    @Test
    public void generics_wildCardedTypes() {
        final Person person = root(Person.class);

        // Wildcard type parameter erases to its upper bound...
        assertEquals(Number.class, $(person.getNumbers().get(0)).getType());

        // ...that may be Object implicitly
        assertEquals(Object.class, $(person.getStuff().next()).getType());
    }

    @Test
    public void generics_TypeLiteralUsage() {
        // One can use TypeToken
        final Identified<String> identified = root(new TypeLiteral<Identified<String>>() {});
        assertEquals(String.class, $(identified.getId()).getType());
    }

    /*
     * Examples of awkward and very likely meaningless use, but still legal
     */

    @Test
    public void awkward_propertyWithParameter() {
        final Person person = root(Person.class);
        assertEquals(BeanPath.root(Person.class).append("withParam", String.class), $(person.withParam(0)));
    }

    @Test
    public void awkward_voidReturnType() {
        root(Person.class).sleep();
        final BeanPath<Void> path = $((Void) null);
        assertEquals(BeanPath.root(Person.class).append("sleep", Void.class), path);
    }

    /*
     * Examples of illegal use
     */

    @Test(expected = BeanPathMagicException.class)
    public void illegal_noCurrentPath() {
        $(null);
    }

    @Test
    public void illegal_finalClass() {
        final Person person = root(Person.class);

        // Attempt to track property of final class fails with NullPointerException

        try {
            $(person.getName().getBytes()); // getName() returns String, which is a final class
            fail();
        } catch (NullPointerException ignored) {
        }

        // because The Framework cannot mock final classes (including String, all primitive wrappers, enums and arrays)
        // and intentionally returns null (to point out illegal use with NullPointerException)

        assertNull(person.getName());

        $(null); // clean up
    }

    public static class ParanoidPerson {
        private String getSecret() {
            return "The Secret";
        }

        public final String getFinalSecret() {
            return "The Final Secret";
        }
    }

    @Test
    public void illegal_privateMethod() {
        final ParanoidPerson paranoidPerson = root(ParanoidPerson.class);

        // The Framework cannot intercept private method invocation
        // (although visible here, but not visible to the Framework)

        try {
            $(paranoidPerson.getSecret());
            fail();
        } catch (BeanPathMagicException ignored) {
        }

        // Such invocation just fall through to the real method

        assertEquals("The Secret", paranoidPerson.getSecret());
    }

    @Test(expected = BeanPathMagicException.class)
    public void illegal_finalMethod() {
        ParanoidPerson paranoidPerson = root(ParanoidPerson.class);
        assertEquals("The Final Secret", paranoidPerson.getFinalSecret());

        // The Framework cannot intercept private method invocation
        // (although visible here, but not visible to the Framework)

        $(paranoidPerson.getFinalSecret());
    }
}
