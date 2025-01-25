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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
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
    fun ObjectDetailsScreen(objectId: String) {
        val firestore = Firebase.firestore
        var anime by remember { mutableStateOf<Anime?>(null) }
        var isLoading by remember { mutableStateOf(true) }

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
                            firestore.collection("anime").document(obj.id)
                                .update("isFavorite", !obj.isFavorite)
                                .addOnSuccessListener {
                                    obj.isFavorite = !obj.isFavorite
                                }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (obj.isFavorite) "Удалить из избранного" else "Добавить в избранное")
                    }
                }
            }
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
            // Image(painter = rememberImagePainter(imageUrl), contentDescription = null)
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
                    FavoritesScreen(auth)
                }
                composable("profile") {
                    UserProfileScreen(auth)
                }
                composable("objectDetails/{objectId}") { backStackEntry ->
                    val objectId = backStackEntry.arguments?.getString("objectId") ?: ""
                    ObjectDetailsScreen(objectId)
                }
            }


//            composable("favorites") { FavoritesScreen(auth) }
//            composable("profile") { UserProfileScreen(auth) }
//            composable("objectDetails/{objectId}") { backStackEntry ->
//                val objectId = backStackEntry.arguments?.getString("objectId") ?: ""
//                ObjectDetailsScreen(objectId)
//            }
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
                callback(true, null)
            } else {
                callback(false, task.exception?.message)
            }
        }
}

@Composable
fun FavoritesScreen(auth: FirebaseAuth) {
    val userId = auth.currentUser?.uid ?: return
    val firestore = Firebase.firestore
    var favoriteObjects by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        firestore.collection("users").document(userId).collection("favorites").get()
            .addOnSuccessListener { querySnapshot ->
                favoriteObjects = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(Anime::class.java)?.apply {
                        id = document.id
                    }
                }
                isLoading = false
            }
            .addOnFailureListener { exception ->
                println("Ошибка получения избранного: ${exception.message}")
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
                ObjectItem(obj, onClick = {
                    removeFromFavorites(firestore, userId, obj.id)
                    favoriteObjects = favoriteObjects.filter { it.id != obj.id }
                })
                Divider()
            }
        }
    }
}

private fun removeFromFavorites(firestore: FirebaseFirestore, userId: String, objectId: String) {
    firestore.collection("users").document(userId).collection("favorites").document(objectId)
        .delete()
        .addOnSuccessListener {
            Log.d("Favorites", "Объект удален из избранного: $objectId")
        }
        .addOnFailureListener { exception ->
            Log.e("Favorites", "Ошибка удаления из избранного: ${exception.message}")
        }
}

@Composable
fun UserProfileScreen(auth: FirebaseAuth) {
    val firestore = Firebase.firestore
    val userId = auth.currentUser?.uid ?: return
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

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
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    ProfileField("Имя", profile.name)
                    ProfileField("Дата рождения", profile.birthDate)
                    ProfileField("Email", profile.email)
                    ProfileField("Телефон", profile.phone)
                    ProfileField("Город", profile.city)
                    ProfileField("Описание", profile.description)
                    ProfileField("Страна", profile.country)
                    ProfileField("Пол", profile.gender)
                    ProfileField("Зарегистрирован", profile.registrationDate)
                    ProfileField("Активность", profile.lastActiveDate)
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
fun ProfileField(label: String, value: String?) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = value ?: "Не указано",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


