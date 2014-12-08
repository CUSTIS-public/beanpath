beanpath
========

Staticly checked Java Bean property paths to be used instead of string literals.

Eg:
```java
Account account = root(Account.class);
BeanPath<String> customerNameProp = $( account.getCustomer().getName() );
// then customerNameProp.toDotDelimitedString() => "customer.name"
```

The core idea is to use runtime proxies to capture method (as in mock frameworks).

See more examples and some explanation in [BeanPathMagicTest](https://github.com/CUSTIS-public/beanpath/blob/master/src/test/java/ru/custis/beanpath/BeanPathMagicTest.java).

More info on [Habrahabr](http://habrahabr.ru/company/custis/blog/243803/) [ru]
