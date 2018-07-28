#!/usr/bin/env kscript

//DEPS com.beust:klaxon:3.0.1

import com.beust.klaxon.json

val jsonObject = json {
    obj(
        "users" to array(
            obj(
                "name" to "User 1",
                "emailId" to "user1@email.com"
            ),
            obj(
                "name" to "User 2",
                "emailId" to "user2@email.com"
            )
        )
    )
}

println(jsonObject.toJsonString(true))
