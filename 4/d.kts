#!/usr/bin/env kscript

//DEPS com.beust:klaxon:3.0.1

import com.beust.klaxon.Klaxon
import com.beust.klaxon.Json

data class User(
    val name: String,
    @Json(name="email_id")
    val emailId: String? = null,
    @Json(name="is_admin")
    val isAdmin: Boolean = false
)

val user1 = Klaxon().parse<User>(
    """
    {
      "name": "User 1",
      "email_id": "foo@bar.com",
      "is_admin": true
    }
    """
)

println(user1)