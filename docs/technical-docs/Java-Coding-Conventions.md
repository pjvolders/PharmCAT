---
title: Coding Convention
permalink: technical-docs/coding-conventions
parent: Technical Documentation
nav_order: 7
---

Recommended reading: [Java For Small Teams](https://www.gitbook.com/book/ncrcoe/java-for-small-teams/).


## Code Style

We mainly follow [Google's Java style](https://google-styleguide.googlecode.com/svn/trunk/javaguide.html) guide.

#### Key Points

Formatting:

* Spaces, not tabs.
* Block indentation is 2 spaces.
* K&R style
* Braces are not optional

Example:

```java
public String doSomething(String str, Iterator itr) throws Exception {

  if (str == null) {
    complain("str is null!");
  } else {
    System.out.println(str);
  }
  while (itr.hasNext()) {
    String val = (String)itr.next();
    System.out.println(val);
  }
  return "Done!";
}
```

> **Not Optional:** Make sure you put braces ({}) around all conditionals (i.e. if, else, while, for, etc.), no matter how short. This makes it much harder to make any scoping mistakes.

Naming Conventions:

* For class names, use upper camel case.
* For method and variable names, use lower camel case.
* In class and method names, acronyms and contractions should only have the first letter capitalized (eg. Db, Kb, Url).  This is in keeping with the camel casing convention.
* Longer, more descriptive names are better than short cryptic names.
* Public static final variables should be in all caps.
* One variable per declaration (e.g. no `int a, b;`)

Import order:

1. `java` imports
1. `javax` imports
1. all non-static imports, in ASCII order
1. blank line
1. all static imports


## Annotating Source Code

Use javax.annotation's `@Nullable`/`@Nonnull` annotations directly in the source.


## Logging

Internally, we use [SLF4J](http://www.slf4j.org) for all our logging needs.  This, however, is just an interface.  The actual logging implementation we use is [Logback](http://logback.qos.ch/).


## Documentation

All code should be documented, and you should use proper Javadoc formatting. If in doubt, consult the Javadoc Website.
