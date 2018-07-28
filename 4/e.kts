#!/usr/bin/env kscript

//DEPS com.beust:klaxon:3.0.1

import com.beust.klaxon.Klaxon
import com.beust.klaxon.Json

data class User(
    val name: String,
    val emailId: String? = null,
    val isAdmin: Boolean = false 
) {
    @Json(ignored=true)
    val lowerCaseName: String get() = name.toLowerCase()
}

val user1 = Klaxon().parse<User>(
    """
    {
      "name": "User 1",
      "emailId": "foo@bar.com",
      "isAdmin": true,
      "lowerCaseName": "NOT LOWER CASE"
    }
    """
)

println(user1)
println(user1!!.lowerCaseName)