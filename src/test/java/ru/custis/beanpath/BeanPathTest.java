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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Iterator;

import static org.testng.Assert.*;

public class BeanPathTest {
    @SuppressWarnings("ConstantConditions")
    @Test
    public void basic() {
        final BeanPath<DatabaseMetaData> path =
                BeanPath.root(DataSource.class).append("connection", Connection.class).append("metaData", DatabaseMetaData.class);

        assertPathIs(path, "metaData", DatabaseMetaData.class, false);
        assertPathIs(path.getParent(), "connection", Connection.class, false);
        assertPathIs(path.getParent().getParent(), "<root>", DataSource.class, true);
        assertEquals(path.getParent().getParent().getParent(), null);
    }

    @Test
    public void pathInvariants() {
        final BeanPath<Connection> path = BeanPath.root(DataSource.class).append("connection", Connection.class);

        assertEquals(path.hasParent(), path.getParent() != null);
        assertEquals(path.isRoot(), path.getRoot() == path);
        assertEquals(!path.isRoot(), path.hasParent());
    }

    @Test
    public void rootInvariants() {
        final BeanPath<DataSource> root = BeanPath.root(DataSource.class);

        assertTrue(root.isRoot());
        assertFalse(root.hasParent());
        assertPathIs(root, "<root>", DataSource.class, true);
        assertEquals(root.getParent(), null);
        assertSame(root, root.getRoot());
    }

    @Test
    public void dotDelimitedStringRepresentation() {
        final BeanPath<DatabaseMetaData> path =
                BeanPath.root(DataSource.class).append("connection", Connection.class).append("metaData", DatabaseMetaData.class);

        assertEquals(path.toDotDelimitedString(), "connection.metaData");
        assertSame(path.toDotDelimitedString(), path.toDotDelimitedString());

        assertEquals(path.getRoot().toDotDelimitedString(), "");
    }

    @Test
    public void iterator() {
        final BeanPath<DatabaseMetaData> path =
                BeanPath.root(DataSource.class).append("connection", Connection.class).append("metaData", DatabaseMetaData.class);

        final Iterator<BeanPath<?>> iterator = path.iterator();
        assertTrue(iterator.hasNext());
        assertPathIs(iterator.next(), "<root>", DataSource.class, true);
        assertTrue(iterator.hasNext());
        assertPathIs(iterator.next(), "connection", Connection.class, false);
        assertTrue(iterator.hasNext());
        assertPathIs(iterator.next(), "metaData", DatabaseMetaData.class, false);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void equalsAndHashCode() {
        final BeanPath<String> somePath = BeanPath.root(Object.class).append("foo", String.class);
        final BeanPath<String> samePath = BeanPath.root(Object.class).append("foo", String.class);
        final BeanPath<String> otherPath = BeanPath.root(Object.class).append("bar", String.class);

        assertNotEquals(somePath, null);
        assertNotEquals(somePath, new Object());

        assertEquals(somePath, somePath);
        assertEquals(somePath, samePath);
        assertNotEquals(somePath, otherPath);
        assertNotEquals(samePath, otherPath);

        assertEquals(somePath.hashCode(), samePath.hashCode());
    }

    @Test
    public void toStringRepresentation() {
        final BeanPath<DatabaseMetaData> path =
                BeanPath.root(DataSource.class).append("connection", Connection.class).append("metaData", DatabaseMetaData.class);

        assertEquals(path.toString(), "<root>:DataSource/connection:Connection/metaData:DatabaseMetaData");
        assertSame(path.toString(), path.toString());
    }

    private static void assertPathIs(BeanPath path, String name, Class type, boolean isRoot) {
        assertEquals(path.getName(), name);
        assertEquals(path.getType(), type);
        assertEquals(path.isRoot(), isRoot);
    }
}
