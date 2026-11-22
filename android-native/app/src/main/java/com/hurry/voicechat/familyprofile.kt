package com.hurry.voicechat

import android.net.Uri
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private const val FAMILY_API = "https://zyora-server.onrender.com"
private const val FAMILY_ASSETS = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"
internal data class FamilyMemberRow(val uid:String,val accountId:String,val name:String,val image:String,val gender:String,val coins:Long,val tag:String="",val role:String="member")
internal data class FamilyRecord(val id:String,val name:String,val image:String,val announcement:String,val ownerUid:String,val members:List<FamilyMemberRow>,val totalCoinsSent:Long=0L,val ownerAccountId:String="")

internal object FamilyProfileApi {
    private suspend fun request(path:String, method:String="GET", body:JSONObject?=null):JSONObject = withContext(Dispatchers.IO) {
        val user=FirebaseAuth.getInstance().currentUser ?: error("Please sign in again.")
        val token=Tasks.await(user.getIdToken(false),30,TimeUnit.SECONDS).token ?: error("Authentication token unavailable.")
        val c=(URL("$FAMILY_API$path").openConnection() as HttpURLConnection).apply {
            requestMethod=method; connectTimeout=12000; readTimeout=16000
            setRequestProperty("Authorization","Bearer $token"); setRequestProperty("Content-Type","application/json; charset=utf-8")
            if(body!=null) doOutput=true
        }
        try {
            if(body!=null)c.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
            val status=c.responseCode; val stream=if(status in 200..299)c.inputStream else c.errorStream
            val raw=stream?.bufferedReader()?.use{it.readText()}.orEmpty(); val json=if(raw.isBlank())JSONObject() else JSONObject(raw)
            if(status !in 200..299)error(json.optString("error").ifBlank{"Family request failed ($status)."})
            json
        } finally { c.disconnect() }
    }
    private fun parse(root:JSONObject):FamilyRecord {
        val f=root.optJSONObject("family")?:root; val a=f.optJSONArray("members")?:JSONArray()
        val rows=(0 until a.length()).mapNotNull{i->a.optJSONObject(i)?.let{m->FamilyMemberRow(m.optString("uid",m.optString("id")),m.optString("accountId"),m.optString("name","User"),m.optString("image"),m.optString("gender"),m.optLong("coinsSent").coerceAtLeast(0),m.optString("tag"),m.optString("role","member"))}}.sortedByDescending{it.coins}
        return FamilyRecord(f.optString("familyId"),f.optString("name"),f.optString("image"),f.optString("announcement"),f.optString("ownerUid"),rows,f.optLong("totalCoinsSent",0L),f.optString("ownerAccountId",f.optString("creatorAccountId")))
    }
    suspend fun create(id:String,name:String,image:String,announcement:String)=request("/api/families","POST",JSONObject().put("familyId",id).put("name",name).put("image",image).put("announcement",announcement))
    suspend fun load(id:String)=parse(request("/api/families/${URLEncoder.encode(id,"UTF-8")}"))
    suspend fun memberCount(id:String):Int=parse(request("/api/families/${URLEncoder.encode(id,"UTF-8")}")).members.size
    fun cachedLeaderboard(context: Context):List<FamilyRecord> = runCatching {
        val raw = context.getSharedPreferences("zyora_family_cache", Context.MODE_PRIVATE)
            .getString("leaderboard", "[]") ?: "[]"
        val rows = JSONArray(raw)
        (0 until rows.length()).mapNotNull { i ->
            rows.optJSONObject(i)?.let { parse(JSONObject().put("family", it)) }
        }
    }.getOrDefault(emptyList())

    fun saveLeaderboardCache(context: Context, families: List<FamilyRecord>) {
        runCatching {
            val rows = JSONArray()
            families.forEach { family ->
                val members = JSONArray()
                family.members.forEach { member ->
                    members.put(JSONObject()
                        .put("uid", member.uid).put("accountId", member.accountId)
                        .put("name", member.name).put("image", member.image)
                        .put("gender", member.gender).put("coinsSent", member.coins)
                        .put("tag", member.tag).put("role", member.role))
                }
                rows.put(JSONObject()
                    .put("familyId", family.id).put("name", family.name)
                    .put("image", family.image).put("announcement", family.announcement)
                    .put("ownerUid", family.ownerUid).put("ownerAccountId", family.ownerAccountId)
                    .put("totalCoinsSent", family.totalCoinsSent).put("members", members))
            }
            context.getSharedPreferences("zyora_family_cache", Context.MODE_PRIVATE)
                .edit().putString("leaderboard", rows.toString()).apply()
        }
    }

