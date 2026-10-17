package com.example

import android.os.Bundle
import android.view.ActionMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.components.SafeFloatingActionMode
import com.example.ui.navigation.CampusApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val schoolApp = application as SchoolApplication

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CampusApp(app = schoolApp)
                }
            }
        }
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        if (type == ActionMode.TYPE_FLOATING && callback != null) {
            return SafeFloatingActionMode(this, callback)
        }
        return super.onWindowStartingActionMode(callback, type)
    }
}
