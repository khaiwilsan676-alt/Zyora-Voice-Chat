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
internal data class FamilyMemberRow(val uid:String,val accountId:String,val name:String,val image:String,val gender:String,val coins:Long)
internal data class FamilyRecord(val id:String,val name:String,val image:String,val announcement:String,val ownerUid:String,val members:List<FamilyMemberRow>,val totalCoinsSent:Long=0L)

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
        val rows=(0 until a.length()).mapNotNull{i->a.optJSONObject(i)?.let{m->FamilyMemberRow(m.optString("uid",m.optString("id")),m.optString("accountId"),m.optString("name","User"),m.optString("image"),m.optString("gender"),m.optLong("coinsSent").coerceAtLeast(0))}}.sortedByDescending{it.coins}
        return FamilyRecord(f.optString("familyId"),f.optString("name"),f.optString("image"),f.optString("announcement"),f.optString("ownerUid"),rows,f.optLong("totalCoinsSent",0L))
    }
    suspend fun create(id:String,name:String,image:String,announcement:String)=request("/api/families","POST",JSONObject().put("familyId",id).put("name",name).put("image",image).put("announcement",announcement))
    suspend fun load(id:String)=parse(request("/api/families/${URLEncoder.encode(id,"UTF-8")}"))
    suspend fun memberCount(id:String):Int=parse(request("/api/families/${URLEncoder.encode(id,"UTF-8")}")).members.size
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
    suspend fun search(id:String,q:String):List<FamilyMemberRow>{
        val r=request("/api/families/${URLEncoder.encode(id,"UTF-8")}/search-users?q=${URLEncoder.encode(q,"UTF-8")}")
        val a=r.optJSONArray("users")?:JSONArray()
        return (0 until a.length()).mapNotNull{x->a.optJSONObject(x)?.let{m->FamilyMemberRow(m.optString("uid"),m.optString("accountId"),m.optString("name","User"),m.optString("image"),m.optString("gender"),0)}}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyProfile(familyId:String,onBack:()->Unit,onOpenChat:(String,String,String)->Unit={_,_,_->}) {
    val scope=rememberCoroutineScope(); val auth=FirebaseAuth.getInstance().currentUser
    val context=LocalContext.current
    var family by remember(familyId){mutableStateOf<FamilyRecord?>(null)}
    var loading by remember{mutableStateOf(true)}; var error by remember{mutableStateOf("")}
    var inviteSheet by remember{mutableStateOf(false)}; var editDialog by remember{mutableStateOf(false)}
    var query by remember{mutableStateOf("")}; var results by remember{mutableStateOf<List<FamilyMemberRow>>(emptyList())}
    var searching by remember{mutableStateOf(false)}; var busy by remember{mutableStateOf("")}
    val isOwner=family?.ownerUid==auth?.uid
    fun reload(){scope.launch{loading=true;runCatching{FamilyProfileApi.load(familyId)}.onSuccess{family=it;error=""}.onFailure{error=it.message?:"Could not load Family."};loading=false}}
    LaunchedEffect(familyId){reload()}
    Box(Modifier.fillMaxSize().background(Color(0xFFF1F2F4))){
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=82.dp)){
            item{
                Box(Modifier.fillMaxWidth().height(250.dp)){
                    AsyncImage(model=FAMILY_ASSETS+"file_00000000c89881faa6c31b7143cce21a.png",contentDescription="Family background",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x55000000),.90f to Color(0xAA777777),1f to Color(0xFFF1F2F4))))
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically){
                        IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back",tint=Color.White,modifier=Modifier.size(27.dp))}
                        Spacer(Modifier.weight(1f))
                        if(isOwner)IconButton(onClick={editDialog=true}){Icon(Icons.Default.Edit,"Edit Family",tint=Color.White)}
                    }
                    Row(Modifier.align(Alignment.BottomStart).padding(start=16.dp,bottom=27.dp),verticalAlignment=Alignment.CenterVertically){
                        AsyncImage(model=family?.image?.takeIf{it.isNotBlank()}?:FAMILY_ASSETS+"IMG_20261116_162146.png",contentDescription="Family DP",modifier=Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
                        Column(Modifier.weight(1f).padding(start=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text(family?.name?:"Family",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold,maxLines=1)
                            Text("Family ID: ${family?.id?:familyId}",color=Color.White,fontSize=12.sp)
                            Row(verticalAlignment=Alignment.CenterVertically){
                                AsyncImage(model=FAMILY_ASSETS+"IMG_20261116_162146.png",contentDescription="Members",modifier=Modifier.size(16.dp))
                                Text(" ${family?.members?.size?:0}/50",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)
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
                            AsyncImage(model=m.image.ifBlank{FAMILY_ASSETS+"IMG_20261116_162146.png"},contentDescription=m.name,modifier=Modifier.size(42.dp).align(Alignment.Center).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
                            AsyncImage(model=FAMILY_ASSETS+when(index){0->"IMG_20261115_135339.png";1->"IMG_20261115_135356.png";else->"IMG_20261115_135419.png"},contentDescription="Rank frame",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
                        }
                        Column(Modifier.weight(1f).padding(start=8.dp)){Text(m.name,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=1);Text("${m.accountId} · ${m.gender}",color=Color.White,fontSize=11.sp)}
                        Column(horizontalAlignment=Alignment.End){AsyncImage(model=FAMILY_ASSETS+"file_00000000e56882119c217d508b6733dc.png",contentDescription="Coins",modifier=Modifier.size(17.dp));Text("******",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)}
                    }
                }else{
                    Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=2.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(9.dp),verticalAlignment=Alignment.CenterVertically){
                        Text("${index+1}",color=Color.Gray,fontSize=13.sp,modifier=Modifier.width(28.dp))
                        AsyncImage(model=m.image.ifBlank{FAMILY_ASSETS+"IMG_20261116_162146.png"},contentDescription=m.name,modifier=Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop)
                        Column(Modifier.weight(1f).padding(start=9.dp)){Text(m.name,color=Color(0xFF222222),fontSize=13.sp,fontWeight=FontWeight.SemiBold);Text("${m.accountId} · ${m.gender}",color=Color.Gray,fontSize=10.sp)}
                        Column(horizontalAlignment=Alignment.End){AsyncImage(model=FAMILY_ASSETS+"file_00000000e56882119c217d508b6733dc.png",contentDescription="Coins",modifier=Modifier.size(15.dp));Text("******",color=Color.DarkGray,fontSize=11.sp,fontWeight=FontWeight.Bold)}
                    }
                }
            }
            if(error.isNotBlank()&&!loading)item{Text(error,color=Color(0xFFB3261E),fontSize=12.sp,modifier=Modifier.padding(12.dp))}
        }
        if(family!=null)Button(onClick={if(isOwner)inviteSheet=true else scope.launch{runCatching{FamilyProfileApi.accept(familyId,context)}.onSuccess{reload()}.onFailure{error=it.message?:"Could not apply."}}},modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp).height(50.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2878E8))){
            Text(if(isOwner)"Add Members" else "Apply",color=Color.White,fontWeight=FontWeight.Bold)
        }
    }
    if(inviteSheet)ModalBottomSheet(onDismissRequest={inviteSheet=false},modifier=Modifier.fillMaxHeight(.70f),shape=RoundedCornerShape(topStart=12.dp,topEnd=12.dp),containerColor=Color.White){
        Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={inviteSheet=false}){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back",tint=Color.Black)};Text("Invite Member",color=Color.Black,fontSize=17.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Spacer(Modifier.width(44.dp))}
            OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(12.dp),placeholder={Text("Search by ID or name")},leadingIcon={Icon(Icons.Default.Search,null)},colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Color(0xFFF1F2F4),focusedContainerColor=Color(0xFFF1F2F4),unfocusedBorderColor=Color.Transparent,focusedBorderColor=Color.Transparent))
            LaunchedEffect(query){if(query.isBlank())results=emptyList()else{delay(300);searching=true;runCatching{FamilyProfileApi.search(familyId,query)}.onSuccess{results=it};searching=false}}
            if(searching)LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.fillMaxSize()){itemsIndexed(results){_,m->
                Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
                    AsyncImage(model=m.image,contentDescription=null,modifier=Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop)
                    Column(Modifier.weight(1f).padding(start=10.dp)){Text(m.name,color=Color.Black,fontWeight=FontWeight.SemiBold);Text("${m.accountId} · ${m.gender}",color=Color.Gray,fontSize=11.sp)}
                    TextButton(enabled=busy!=m.uid,onClick={scope.launch{busy=m.uid;runCatching{FamilyProfileApi.invite(familyId,m.accountId.ifBlank{m.uid})}.onSuccess{error="Invitation sent to ${m.name}."}.onFailure{error=it.message?:"Invitation failed."};busy=""}}){Text(if(busy==m.uid)"Sending..." else "Invite")}
                };HorizontalDivider(color=Color(0xFFECECEC))
            }}
        }
    }
    if(editDialog&&family!=null)FamilyEditDialog(family!!,{editDialog=false}){n,i,a->scope.launch{
        runCatching {
            val hostedImage = if (i.startsWith("content://")) {
                val data = ProfilePersistence.imageDataUri(context, Uri.parse(i)) ?: error("Could not read Family DP.")
                ProfilePersistence.uploadImageToMongoDB(data, "$familyId-family", "family-dp") ?: error("Family DP upload failed.")
            } else i
            FamilyProfileApi.update(familyId,n,hostedImage,a)
            hostedImage
        }.onSuccess{hostedImage->context.getSharedPreferences("hurry_user",Context.MODE_PRIVATE).edit().putString("familyName",n).putString("familyImage",hostedImage).putString("familyAnnouncement",a).apply();editDialog=false;reload()}.onFailure{error=it.message?:"Could not update Family."}
    }}
}

