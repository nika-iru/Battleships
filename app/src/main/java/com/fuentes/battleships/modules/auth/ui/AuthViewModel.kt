package com.fuentes.battleships.modules.auth.ui

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.util.UUID

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthState())
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    init {
        if (Firebase.auth.currentUser != null) {
            _uiState.update { currentState
                ->
                currentState.copy(
                    email = Firebase.auth.currentUser?.email,
                    userId = Firebase.auth.currentUser?.uid
                )
            }
        }
    }

    fun logout() {
        Firebase.auth.signOut()

        _uiState.update { currentState
            ->
            currentState.copy(
                email = null,
                userId = null,
            )
        }
    }

    fun getCurrentUserId(): String? {
        return Firebase.auth.currentUser?.uid
    }

    fun getCurrentUserEmail(): String? {
        return Firebase.auth.currentUser?.email
    }

    fun registerUser(email: String, password: String) {
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // sign out immediately after registering
                    Firebase.auth.signOut()
                } else {
                    Log.w("FIREBASE_REGISTER", "createUserWithEmail:failure")
                }
            }
    }

    fun signInUserWithEmailAndPassword(email: String, password: String) {
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val currentUser = task.result.user

                    _uiState.update { currentState
                        ->
                        currentState.copy(
                            email = currentUser?.email,
                            userId = Firebase.auth.currentUser?.uid
                        )
                    }


                } else {
                    Log.w("FIREBASE_REGISTER", "signInWithEmailAndPassword:failure")
                }
            }
    }

    fun signInWithGoogle(appContext: Context) {
        viewModelScope.launch {
            // create google id option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // allow user to pick any account
                .setServerClientId("202765760718-hjut6cq0ie0cu7vv7em2rirvvc3eoeq8.apps.googleusercontent.com")
                .setAutoSelectEnabled(false) // prevent the app from auto selecting the previous account
                .setNonce(UUID.randomUUID().toString())
                .build()

            // create google sign-in request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // create google credential
            val credentialManager = CredentialManager.create(appContext)
            val credentialResponse = credentialManager.getCredential(
                request = request,
                context = appContext,
            )
            val credential = credentialResponse.credential

            if (credential is CustomCredential) {
                // check the credential type
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    // Use googleIdTokenCredential and extract id to validate and
                    // authenticate to firebase.
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    // create firebase credential
                    val firebaseCredential = GoogleAuthProvider.getCredential(
                        googleIdTokenCredential.idToken, null
                    )

                    val authResult = Firebase.auth.signInWithCredential(firebaseCredential)
                    authResult.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val currentUser = task.result.user

                            if (currentUser != null) {
                                // user successfully logged in
                                _uiState.update { currentState
                                    ->
                                    currentState.copy(
                                        email = currentUser.email,
                                        userId = Firebase.auth.currentUser?.uid
                                    )
                                }
                            }
                        } else {
                            Log.e("FIREBASE_LOGIN", "The login task failed.")
                        }
                    }
                } else {
                    // Catch any unrecognized credential type here.
                    Log.e("GOOGLE_CREDENTIAL", "Unexpected type of credential")
                }
            } else {
                // Catch any unrecognized credential type here.
                Log.e("GOOGLE_CREDENTIAL", "Unexpected type of credential")
            }
        }
    }
}