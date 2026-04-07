package com.example.productivityapp.ui.water

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.datastore.UserDataStore
import com.example.productivityapp.viewmodel.WaterViewModel
import com.example.productivityapp.viewmodel.WaterViewModelFactory

@Composable
fun WaterIntakeScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val ds = UserDataStore(ctx.applicationContext)
    val vm: WaterViewModel = viewModel(factory = WaterViewModelFactory(ds))
    val water = vm.waterMl.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Water Intake")
            Text("Today: ${water.value} ml")
            Button(onClick = { vm.addWater(200) }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Add 200 ml")
            }
            Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("Back") }
        }
    }
}

