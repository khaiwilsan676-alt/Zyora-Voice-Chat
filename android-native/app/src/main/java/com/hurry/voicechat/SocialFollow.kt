package com.hurry.voicechat

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SocialFollowUser(
    val id:String,
    val uid:String,
    val accountId:String,
    val name:String,
    val image:String,
    val gender:String,
    val country:String,
    val countryFlag:String
)
data class SocialFollowSnapshot(
    val followers:Int=0,
    val followingCount:Int=0,
    val friends:Int=0,
    val isFollowing:Boolean=false
)

object SocialFollowApi {
    private const val API="https://zyora-server.onrender.com"
    private fun enc(v:String)=URLEncoder.encode(v,"UTF-8")
    private fun request(path:String,method:String="GET",body:JSONObject?=null):JSONObject {
        val c=(URL(API+path).openConnection() as HttpURLConnection).apply {
            requestMethod=method; connectTimeout=12000; readTimeout=12000
            if(body!=null){doOutput=true;setRequestProperty("Content-Type","application/json; charset=utf-8")}
        }
        try {
            if(body!=null)c.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
            val code=c.responseCode
            val stream=if(code in 200..299)c.inputStream else c.errorStream
            val raw=stream?.bufferedReader()?.use{it.readText()}.orEmpty()
            if(code !in 200..299) error("Follow API HTTP $code")
            return JSONObject(raw)
        } finally { c.disconnect() }
    }
    suspend fun snapshot(context:Context, userId:String, targetId:String):SocialFollowSnapshot?=withContext(Dispatchers.IO){
        if(userId.isBlank()||targetId.isBlank())return@withContext null
        runCatching {
            val root=request("/api/follows?targetUid=${enc(targetId)}&followerId=${enc(userId)}")
            val snap=SocialFollowSnapshot(root.optInt("followers"),root.optInt("followingCount"),root.optInt("friends"),root.optBoolean("isFollowing",root.optBoolean("following")))
            saveSnapshot(context,targetId,snap)
            snap
        }.getOrNull()
    }
    suspend fun toggle(context:Context,userId:String,targetId:String,follow:Boolean):SocialFollowSnapshot?=withContext(Dispatchers.IO){
        if(userId.isBlank()||targetId.isBlank()||userId==targetId)return@withContext null
        runCatching {
            // The mutation response already contains the authoritative counts/state.
            // Do not issue a second GET here: that doubled latency on every tap,
            // especially when Render was waking from its idle state.
            val root=request("/api/follows","POST",JSONObject().put("followerId",userId).put("targetUid",targetId).put("action",if(follow)"follow" else "unfollow"))
            if (!root.optBoolean("success", false)) error("Follow update was not accepted")
            val snap=SocialFollowSnapshot(
                root.optInt("followers"),
                root.optInt("followingCount"),
                root.optInt("friends"),
                root.optBoolean("isFollowing",root.optBoolean("following",follow))
            )
            saveSnapshot(context,targetId,snap)
            snap
        }.getOrNull()
    }
    fun cachedList(context:Context,userId:String,type:String):List<SocialFollowUser> {
        if(userId.isBlank()) return emptyList()
        val key="social_list:$userId:$type"
        val cached=runCatching{NativeIndexDatabase(context).loadProfile(key)?.optJSONArray("rows")}.getOrNull()
        return parseRows(cached)
    }
    suspend fun list(context:Context,userId:String,type:String):List<SocialFollowUser> = withContext(Dispatchers.IO){
        if(userId.isBlank())return@withContext emptyList()
        val key="social_list:$userId:$type"
        val cachedRows=cachedList(context,userId,type)
        runCatching {
            val root=request("/api/follows?userId=${enc(userId)}&type=${enc(type)}")
            val rows=parseRows(root.optJSONArray("rows"))
            NativeIndexDatabase(context).saveProfile(key,JSONObject().put("rows",root.optJSONArray("rows")?:org.json.JSONArray()).put("updatedAt",System.currentTimeMillis()))
            rows
        }.getOrElse { cachedRows }
    }
    private fun parseRows(array:org.json.JSONArray?):List<SocialFollowUser>{
        if(array==null)return emptyList()
        return (0 until array.length()).mapNotNull{i->array.optJSONObject(i)?.let{p->
            SocialFollowUser(p.optString("id"),p.optString("uid"),p.optString("accountId"),p.optString("name","User"),p.optString("image"),p.optString("gender"),p.optString("country"),p.optString("countryFlag"))
        }}
    }
    fun cacheSnapshot(context:Context,targetId:String,s:SocialFollowSnapshot){
        NativeIndexDatabase(context).saveProfile("social_counts:$targetId",JSONObject().put("followers",s.followers).put("followingCount",s.followingCount).put("friends",s.friends).put("isFollowing",s.isFollowing))
    }
    private fun saveSnapshot(context:Context,targetId:String,s:SocialFollowSnapshot) = cacheSnapshot(context,targetId,s)
    fun cachedSnapshot(context:Context,targetId:String):SocialFollowSnapshot?=runCatching{
        val j=NativeIndexDatabase(context).loadProfile("social_counts:$targetId")?:return null
        SocialFollowSnapshot(j.optInt("followers"),j.optInt("followingCount"),j.optInt("friends"),j.optBoolean("isFollowing"))
    }.getOrNull()
}
