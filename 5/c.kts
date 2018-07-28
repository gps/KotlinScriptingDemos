#!/usr/bin/env kscript

//DEPS com.github.kittinunf.fuel:fuel:1.14.0
//DEPS com.beust:klaxon:3.0.1

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.Fuel

data class User(
    val name: String,
    val emailId: String? = null,
    val isAdmin: Boolean = false
)

val user1 = User("User 1", "foo@bar.com")

val klaxon = Klaxon()
val parser = Parser()

val res = Fuel.post("https://httpbin.org/post")
    .header(mapOf("Content-Type" to "application/json"))
    .body(klaxon.toJsonString(user1))
    .response()
println(res.second.statusCode)

val responseJson = parser.parse(res.third.component1()?.inputStream()!!) as JsonObject
val parsedUser = klaxon.maybeParse<User>(responseJson.obj("json")!!)
println(parsedUser)
