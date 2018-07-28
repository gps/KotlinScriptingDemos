#!/usr/bin/env kscript

//DEPS com.beust:klaxon:3.0.1

import com.beust.klaxon.Klaxon

data class User(
    val name: String,
    val emailId: String? = null,
    val isAdmin: Boolean = false
)

val user1 = Klaxon().parse<User>(
    """
    {
      "name": "User 1",
      "isAdmin": true
    }
    """
)

println(user1)