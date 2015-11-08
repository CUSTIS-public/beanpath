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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BeanPathTest {
    @SuppressWarnings("ConstantConditions")
    @Test
    public void basic() {
        final BeanPath<DatabaseMetaData> path =
                BeanPath.root(DataSource.class).append("connection", Connection.class).append("metaData", DatabaseMetaData.class);

        assertPathIs("metaData", DatabaseMetaData.class, false, path);
        assertPathIs("connection", Connection.class, false, path.getParent());
        assertPathIs("<root>", DataSource.class, true, path.getParent().getParent());
        assertNull(path.getParent().getParent().getParent());
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
        assertPathIs("<root>", DataSource.class, true, root);
        assertNull(root.getParent());
        assertSame(root, root.getRoot());
    }

    @Test
    public void dotDelimitedStringRepresentation() {
        final BeanPath<DatabaseMetaData> path =
                BeanPath.root(DataSource.class).append("connection", Connection.class).append("metaData", DatabaseMetaData.class);

        assertEquals("connection.metaData", path.toDotDelimitedString());
        assertSame(path.toDotDelimitedString(), path.toDotDelimitedString());

        assertEquals("", path.getRoot().toDotDelimitedString());
    }

    @Test
    public void iterator() {
        final BeanPath<DatabaseMetaData> path =
                BeanPath.root(DataSource.class).append("connection", Connection.class).append("metaData", DatabaseMetaData.class);

        final Iterator<BeanPath<?>> iterator = path.iterator();
        assertTrue(iterator.hasNext());
        assertPathIs("<root>", DataSource.class, true, iterator.next());
        assertTrue(iterator.hasNext());
        assertPathIs("connection", Connection.class, false, iterator.next());
        assertTrue(iterator.hasNext());
        assertPathIs("metaData", DatabaseMetaData.class, false, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void equalsAndHashCode() {
        final BeanPath<String> somePath = BeanPath.root(Object.class).append("foo", String.class);
        final BeanPath<String> samePath = BeanPath.root(Object.class).append("foo", String.class);
        final BeanPath<String> otherPath = BeanPath.root(Object.class).append("bar", String.class);

        assertNotNull(somePath);
        assertNotEquals(new Object(), somePath);

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

        assertEquals("<root>:DataSource/connection:Connection/metaData:DatabaseMetaData", path.toString());
        assertSame(path.toString(), path.toString());
    }

    private static void assertPathIs(String name, Class type, boolean isRoot, BeanPath path) {
        assertEquals(name, path.getName());
        assertEquals(type, path.getType());
        assertEquals(isRoot, path.isRoot());
    }
}
