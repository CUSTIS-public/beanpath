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

package ru.custis.beanpath.beans;

import java.util.Iterator;
import java.util.List;

public class Person extends Identified<Long> {

    public String getName() { return "John Smith"; }

    public int getAge() { return 27; }

    public Document getDocument() { return new Document(); }

    public Person getBestFriend() { return new Person(); }

    public Gender getGender() { return Gender.MALE; }

    public String withParam(int param) { return ""; }

    public List<? extends Number> getNumbers() { return null; }

    public Iterator<?> getStuff() { return null; }

    public void sleep() { }
}
