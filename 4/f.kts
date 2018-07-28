#!/usr/bin/env kscript

//DEPS com.beust:klaxon:3.0.1

import com.beust.klaxon.Klaxon
import com.beust.klaxon.Json

data class User(
    val name: String,
    val emailId: String? = null,
    val isAdmin: Boolean = false,
    @Json(ignored=false)
    private val lowerCaseName: String
)

val user1 = Klaxon().parse<User>(
    """
    {
      "name": "User 1",
      "emailId": "foo@bar.com",
      "isAdmin": true,
      "lowerCaseName": "user 1"
    }
    """
)

println(user1)