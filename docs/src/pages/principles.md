# Principles

The goal of Krop is to make building web services and applications easy. There are many existing web service toolkits, so this section lays out the principles that guide the development of Krop and shows how it differs from other toolkits.

## Simple Things Are Easy

Above all, Krop aims to make it really really easy to create web services. It should be trivial to create a simple website that consists of a few pages and some interactive parts. It should be equally easy to create a web service that responds to a few end points.

In the event you need features not provided by Krop, you can `unwrap` types to obtain their http4s equivalent and work with them directly.


## An Amazing Developer Experience

Krop wants to address the whole lifecycle of creating web applications. This means not only making it easy to write code, but providing

* excellent documentation for accomplishing common tasks, such as serving files, connecting to a database, and so on;
* support for the development process, such as showing why routes didn't match a given request; and
* tried and tested architectures for efficient development of scalable systems.
