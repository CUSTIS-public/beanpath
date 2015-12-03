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


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Models chain of bean properties in object oriented manner
 */
@Immutable
public class BeanPath<T> implements Iterable<BeanPath<?>>, Serializable {
    private static final long serialVersionUID = 42L;

    private final BeanPath<?> parent;
    private final String name;
    private final Class<T> type;

    private BeanPath(BeanPath<?> parent, String name, Class<T> type) {
        this.parent = parent;
        this.name = Assert.notNull(name, "name");
        this.type = Assert.notNull(type, "type");
    }

    /**
     * Creates root path of given {@code type}, with pseudo name {@code <root>} and no parent
     */
    public static @Nonnull <T> BeanPath<T> root(@Nonnull Class<T> type) {
        Assert.notNull(type, "type");
        return new BeanPath<T>(null, "<root>", type);
    }

    /**
     * Appends an element to this path and returns the new path.
     * Creates new instance, leaves {@code this} intact.
     */
    public @Nonnull <T1> BeanPath<T1> append(@Nonnull String name, @Nonnull Class<T1> type) {
        Assert.notNull(name, "name");
        Assert.notNull(type, "type");
        return new BeanPath<T1>(this, name, type);
    }

    /**
     * Is this path is root
     * <p/>
     * It honors invariants:
     * <pre><code>
     *     this.isRoot() == (this.getParent() == null)
     *     this.isRoot() == !this.hasParent()
     * </code></pre>
     */
    public boolean isRoot() {
        return (parent == null);
    }

    /**
     * The root of the path, i.e. vacuous path with one element that represents a bean
     * on which property chain applicated.
     * <p/>
     * It honors invariants:
     * <pre><code>
     *   (this.getRoot() == this) && this.isRoot()
     *   (this.getRoot() == this.getRoot().getRoot()) // and so on
     * </code></pre>
     */
    public @Nonnull BeanPath<?> getRoot() {
        return (parent == null) ? this : parent.getRoot();
    }

    /**
     * Whether the path has a parent
     * <p/>
     * It honors invariants:
     * <pre><code>
     *     this.hasParent() == (this.getParent() != null)
     *     this.hasParent() == !this.isRoot()
     * </code></pre>
     */
    public boolean hasParent() {
        return (parent != null);
    }

    /**
     * Parent path of this path, i.e. path without last (tail) property;
     * or {@code null} if {@code this.isRoot()}
     */
    public @Nullable BeanPath<?> getParent() {
        return parent;
    }

    /**
     * Name of the path element, i.e. name of the last (tail) property in the chain
     */
    public @Nonnull String getName() {
        return name;
    }

    /**
     * Type of the path, i.e. type of the last (tail) property in the chain
     */
    public @Nonnull Class<T> getType() {
        return type;
    }

    /**
     * Iterator over path elements, from {@code root} to {@code this}.
     * Contains at lest one path element — {@code this}, in case of
     * {@code this.isRoot()}.
     */
    @Override
    public @Nonnull Iterator<BeanPath<?>> iterator() {
        return toCollection(new LinkedList<BeanPath<?>>()).iterator();
    }

    private Collection<BeanPath<?>> toCollection(Collection<BeanPath<?>> collection) {
        if (hasParent()) {
            parent.toCollection(collection);
        }
        collection.add(this);
        return collection;
    }

    /**
     * Representation of the path in well familiar dot-delimited notation,
     * e.g. {@code foo.bar.baz}.
     * <p/>
     * Nameless root is not included. For a root path it returns an empty string.
     */
    public @Nonnull String toDotDelimitedString() {
        String dds = cachedDotDelimitedString;
        if (dds == null) {
            cachedDotDelimitedString = dds = toDotDelimitedString(new StringBuilder(32)).toString();
        }
        return dds;
    }

    // Instances of BeanPath is immutable,
    // thus we can lazy compute and cache derived properties.
    // Furthermore, we do not need synchronization:
    // its ok if two threads compute it twice concurrently.
    private transient String cachedDotDelimitedString = null;

    private StringBuilder toDotDelimitedString(StringBuilder sb) {
        if (isRoot()) {
            return sb;
        }
        if (getRoot() != parent) {
            parent.toDotDelimitedString(sb);
            sb.append('.');
        }
        sb.append(name);
        return sb;
    }

    /**
     * Whether two paths are equal, i.e. represents same property chain
     * on same root bean
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final BeanPath that = (BeanPath) obj;
        return (this.parent == that.parent
                || (this.parent != null && that.parent != null && this.parent.equals(that.parent)))
               && this.name.equals(that.name)
               && this.type.equals(that.type);
    }

    /**
     * Hash code of the path, consistent with equality
     */
    @Override
    public int hashCode() {
        int hc = cachedHashCode;
        if (hc == 0) {
            hc = (parent != null) ? parent.hashCode() : 0;
            hc = 31 * hc + name.hashCode();
            hc = 31 * hc + type.hashCode();
            cachedHashCode = hc;
        }
        return hc;
    }

    // Instances of BeanPath are immutable,
    // thus we can lazy compute and cache derived properties.
    // Furthermore, we do not need synchronization:
    // its ok if two threads compute it twice concurrently.
    private transient int cachedHashCode = 0;

    /**
     * String representation of the path, for debugging purposes.
     * Currently it looks like this:
     * <pre>{@code
     * <root>:TypeOfRoot//propertyOne:Type//someString:String
     * }</pre>
     * <p/>
     * But do not relay on it it is subject to change without notice!
     */
    @Override
    public @Nonnull String toString() {
        String ts = cachedToString;
        if (ts == null) {
            cachedToString = ts = toString(new StringBuilder(32)).toString();
        }
        return ts;
    }

    // Instances of BeanPath are immutable,
    // thus we can lazy compute and cache derived properties.
    // Furthermore, we do not need synchronization:
    // its ok if two threads compute it twice concurrently.
    private transient String cachedToString = null;

    private StringBuilder toString(StringBuilder sb) {
        if (hasParent()) {
            parent.toString(sb);
            sb.append("/");
        }
        sb.append(name).append(':').append(type.getSimpleName());
        return sb;
    }
}
