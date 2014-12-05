beanpath
========

More info on [Habrahabr](http://habrahabr.ru/company/custis/blog/243803/) [ru]

Same as in Mockito:

```java
Account account = root(Account.class);
tableBuilder.addColumn( $( account.gertCustomer().getName() ) );
```
when 

```java
$( account.gertCustomer().getName() ).toDotDelimitedString() => "customer.name"
```
