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
import ru.custis.beanpath.beans.*;

import static org.testng.Assert.*;
import static ru.custis.beanpath.BeanPathMagic.*;

public class BeanPathMagicTest {

    /*
     * Basic usage scenario
     */

    @Test
    public void basicUsage() {

        final Person person = root(Person.class);

        // String property
        assertEquals($(person.getName()),
                BeanPath.root(Person.class).append("name", String.class));

        // nested property
        assertEquals($(person.getDocument().getNumber()),
                BeanPath.root(Person.class).append("document", Document.class).append("number", String.class));

        // primitive property
        assertEquals($(person.getAge()),
                BeanPath.root(Person.class).append("age", Integer.class));

        // enum property
        assertEquals($(person.getGender()),
                BeanPath.root(Person.class).append("gender", Gender.class));

        // recursive property
        assertEquals($(person.getBestFriend().getName()),
                BeanPath.root(Person.class).append("bestFriend", Person.class).append("name", String.class));

        // double-dollar shortcut
        assertEquals($$(person.getDocument().getIssuedBy()), "document.issuedBy");
    }

    @Test
    public void mockCaching() {

        // The Framework caches mock instances on its type basis for best performance

        assertSame(root(Person.class), root(Person.class));
        assertSame(root(Document.class), root(Document.class));
        assertSame(root(new TypeLiteral<Identified<?>>() {
        }), root(new TypeLiteral<Identified<?>>() {
        }));
    }

    public static abstract class Uninstantaible {

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
        assertEquals($(names.getProperty()).getName(), "property"); // 'get' prefix
        assertEquals($(names.isProperty()).getName(), "property"); // 'is' prefix
        assertEquals($(names.getA()).getName(), "a"); // notice lowercase 'a'
        assertEquals($(names.getUTC()).getName(), "UTC"); // notice 'UTC' in uppercase

        // otherwise lives unchanged

        assertEquals($(names.property()).getName(), "property"); // no prefix
        assertEquals($(names.is()).getName(), "is");  // 'prefix' only
        assertEquals($(names.get()).getName(), "get"); // 'prefix' only
        assertEquals($(names.getting()).getName(), "getting"); // looks like prefix but it's not
        assertEquals($(names.isabel()).getName(), "isabel"); // looks like prefix but it's not
    }

    @Test
    public void primitiveTypesWrapping() {

        // Primitive types promotes to their corresponding wrapper types

        final PrimitiveBean primitives = root(PrimitiveBean.class);
        assertEquals($(primitives.getBoolean()).getType(), Boolean.class);
        assertEquals($(primitives.getChar()).getType(), Character.class);
        assertEquals($(primitives.getByte()).getType(), Byte.class);
        assertEquals($(primitives.getShort()).getType(), Short.class);
        assertEquals($(primitives.getInt()).getType(), Integer.class);
        assertEquals($(primitives.getLong()).getType(), Long.class);
        assertEquals($(primitives.getFloat()).getType(), Float.class);
        assertEquals($(primitives.getDouble()).getType(), Double.class);
        primitives.getVoid();
        assertEquals($((Void) null).getType(), Void.class);
    }

    /*
     * Examples with generics
     */

    @Test
    public void generics_actualTypeParameterResolution() {

        final Person person = root(Person.class);

        // The Framework is smart enough to resolve actual type parameter
        assertEquals($(person.getId()).getType(), Long.class);
    }

    @Test
    public void generics_wildCardedTypes() {

        final Person person = root(Person.class);

        // Wildcard type parameter erases to its upper bound...
        assertEquals($(person.getNumbers().get(0)).getType(), Number.class);

        // ...that may be Object implicitly
        assertEquals($(person.getStuff().next()).getType(), Object.class);
    }

    @Test
    public void generics_TypeLiteralUsage() {

        // One can use TypeToken
        final Identified<String> identified = root(new TypeLiteral<Identified<String>>() {
        });
        assertEquals($(identified.getId()).getType(), String.class);
    }

    /*
     * Examples of awkward and very likely meaningless use, but still legal
     */

    @Test
    public void awkward_propertyWithParameter() {
        final Person person = root(Person.class);
        assertEquals($(person.withParam(0)),
                BeanPath.root(Person.class).append("withParam", String.class));
    }

    @Test
    public void awkward_voidReturnType() {
        root(Person.class).sleep();
        final BeanPath<Void> path = $((Void) null);
        assertEquals(path, BeanPath.root(Person.class).append("sleep", Void.class));
    }

    /*
     * Examples of illegal use
     */

    @Test(expectedExceptions = BeanPathMagicException.class,
            expectedExceptionsMessageRegExp = "No current path.*")
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

        assertEquals(paranoidPerson.getSecret(), "The Secret");
    }

    @Test(expectedExceptions = BeanPathMagicException.class)
    public void illegal_finalMethod() {

        ParanoidPerson paranoidPerson = root(ParanoidPerson.class);
        assertEquals(paranoidPerson.getFinalSecret(), "The Final Secret");

        // The Framework cannot intercept private method invocation
        // (although visible here, but not visible to the Framework)

        $(paranoidPerson.getFinalSecret());
    }
}