    suspend fun leaderboard():List<FamilyRecord> { val root=request("/api/families/leaderboard"); val rows=root.optJSONArray("families")?:JSONArray(); return (0 until rows.length()).mapNotNull { i -> rows.optJSONObject(i)?.let { parse(JSONObject().put("family",it)) } } }
    suspend fun update(id:String,n:String,i:String,a:String)=request("/api/families/${URLEncoder.encode(id,"UTF-8")}","PUT",JSONObject().put("name",n).put("image",i).put("announcement",a))
    suspend fun accept(id:String,context:Context):JSONObject {
        val result=request("/api/families/${URLEncoder.encode(id,"UTF-8")}/accept","POST",JSONObject())
        val family=result.optJSONObject("family")
        if(family!=null){
            context.getSharedPreferences("hurry_user",Context.MODE_PRIVATE).edit()
                .putBoolean("familyCreated",true).putString("familyCode",id)
                .putString("familyName",family.optString("name"))
                .putString("familyImage",family.optString("image"))
                .putString("familyAnnouncement",family.optString("announcement"))
                .putString("familyCreatorId",family.optString("ownerAccountId"))
                .apply()
        }
        return result
    }
    suspend fun invite(id:String,target:String)=request("/api/families/${URLEncoder.encode(id,"UTF-8")}/invite","POST",JSONObject().put("targetId",target))
    suspend fun apply(id:String)=request("/api/families/${URLEncoder.encode(id,"UTF-8")}/apply","POST",JSONObject())
    suspend fun applications(id:String):List<FamilyMemberRow>{
        val r=request("/api/families/${URLEncoder.encode(id,"UTF-8")}/applications")
        val a=r.optJSONArray("applications")?:JSONArray()
        return (0 until a.length()).mapNotNull{x->a.optJSONObject(x)?.let{m->FamilyMemberRow(m.optString("uid"),m.optString("accountId"),m.optString("name","User"),m.optString("image"),m.optString("gender"),0,m.optString("tag"))}}
    }
    suspend fun approve(id:String,uid:String)=request("/api/families/${URLEncoder.encode(id,"UTF-8")}/applications/${URLEncoder.encode(uid,"UTF-8")}/approve","POST",JSONObject())
    suspend fun setAdmin(id:String,uid:String)=request("/api/families/${URLEncoder.encode(id,"UTF-8")}/members/${URLEncoder.encode(uid,"UTF-8")}/admin","POST",JSONObject())
    suspend fun removeAdmin(id:String,uid:String)=request("/api/families/${URLEncoder.encode(id,"UTF-8")}/members/${URLEncoder.encode(uid,"UTF-8")}/remove-admin","POST",JSONObject())
    suspend fun removeMember(id:String,uid:String)=request("/api/families/${URLEncoder.encode(id,"UTF-8")}/members/${URLEncoder.encode(uid,"UTF-8")}/leave","POST",JSONObject())
    suspend fun search(id:String,q:String):List<FamilyMemberRow>{
        val r=request("/api/families/${URLEncoder.encode(id,"UTF-8")}/search-users?q=${URLEncoder.encode(q,"UTF-8")}")
        val a=r.optJSONArray("users")?:JSONArray()
        return (0 until a.length()).mapNotNull{x->a.optJSONObject(x)?.let{m->FamilyMemberRow(m.optString("uid"),m.optString("accountId"),m.optString("name","User"),m.optString("image"),m.optString("gender"),0)}}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyProfile(familyId:String,onBack:()->Unit,onOpenChat:(String,String,String)->Unit={_,_,_->}) {
    val context = LocalContext.current
    val scope=rememberCoroutineScope(); val auth=FirebaseAuth.getInstance().currentUser
    var family by remember(familyId){mutableStateOf<FamilyRecord?>(null)}
    var loading by remember{mutableStateOf(true)}; var error by remember{mutableStateOf("")}
    var inviteSheet by remember{mutableStateOf(false)}; var editDialog by remember{mutableStateOf(false)}; var savingEdit by remember{mutableStateOf(false)}
    var query by remember{mutableStateOf("")}; var results by remember{mutableStateOf<List<FamilyMemberRow>>(emptyList())}; var applications by remember{mutableStateOf<List<FamilyMemberRow>>(emptyList())}; var applicationSent by remember(familyId){mutableStateOf(false)}
    var searching by remember{mutableStateOf(false)}; var busy by remember{mutableStateOf("")}; var selectedMember by remember{mutableStateOf<FamilyMemberRow?>(null)}
    val isOwner=family?.ownerUid==auth?.uid
    val currentMember=family?.members?.firstOrNull{it.uid==auth?.uid}
    val isAdmin=currentMember?.role=="admin"
    val isMember=family?.members?.any{it.uid==auth?.uid}==true
    fun reload(){scope.launch{loading=true;runCatching{FamilyProfileApi.load(familyId)}.onSuccess{family=it;error=""}.onFailure{error=it.message?:"Could not load Family."};if(isOwner||isAdmin)runCatching{FamilyProfileApi.applications(familyId)}.onSuccess{applications=it};loading=false}}
    LaunchedEffect(familyId){reload()}
    LaunchedEffect(inviteSheet,isOwner,isAdmin){if(inviteSheet&&(isOwner||isAdmin))runCatching{FamilyProfileApi.applications(familyId)}.onSuccess{applications=it}}
    Box(Modifier.fillMaxSize().background(Color(0xFFF1F2F4))){
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=82.dp)){
            item{
                Box(Modifier.fillMaxWidth().height(250.dp)){
                    AsyncImage(model=FAMILY_ASSETS+"file_00000000c89881faa6c31b7143cce21a.png",contentDescription="Family background",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x55000000),.90f to Color(0xAA777777),1f to Color(0xFFF1F2F4))))
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically){
                        IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back",tint=Color.White,modifier=Modifier.size(27.dp))}
                        Spacer(Modifier.weight(1f))
                        if(isOwner)IconButton(onClick={editDialog=true}){LucidePublicProfileIcon(warning=false,modifier=Modifier.size(22.dp),tint=Color.White)}
                    }
                    Row(Modifier.align(Alignment.BottomStart).padding(start=16.dp,bottom=27.dp),verticalAlignment=Alignment.CenterVertically){
                        AsyncImage(model=family?.image?.takeIf{it.isNotBlank()}?:FAMILY_ASSETS+"IMG_20261116_162146.png",contentDescription="Family DP",modifier=Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
                        Column(Modifier.weight(1f).padding(start=12.dp),verticalArrangement=Arrangement.spacedBy(0.dp)){
                            Text(family?.name?:"Family",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold,maxLines=1)
                            Text("Family ID: ${family?.ownerAccountId?.ifBlank { family?.members?.firstOrNull { it.uid == family?.ownerUid }?.accountId.orEmpty() }.orEmpty().ifBlank { "—" }}",color=Color.White,fontSize=12.sp)
                            Row(Modifier.padding(top=4.dp), verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                AsyncImage(model=FAMILY_ASSETS+"IMG_20261116_162146.png",contentDescription="Members",modifier=Modifier.size(15.dp))
                                Text("${family?.members?.size?:0}/50",color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item{
                Column(Modifier.fillMaxWidth().padding(horizontal=12.dp).offset(y=(-8).dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp)){
                    Text("Announcement",color=Color(0xFF222222),fontWeight=FontWeight.Bold,fontSize=15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(family?.announcement?.ifBlank{"No announcement yet"}?:"",color=Color(0xFF666666),fontSize=13.sp,lineHeight=19.sp)
                }
            }
            item{Text("Family Members",color=Color(0xFF222222),fontWeight=FontWeight.Bold,fontSize=16.sp,modifier=Modifier.padding(start=14.dp,top=12.dp,bottom=8.dp))}
            if(loading)item{Box(Modifier.fillMaxWidth().padding(24.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()}}
            else if(family?.members.isNullOrEmpty())item{Text(error.ifBlank{"No family members yet."},color=Color.Gray,modifier=Modifier.padding(16.dp))}
            else itemsIndexed(family!!.members){index,m->
                if(index<3){
                    val bg=when(index){0->Brush.horizontalGradient(listOf(Color(0xFFC83A32),Color(0xFFFFD65A)));1->Brush.horizontalGradient(listOf(Color(0xFF245EAA),Color(0xFF4B9BEB)));else->Brush.horizontalGradient(listOf(Color(0xFF68402F),Color(0xFF9B6849)))}
                    Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=3.dp).clip(RoundedCornerShape(12.dp)).background(bg).padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                        Text("TOP ${index+1}",color=Color.White,fontWeight=FontWeight.Black,fontSize=12.sp,modifier=Modifier.width(52.dp))
                        Box(Modifier.size(52.dp)){
                            AsyncImage(model=m.image.ifBlank{FAMILY_ASSETS+"IMG_20261116_162146.png"},contentDescription=m.name,modifier=Modifier.size(40.dp).align(Alignment.Center).clip(CircleShape).clickable { if(m.uid==auth?.uid || isOwner || isAdmin) selectedMember=m },contentScale=ContentScale.Crop)
                            AsyncImage(model=FAMILY_ASSETS+when(index){0->"IMG_20261115_135339.png";1->"IMG_20261115_135356.png";else->"IMG_20261115_135419.png"},contentDescription="Rank frame",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
                            if(m.role=="admin") Box(Modifier.align(Alignment.BottomCenter).offset(y=2.dp).width(32.dp).height(12.dp).clip(RoundedCornerShape(50)).background(Color(0xFF2588F5)).border(1.dp,Color.White,RoundedCornerShape(50)),contentAlignment=Alignment.Center){Text("Admin",modifier=Modifier.fillMaxWidth(),color=Color.White,fontSize=6.sp,lineHeight=8.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)}
                        }
                        Column(Modifier.weight(1f).padding(start=8.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Text(m.name,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=1);val female=m.gender.equals("female",true);Box(Modifier.size(17.dp).clip(CircleShape).background(if(female)Color(0xFFFF5874) else Color(0xFF3B82F6)),contentAlignment=Alignment.Center){CustomGenderIcon(isFemale=female,modifier=Modifier.size(9.dp))}};Text(listOf(m.accountId,m.tag).filter{it.isNotBlank() && !it.equals("true",true) && !it.equals("false",true)}.joinToString(" · "),color=Color.White,fontSize=11.sp)}
                        Column(horizontalAlignment=Alignment.End){AsyncImage(model=FAMILY_ASSETS+"file_00000000e56882119c217d508b6733dc.png",contentDescription="Coins",modifier=Modifier.size(17.dp));Text(if(isOwner||isAdmin)m.coins.toString() else "******",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)}
                    }
                }else{
                    Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=2.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(9.dp),verticalAlignment=Alignment.CenterVertically){
                        Text("${index+1}",color=Color.Gray,fontSize=13.sp,modifier=Modifier.width(28.dp))
                        Box(Modifier.size(44.dp)){
                            AsyncImage(model=m.image.ifBlank{FAMILY_ASSETS+"IMG_20261116_162146.png"},contentDescription=m.name,modifier=Modifier.size(38.dp).align(Alignment.Center).clip(CircleShape).clickable { if(m.uid==auth?.uid || isOwner || isAdmin) selectedMember=m },contentScale=ContentScale.Crop)
                            if(m.role=="admin") Box(Modifier.align(Alignment.BottomCenter).offset(y=1.dp).width(32.dp).height(12.dp).clip(RoundedCornerShape(50)).background(Color(0xFF2588F5)).border(1.dp,Color.White,RoundedCornerShape(50)),contentAlignment=Alignment.Center){Text("Admin",modifier=Modifier.fillMaxWidth(),color=Color.White,fontSize=6.sp,lineHeight=8.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)}
                        }
                        Column(Modifier.weight(1f).padding(start=9.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Text(m.name,color=Color(0xFF222222),fontSize=13.sp,fontWeight=FontWeight.SemiBold);val female=m.gender.equals("female",true);Box(Modifier.size(17.dp).clip(CircleShape).background(if(female)Color(0xFFFF5874) else Color(0xFF3B82F6)),contentAlignment=Alignment.Center){CustomGenderIcon(isFemale=female,modifier=Modifier.size(9.dp))}};Text(listOf(m.accountId,m.tag).filter{it.isNotBlank() && !it.equals("true",true) && !it.equals("false",true)}.joinToString(" · "),color=Color.Gray,fontSize=10.sp)}
                        Column(horizontalAlignment=Alignment.End){AsyncImage(model=FAMILY_ASSETS+"file_00000000e56882119c217d508b6733dc.png",contentDescription="Coins",modifier=Modifier.size(15.dp));Text(if(isOwner||isAdmin)m.coins.toString() else "******",color=Color.DarkGray,fontSize=11.sp,fontWeight=FontWeight.Bold)}
                    }
                }
            }
            if(error.isNotBlank()&&!loading)item{Text(error,color=Color(0xFFB3261E),fontSize=12.sp,modifier=Modifier.padding(12.dp))}
        }
        if(family!=null && (isOwner || isAdmin || !isMember))Button(onClick={if(isOwner||isAdmin)inviteSheet=true else scope.launch{runCatching{FamilyProfileApi.apply(familyId)}.onSuccess{applicationSent=true;error="Application sent. Waiting for family owner approval."}.onFailure{error=it.message?:"Could not apply."}}},enabled=(isOwner||isAdmin)||!applicationSent,modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp).height(50.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2878E8))){
            Text(if(isOwner||isAdmin)"Add Members" else if(applicationSent)"Applied" else "Apply",color=Color.White,fontWeight=FontWeight.Bold)
        }
    }
    if(inviteSheet)ModalBottomSheet(onDismissRequest={inviteSheet=false},modifier=Modifier.fillMaxHeight(.70f),shape=RoundedCornerShape(topStart=12.dp,topEnd=12.dp),containerColor=Color.White,dragHandle=null){
        Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
            Box(Modifier.fillMaxWidth().height(48.dp)){
                IconButton(onClick={inviteSheet=false},modifier=Modifier.align(Alignment.CenterStart)){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back",tint=Color.Black)}
                Text("Invite Members",color=Color.Black,fontSize=17.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.Center))
            }
            OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(12.dp),placeholder={Text("Search by ID or name")},leadingIcon={Icon(Icons.Default.Search,null)},colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Color(0xFFF1F2F4),focusedContainerColor=Color(0xFFF1F2F4),unfocusedBorderColor=Color.Transparent,focusedBorderColor=Color.Transparent))
            if((isOwner || isAdmin) && applications.isNotEmpty()){
                Text("Family Applications",color=Color(0xFF222222),fontWeight=FontWeight.Bold,fontSize=14.sp,modifier=Modifier.padding(top=12.dp,bottom=4.dp))
                Column(Modifier.heightIn(max=160.dp).verticalScroll(rememberScrollState())){
                    applications.forEach { applicant ->
                        Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){
                            AsyncImage(model=applicant.image,contentDescription=null,modifier=Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)),contentScale=ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(start=9.dp)){Text(applicant.name,color=Color.Black,fontWeight=FontWeight.SemiBold,fontSize=13.sp);Text(listOf(applicant.accountId,applicant.gender,applicant.tag).filter{it.isNotBlank()}.joinToString(" · "),color=Color.Gray,fontSize=10.sp)}
                            Button(enabled=busy!=applicant.uid,onClick={scope.launch{busy=applicant.uid;runCatching{FamilyProfileApi.approve(familyId,applicant.uid)}.onSuccess{applications=applications.filterNot{it.uid==applicant.uid};reload()}.onFailure{error=it.message?:"Approval failed."};busy=""}},shape=RoundedCornerShape(8.dp),contentPadding=PaddingValues(horizontal=12.dp,vertical=5.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2878E8))){Text(if(busy==applicant.uid)"..." else "Apply",fontSize=12.sp,color=Color.White)}
                        }
                    }
                }
            }
            LaunchedEffect(query){if(query.isBlank())results=emptyList()else{delay(300);searching=true;runCatching{FamilyProfileApi.search(familyId,query)}.onSuccess{results=it};searching=false}}
            if(searching)LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.fillMaxSize()){itemsIndexed(results){_,m->
                Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
                    AsyncImage(model=m.image,contentDescription=null,modifier=Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop)
                    Column(Modifier.weight(1f).padding(start=10.dp)){Text(m.name,color=Color.Black,fontWeight=FontWeight.SemiBold);Text("${m.accountId} · ${m.gender}",color=Color.Gray,fontSize=11.sp)}
                    Button(enabled=busy!=m.uid,onClick={scope.launch{busy=m.uid;runCatching{FamilyProfileApi.invite(familyId,m.accountId.ifBlank{m.uid})}.onSuccess{error="Invitation sent to ${m.name}."}.onFailure{error=it.message?:"Invitation failed."};busy=""}},shape=RoundedCornerShape(8.dp),contentPadding=PaddingValues(horizontal=12.dp,vertical=5.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2878E8))){Text(if(busy==m.uid)"..." else "Invite",fontSize=12.sp,color=Color.White)}
                };HorizontalDivider(color=Color(0xFFECECEC))
            }}
        }
    }
    selectedMember?.let { member ->
        val canPromote=isOwner && member.role!="admin" && member.uid!=auth?.uid
        val canDemote=isOwner && member.role=="admin" && member.uid!=auth?.uid
        val canRemove=(isOwner || isAdmin) && member.uid!=family?.ownerUid
        val canSelfLeave=member.uid==auth?.uid && member.uid!=family?.ownerUid
        ModalBottomSheet(onDismissRequest={selectedMember=null},shape=RoundedCornerShape(topStart=12.dp,topEnd=12.dp),containerColor=Color.White,dragHandle=null){
            Column(Modifier.fillMaxWidth().padding(horizontal=20.dp).padding(top=16.dp,bottom=24.dp)){
                if(canPromote) TextButton(onClick={scope.launch{busy=member.uid;runCatching{FamilyProfileApi.setAdmin(familyId,member.uid)}.onSuccess{selectedMember=null;reload()}.onFailure{error=it.message?:"Could not add Admin."};busy=""}},modifier=Modifier.fillMaxWidth(),enabled=busy!=member.uid){Text("Add Admin",color=Color.Black,fontSize=15.sp)}
                if(canDemote) TextButton(onClick={scope.launch{busy=member.uid;runCatching{FamilyProfileApi.removeAdmin(familyId,member.uid)}.onSuccess{selectedMember=null;reload()}.onFailure{error=it.message?:"Could not remove Admin."};busy=""}},modifier=Modifier.fillMaxWidth(),enabled=busy!=member.uid){Text("Remove Admin",color=Color.Black,fontSize=15.sp)}
                if(canRemove || canSelfLeave) TextButton(onClick={scope.launch{busy=member.uid;runCatching{FamilyProfileApi.removeMember(familyId,member.uid)}.onSuccess{selectedMember=null;if(canSelfLeave){context.getSharedPreferences("hurry_user",Context.MODE_PRIVATE).edit().remove("familyCreated").remove("familyCode").remove("familyName").remove("familyImage").remove("familyAnnouncement").remove("familyCreatorId").apply();onBack()}else reload()}.onFailure{error=it.message?:"Could not remove member."};busy=""}},modifier=Modifier.fillMaxWidth(),enabled=busy!=member.uid){Text(if (canSelfLeave) "Leave Family" else "Remove Member",color=Color.Black,fontSize=15.sp)}
            }
        }
    }
    if(editDialog&&family!=null)FamilyEditDialog(family!!,busy=savingEdit,error=error,onDismiss={editDialog=false}){n,i,a->scope.launch{
        savingEdit=true
        runCatching {
            val hostedImage = if (i.startsWith("content://")) {
                val data = ProfilePersistence.imageDataUri(context, Uri.parse(i)) ?: throw IllegalStateException("Could not read Family DP.")
                ProfilePersistence.uploadImageToMongoDB(data, "$familyId-family", "family-dp") ?: throw IllegalStateException("Family DP upload failed.")
            } else i
            FamilyProfileApi.update(familyId,n,hostedImage,a)
            hostedImage
        }.onSuccess{hostedImage->context.getSharedPreferences("hurry_user",Context.MODE_PRIVATE).edit().putString("familyName",n).putString("familyImage",hostedImage).putString("familyAnnouncement",a).apply();savingEdit=false;editDialog=false;reload()}.onFailure{error=it.message?:"Could not update Family.";savingEdit=false}
    }}
}

@Composable
private fun FamilyEditDialog(family:FamilyRecord,busy:Boolean,error:String,onDismiss:()->Unit,onSave:(String,String,String)->Unit){
    val context=LocalContext.current
    var name by remember(family.id){mutableStateOf(family.name)}
    var image by remember(family.id){mutableStateOf(family.image)}
    var announcement by remember(family.id){mutableStateOf(family.announcement)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->
        if(uri!=null){
            runCatching{context.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            image=uri.toString()
        }
    }
    Dialog(onDismissRequest={if(!busy)onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
        FamilyCreate(
            onBack={if(!busy)onDismiss()},
            name=name,onName={name=it.take(40)},
            announcement=announcement,onAnnouncement={announcement=it.take(500)},
            image=image,onImage={picker.launch(arrayOf("image/*"))},
            busy=busy,error=error.takeIf{it.isNotBlank()},
            onCreate={if(!busy&&name.isNotBlank())onSave(name,image,announcement)},
            editMode=true
        )
    }
}
