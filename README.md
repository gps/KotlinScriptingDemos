# Kotlin Scripting Demos

This repository has the files I used to demo Scripting in Kotlin at [BLR Kotlin Meetup 7](https://www.meetup.com/BlrKotlin/events/252705874/).

## Presenter Notes

### Motivation

How many people here are Android developers?
How many people here know a scripting language? Perhaps Python, Ruby, Groovy, Perl, Bash?

### Why Scripting?

### Why Kotlin for Scripting?

### Kotlin’s Scripting Story

#### Demo 1
cat run.sh
./run.sh

### But… 

#### Demo 2
Show hello.kts
./hello.kts

Show run.sh
./run.sh

#### Demo 3
This example shows two things:

- How to bring in a dependency from maven central
- Using an argparser to create a good command line interface

./dep.kts

Explain the arg parse library. It has way more options to build really expressive command line UIs.

Point out the type conversion to int

#### Demo 4
This set of examples shows you how to use a really good JSON library: Klaxon

Caveat: the libraries I’m showing you are good for scripting. I’ve never used them in “production” code, so I have no idea how well they perform, etc.

a.kts
Show a basic parsing example

b.kts
Show how nullability works with Klaxon

c.kts
Show how default value for args work with Klaxon

d.kts
Show how you can change the key name in JSON

e.kts
Show how you can ignore certain fields  - notice the error
Uncomment the ignored line and show the line going away

f.kts
Show how you can force Klaxon to fill in even a private property

g.kts
Show how Klaxon can convert a data class to JSON string

h.kts
Show the json DSL that Klaxon has

i.kts
Show the lookup API that Klaxon has

#### Demo 5
This set of examples show you how to use a really good HTTP client: Fuel

a.kts
Show a basic get request

b.kts
Show a simple post request with JSON body

c.kts
Combine Klaxon and Fuel for profit

#### Demo 6
Really cool feature that kscript brings - you can include other kscript files

Show User.kts
Show a.kts
Show b.kts

Run a and b

Show how code is shared between the two

#### Demo 7

Show kscript’s “binary” deployment feature

You can package a kscript into a single fat binary.

Show Vagrantfile - only Java is installed. Not Kotlin, no maven/gradle, etc.

Run: kscript --package a.kts on macOS
Vagrant ssh, and then run /vagrant/a

#### Demo 8

Real world example from Timing

Problems with Scripting in Kotlin
