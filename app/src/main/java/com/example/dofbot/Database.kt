package com.example.dofbot

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Database(
    context: Context,
    name: String = database,
    factory: SQLiteDatabase.CursorFactory? = null,
    version: Int = v
) : SQLiteOpenHelper(context, name, factory, version){
    companion object{
        private const val database = "TOKEN"  //資料庫名稱
        private const val v = 1 //資料庫版本

    }

    override fun onCreate(p0: SQLiteDatabase) {
        //建立存放email跟token的資料庫
        p0.execSQL("CREATE TABLE tokenList(email text PRIMARY KEY, token text NOT NULL)")//", name text NOT NULL, id text NOT NULL)")
    }

    override fun onUpgrade(p0: SQLiteDatabase, p1: Int, p2: Int) {
        p0.execSQL("DROP TABLE IF EXISTS myTable")
        onCreate(p0)
    }
}