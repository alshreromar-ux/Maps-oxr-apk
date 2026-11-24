package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOut
import com.example.data.CityPulseRepository
import com.example.data.SavedPlacesLocalStore
import com.example.firebase.configureAppCheck
import com.example.ui.CityPulseMapScreen
import com.example.ui.CityPulseViewModel
import com.example.ui.theme.CityPulseTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            CityPulseTheme {
                CityPulseApp()
            }
        }
    }
}

@Composable
fun CityPulseApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isGuestMode by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val updated = auth.currentUser
            currentUser = updated
            if (updated != null) {
                isGuestMode = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = {
                isGuestMode = false
                currentUser = Firebase.auth.currentUser
            },
            onContinueAsGuest = {
                isGuestMode = true
            }
        )
    } else {
        val isGuestSession = user == null
        val sessionKey = user?.uid ?: "guest_session"
        val displayName = if (isGuestSession) {
            "ضيف نبض المدينة"
        } else {
            user?.displayName?.takeIf { it.isNotBlank() } ?: "مستخدم نبض المدينة"
        }
        key(sessionKey) {
            val viewModel: CityPulseViewModel = viewModel(
                key = sessionKey,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val localStore = SavedPlacesLocalStore(app)
                        val repo = if (isGuestSession) {
                            CityPulseRepository(db = null, localStore = localStore)
                        } else {
                            val databaseId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(databaseId)
                            CityPulseRepository(db = db, localStore = localStore)
                        }
                        CityPulseViewModel(
                            repository = repo,
                            currentUserId = sessionKey,
                            currentUserDisplayName = displayName,
                            isGuestMode = isGuestSession
                        )
                    }
                }
            )
            CityPulseMapScreen(
                viewModel = viewModel,
                onSignOut = {
                    if (isGuestSession) {
                        isGuestMode = false
                    } else {
                        signOut(credentialManager = credentialManager, scope = scope)
                    }
                },
                onGuestUpgradeToGoogle = {
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isGuestMode = false
                            currentUser = Firebase.auth.currentUser
                        },
                        onAuthError = { msg ->
                            viewModel.postMessage(msg)
                        },
                        scope = scope
                    )
                }
            )
        }
    }
}
