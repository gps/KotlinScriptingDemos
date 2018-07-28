#!/usr/bin/env kscript

//DEPS com.xenomachina:kotlin-argparser:2.0.6

import com.xenomachina.argparser.ArgParser

class Args(parser: ArgParser) {
    val someArgument by parser.storing("-s", "--some-argument", help="Some Argument")
    val anInteger by parser.storing("-i", "--an-integer", help="An integer") { this.toInt() }
}

val parsedArgs = ArgParser(args).parseInto(::Args)

println(parsedArgs.someArgument)
println(parsedArgs.anInteger)
