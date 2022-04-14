package com.pydio.android.cells.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pydio.cells.api.ServerURL
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import java.lang.reflect.Type
import java.util.*

class Converters {

    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType: Type = object : TypeToken<ArrayList<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun toProperties(value: String): Properties {
        val propType: Type = object : TypeToken<Properties>() {}.type
        return Gson().fromJson(value, propType)
    }

    @TypeConverter
    fun fromProperties(meta: Properties): String {
        return Gson().toJson(meta)
    }

    @TypeConverter
    fun toStateID(value: String): StateID {
        val propType: Type = object : TypeToken<StateID>() {}.type
        return Gson().fromJson(value, propType)
    }

    @TypeConverter
    fun fromStateID(stateID: StateID): String {
        return Gson().toJson(stateID)
    }

    @TypeConverter
    fun fromServerURL(url: ServerURL): String {
        return url.toJson()
    }

    @TypeConverter
    fun toServerURL(value: String): ServerURL {
        return ServerURLImpl.fromJson(value)
    }
}
