package com.example.shop

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shop.ui.theme.ShopTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null)
            {
                setContent {
                    ShopTheme {
                        AuthenticationScreen(auth = auth) { user ->
                            navigateToHomeScreen()
                        }
                    }
                }
            } else {
                navigateToHomeScreen()
            }
        }
    }

    private fun navigateToHomeScreen() {
        setContent {
            ShopTheme {
                HomeScreen(auth = auth)
            }
        }
    }


    @Composable
    fun ObjectDetailsScreen(objectId: String, userId: String) {
        val firestore = Firebase.firestore
        var anime by remember { mutableStateOf<Anime?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var isFavorite by remember { mutableStateOf(false) }

        LaunchedEffect(objectId) {
            firestore.collection("anime").document(objectId).get()
                .addOnSuccessListener { document ->
                    anime = document.toObject(Anime::class.java)?.apply {
                        id = document.id
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        }

        LaunchedEffect(userId, objectId) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val userFavorites = document.get("favorites") as? List<String>
                    isFavorite = userFavorites?.contains(objectId) == true
                }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            anime?.let { obj ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = obj.name,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = obj.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(text = "Изображения:", style = MaterialTheme.typography.bodyMedium)

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        items(obj.images) { imageUrl ->
                            ImageCard(imageUrl)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (!isFavorite) {
                                addToFavorites(userId, objectId, firestore) {
                                    isFavorite = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isFavorite
                    ) {
                        Text(
                            text = if (isFavorite) "Уже в избранном" else "Добавить в избранное"
                        )
                    }
                }
            }
        }
    }

    private fun addToFavorites(userId: String, animeId: String, firestore: FirebaseFirestore, onSuccess: () -> Unit) {
        val userRef = firestore.collection("users").document(userId)
        userRef.update("favorites", FieldValue.arrayUnion(animeId))
            .addOnSuccessListener {
                println("Аниме добавлено в избранное!")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                println("Ошибка при добавлении в избранное: ${exception.message}")
            }
    }

    @Composable
    fun ImageCard(imageUrl: String) {
        Card(
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Image")
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomeScreen(auth: FirebaseAuth) {
        val navController = rememberNavController()

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Shop App") })
            },
            bottomBar = {
                BottomNavigationBar(navController)
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "objectList",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("objectList") {
                    ObjectListScreen(auth) { objectId ->
                        navController.navigate("objectDetails/$objectId")
                    }
                }
                composable("favorites") {
                    FavoritesScreen(auth, navController)
                }
                composable("profile") {
                    UserProfileScreen(auth)
                }
                composable("objectDetails/{objectId}") { backStackEntry ->
                    val objectId = backStackEntry.arguments?.getString("objectId") ?: ""
                    val userId = auth.currentUser?.uid.orEmpty()
                    ObjectDetailsScreen(objectId, userId)
                }
            }
            }
        }
    }

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("objectList", "Список", Icons.Default.List),
        BottomNavItem("favorites", "Избранное", Icons.Default.Favorite),
        BottomNavItem("profile", "Профиль", Icons.Default.Person)
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = false,
                onClick = { navController.navigate(item.route) }
            )
        }
    }
}


@Composable
fun ObjectListScreen(auth: FirebaseAuth, onObjectClick: (String) -> Unit) {
    val firestore = Firebase.firestore
    var objects by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        firestore.collection("anime").get()
            .addOnSuccessListener { querySnapshot ->
                val fetchedObjects = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(Anime::class.java)?.apply {
                        id = document.id
                    }
                }
                objects = fetchedObjects
                isLoading = false
            }
            .addOnFailureListener { exception ->
                println("Ошибка получения данных: ${exception.message}")
                isLoading = false
            }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            items(objects) { obj ->
                ObjectItem(obj, onClick = { onObjectClick(obj.id) })
                Divider()
            }
        }
    }
}

@Composable
fun ObjectItem(obj: Anime, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = obj.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = obj.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    }

@Composable
fun AuthenticationScreen(auth: FirebaseAuth, onSuccess: (FirebaseAuth) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = if (isLogin) "Вход" else "Регистрация", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isLogin) {
                    login(auth, email, password) { success, error ->
                        if (success) {
                            onSuccess(auth)
                        } else {
                            message = error ?: "Ошибка входа"
                        }
                    }
                } else {
                    register(auth, email, password) { success, error ->
                        if (success) {
                            message = "Регистрация успешна!"
                            isLogin = true
                        } else {
                            message = error ?: "Ошибка регистрации"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLogin) "Войти" else "Зарегистрироваться")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { isLogin = !isLogin }) {
            Text(if (isLogin) "Создать аккаунт" else "Уже есть аккаунт? Войти")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun login(
    auth: FirebaseAuth,
    email: String,
    password: String,
    callback: (Boolean, String?) -> Unit
) {
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true, null)
            } else {
                callback(false, task.exception?.message)
            }
        }
}

private fun register(
    auth: FirebaseAuth,
    email: String,
    password: String,
    callback: (Boolean, String?) -> Unit
) {
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                val firestore = Firebase.firestore

                val user = UserProfile(
                    name = "",
                    email = email,
                    registrationDate = System.currentTimeMillis().toString(),
                    surname = "",
                    country = "",
                    city = "",
                    birthDate = "",
                    sex = "",
                    phone = "",
                    status = ""
                )

                firestore.collection("users").document(userId)
                    .set(user)
                    .addOnSuccessListener {
                        callback(true, null)
                    }
                    .addOnFailureListener { exception ->
                        callback(false, exception.message)
                    }
            } else {
                callback(false, task.exception?.message)
            }
        }
}


