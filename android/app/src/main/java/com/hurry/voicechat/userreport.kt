package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable fun UserReportScreen(onClose:()->Unit={},onSubmit:(String,String)->Unit={_,_->}) {
 var category by remember{mutableStateOf("")};var description by remember{mutableStateOf("")};var submitted by remember{mutableStateOf(false)}
 val cats=listOf("Violence","Abusing","Illegal","Other's")
 if(submitted){Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center){Text("Thank You!",style=MaterialTheme.typography.headlineSmall);Text("Your report has been submitted successfully.");Button(onClose){Text("OK")}};return}
 Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)){Row{TextButton(onClose){Text("‹")};Spacer(Modifier.weight(1f));Text("Report",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.weight(1f))}
 Row{cats.forEach{FilterChip(category==it,{category=it},{Text(it)});Spacer(Modifier.width(4.dp))}}
 OutlinedTextField(description,{description=it},Modifier.fillMaxWidth().height(140.dp),label={Text("Description")})
 Spacer(Modifier.height(12.dp));Button({if(category.isNotBlank()){onSubmit(category,description);submitted=true}},Modifier.fillMaxWidth()){Text("Submit")}}
}