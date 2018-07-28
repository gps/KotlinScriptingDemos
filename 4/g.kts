#!/usr/bin/env kscript

//DEPS com.beust:klaxon:3.0.1

import com.beust.klaxon.Klaxon
import com.beust.klaxon.Json

data class User(
    val name: String,
    val emailId: String? = null,
    val isAdmin: Boolean = false
)

val klaxon = Klaxon()

val user1 = User("User 1", "foo@bar.com")

println("${klaxon.toJsonString(user1)}")
