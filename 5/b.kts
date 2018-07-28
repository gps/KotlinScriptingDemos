#!/usr/bin/env kscript

//DEPS com.github.kittinunf.fuel:fuel:1.14.0

import com.github.kittinunf.fuel.Fuel

val res = Fuel.post("https://httpbin.org/post")
    .header(mapOf("Content-Type" to "application/json"))
    .body(
        """
        {
            "someKey": "someValue"
        }
        """
    )
    .responseString().second

println(res.statusCode)
println(String(res.data))