@Composable
private fun FamilyEditDialog(family:FamilyRecord,onDismiss:()->Unit,onSave:(String,String,String)->Unit){
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
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Column(Modifier.fillMaxSize().background(Color(0xFFF9F9F9))){
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start=4.dp,end=16.dp,top=8.dp),verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick=onDismiss){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back",tint=Color.Black,modifier=Modifier.size(27.dp))}
                Text("Edit my family",color=Color.Black,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.width(44.dp))
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom=20.dp)){
                Box(Modifier.align(Alignment.CenterHorizontally).padding(top=24.dp).size(100.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F4F6)).clickable{picker.launch(arrayOf("image/*"))},contentAlignment=Alignment.Center){
                    if(image.isNotBlank())AsyncImage(model=image,contentDescription="Family DP",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                    else Text("＋",color=Color.Gray,fontSize=30.sp)
                }
                Text("Family name",color=Color.Black,fontWeight=FontWeight.Bold,fontSize=14.sp,modifier=Modifier.padding(start=20.dp,top=28.dp,bottom=8.dp))
                OutlinedTextField(value=name,onValueChange={name=it},modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp),singleLine=true,shape=RoundedCornerShape(12.dp),placeholder={Text("Family Name")})
                Text("Family Announcement",color=Color.Black,fontWeight=FontWeight.Bold,fontSize=14.sp,modifier=Modifier.padding(start=20.dp,top=20.dp,bottom=8.dp))
                OutlinedTextField(value=announcement,onValueChange={if(it.length<=500)announcement=it},modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp).height(150.dp),shape=RoundedCornerShape(12.dp),placeholder={Text("Family Announcement")})
                Text(announcement.length.toString()+"/500",color=Color.Gray,fontSize=12.sp,modifier=Modifier.align(Alignment.End).padding(end=22.dp,top=4.dp))
            }
            Button(onClick={onSave(name,image,announcement)},enabled=name.isNotBlank(),modifier=Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=20.dp,vertical=14.dp).height(52.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2878E8))){
                Text("Save Changes",color=Color.White,fontWeight=FontWeight.Bold)
            }
        }
    }
}
