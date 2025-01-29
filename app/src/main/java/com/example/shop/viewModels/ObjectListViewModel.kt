package com.example.shop.viewModels

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shop.data.Anime
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class ObjectListViewModel : ViewModel() {
    private val _objects = mutableStateOf<List<Anime>>(emptyList())
    val objects: State<List<Anime>> = _objects

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    fun loadObjects() {
        viewModelScope.launch {
            try {
                val firestore = Firebase.firestore
                firestore.collection("anime").get()
                    .addOnSuccessListener { querySnapshot ->
                        val fetchedObjects = querySnapshot.documents.mapNotNull { document ->
                            document.toObject(Anime::class.java)?.apply {
                                id = document.id
                            }
                        }
                        _objects.value = fetchedObjects
                        _isLoading.value = false
                    }
                    .addOnFailureListener { exception ->
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
}