@Composable
fun FavoritesScreen(auth: FirebaseAuth, navController: NavController) {
    val userId = auth.currentUser?.uid ?: return
    val firestore = Firebase.firestore
    var favoriteObjects by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val favorites = document.get("favorites") as? List<String> ?: emptyList()
                if (favorites.isNotEmpty()) {
                    firestore.collection("anime").whereIn(FieldPath.documentId(), favorites).get()
                        .addOnSuccessListener { querySnapshot ->
                            favoriteObjects = querySnapshot.documents.mapNotNull { doc ->
                                doc.toObject(Anime::class.java)?.apply {
                                    id = doc.id
                                }
                            }
                            isLoading = false
                        }
                        .addOnFailureListener { exception ->
                            println("Ошибка загрузки аниме: ${exception.message}")
                            isLoading = false
                        }
                } else {
                    isLoading = false
                }
            }
            .addOnFailureListener { exception ->
                println("Ошибка загрузки избранного: ${exception.message}")
                isLoading = false
            }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            items(favoriteObjects) { obj ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("objectDetails/${obj.id}")
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = obj.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = obj.description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            removeFromFavorites(firestore, userId, obj.id)
                            favoriteObjects = favoriteObjects.filter { it.id != obj.id }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить из избранного"
                        )
                    }
                }
                Divider()
            }
        }
    }
}

fun removeFromFavorites(firestore: FirebaseFirestore, userId: String, animeId: String) {
    val userRef = firestore.collection("users").document(userId)
    userRef.update("favorites", FieldValue.arrayRemove(animeId))
        .addOnSuccessListener {
            println("Удалено из избранного: $animeId")
        }
        .addOnFailureListener { exception ->
            println("Ошибка удаления: ${exception.message}")
        }
}

fun formatDate(timestamp: String): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    if (timestamp.isNotEmpty()) {
        val date = Date(timestamp.toLong())
        return sdf.format(date)
    }
    return ""
}

fun updateUserField(firestore: FirebaseFirestore, userId: String, fieldName: String, value: String) {
    firestore.collection("users").document(userId)
        .update(fieldName, value)
        .addOnSuccessListener { println("$fieldName обновлено успешно") }
        .addOnFailureListener { println("Ошибка обновления $fieldName: ${it.message}") }
}

fun parseDate(dateString: String): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).toString()
}

@Composable
fun UserProfileScreen(auth: FirebaseAuth) {
    val firestore = Firebase.firestore
    val userId = auth.currentUser?.uid ?: return
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                userProfile = documentSnapshot.toObject(UserProfile::class.java)
                isLoading = false
            }
            .addOnFailureListener { exception ->
                println("Ошибка загрузки профиля: ${exception.message}")
                isLoading = false
            }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        userProfile?.let { profile ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    EditableProfileField("Имя", profile.name) { newValue ->
                        userProfile = userProfile?.copy(name = newValue)
                        updateUserField(firestore, userId, "name", newValue)
                    }
                    EditableProfileField("Фамилия", profile.surname) { newValue ->
                        userProfile = userProfile?.copy(surname = newValue)
                        updateUserField(firestore, userId, "surname", newValue)
                    }
                    ProfileField("Email", profile.email)
                    EditableProfileField("Страна", profile.country) { newValue ->
                        userProfile = userProfile?.copy(country = newValue)
                        updateUserField(firestore, userId, "country", newValue)
                    }
                    EditableProfileField("Город", profile.city) { newValue ->
                        userProfile = userProfile?.copy(city = newValue)
                        updateUserField(firestore, userId, "city", newValue)
                    }
                    ProfileField("Дата регистрации", formatDate(profile.registrationDate))
                    EditableProfileField("Дата рождения", formatDate(profile.birthDate)) { newValue ->
                        userProfile = userProfile?.copy(birthDate = parseDate(newValue))
                        updateUserField(firestore, userId, "birthDate", newValue)
                    }
                    EditableProfileField("Пол", profile.sex) { newValue ->
                        userProfile = userProfile?.copy(sex = newValue)
                        updateUserField(firestore, userId, "sex", newValue)
                    }
                    EditableProfileField("Телефон", profile.phone) { newValue ->
                        userProfile = userProfile?.copy(phone = newValue)
                        updateUserField(firestore, userId, "phone", newValue)
                    }
                    EditableProfileField("Описание", profile.status) { newValue ->
                        userProfile = userProfile?.copy(status = newValue)
                        updateUserField(firestore, userId, "status", newValue)
                    }
                }

                Button(
                    onClick = { auth.signOut() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Выйти из системы")
                }
            }
        } ?: Text("Профиль не найден", modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun EditableProfileField(label: String, value: String?, onValueChange: (String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var editableValue by remember { mutableStateOf(value ?: "") }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)

        if (isEditing) {
            OutlinedTextField(
                value = editableValue,
                onValueChange = { editableValue = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        onValueChange(editableValue)
                        isEditing = false
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
                    }
                }
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (value.isNullOrEmpty()) "Не указано" else value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                }
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String?) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = if (value.isNullOrEmpty()) "Не указано" else value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}