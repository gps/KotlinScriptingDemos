#!/usr/bin/env kscript

//DEPS com.github.kittinunf.fuel:fuel:1.14.0

import com.github.kittinunf.fuel.Fuel

val res = Fuel.get("https://httpbin.org/get?key=value").responseString().second
println(res.statusCode)
println(String(res.data))
