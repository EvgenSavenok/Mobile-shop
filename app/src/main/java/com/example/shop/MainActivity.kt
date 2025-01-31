package com.example.shop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import com.example.shop.data.Anime
import com.example.shop.data.BottomNavItem
import com.example.shop.data.UserProfile
import com.example.shop.viewModels.AuthenticationViewModel
import com.example.shop.viewModels.ObjectListViewModel
import com.google.firebase.firestore.FieldPath

class MainActivity : ComponentActivity()
{

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                setContent {
                    ShopTheme {
                        AuthenticationScreen(auth = auth) {
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
    fun HomeScreen(auth: FirebaseAuth) {
        val navController = rememberNavController()
        val userId = auth.currentUser?.uid.orEmpty()

        Scaffold(
            topBar = { CustomTopAppBar() },
            bottomBar = { BottomNavigationBar(navController) }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "objectList",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("objectList") {
                    ObjectListScreen { objectId ->
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
                    ObjectDetailsScreen(objectId, userId)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomTopAppBar() {
        TopAppBar(
            title = {
                Text(
                    text = "Anime App",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
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
                    val userFavorites = document.get("favorites") as? List<*>
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

    private fun addToFavorites(
        userId: String,
        animeId: String,
        firestore: FirebaseFirestore,
        onSuccess: () -> Unit
    ) {
        val userRef = firestore.collection("users").document(userId)
        userRef.update("favorites", FieldValue.arrayUnion(animeId))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
            }
    }

    @Composable
    fun ImageCard(imageUrl: String) {
        var isDialogOpen by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp)
                .clickable { isDialogOpen = true },
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .scale(Scale.FILL)
                        .build()
                )

                Image(
                    painter = painter,
                    contentDescription = "Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (isDialogOpen) {
            ImageDialog(imageUrl = imageUrl, onDismiss = { isDialogOpen = false })
        }
    }

    @Composable
    fun ImageDialog(imageUrl: String, onDismiss: () -> Unit) {
        Dialog(onDismissRequest = onDismiss) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build()
                        )
                        Image(
                            painter = painter,
                            contentDescription = "Enlarged Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .fillMaxWidth(0.6f)
                        ) {
                            Text("Закрыть")
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun BottomNavigationBar(navController: NavController) {
        val items = listOf(
            BottomNavItem("objectList", "Список", Icons.AutoMirrored.Filled.List),
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
    fun ObjectListScreen(onObjectClick: (String) -> Unit) {
        val viewModel: ObjectListViewModel = viewModel()
        val objects = viewModel.objects.value
        val isLoading = viewModel.isLoading.value

        LaunchedEffect(Unit) {
            viewModel.loadObjects()
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
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(obj.images[0])
                        .crossfade(true)
                        .scale(Scale.FILL)
                        .build()
                )

                Image(
                    painter = painter,
                    contentDescription = "Anime Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = obj.name,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = obj.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }


    @Composable
    fun EmailTextField(email: String, onEmailChange: (String) -> Unit) {
        TextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    fun PasswordTextField(password: String, onPasswordChange: (String) -> Unit) {
        TextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    fun AuthenticationButton(isLogin: Boolean, onAuthenticate: () -> Unit) {
        Button(
            onClick = onAuthenticate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLogin) "Войти" else "Зарегистрироваться")
        }
    }

    @Composable
    fun ToggleLoginButton(isLogin: Boolean, onToggle: () -> Unit) {
        TextButton(onClick = onToggle) {
            Text(if (isLogin) "Создать аккаунт" else "Уже есть аккаунт? Войти")
        }
    }

    @Composable
    fun ErrorMessage(message: String) {
        if (message.isNotEmpty()) {
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }
    }

    @Composable
    fun AuthenticationScreen(
        auth: FirebaseAuth,
        onSuccess: (FirebaseAuth) -> Unit
    ) {
        val viewModel: AuthenticationViewModel = viewModel()
        val email by viewModel.email
        val password by viewModel.password
        val message by viewModel.message
        val isLogin by viewModel.isLogin
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLogin) "Вход" else "Регистрация",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            EmailTextField(email, viewModel::onEmailChange)
            Spacer(modifier = Modifier.height(8.dp))
            PasswordTextField(password, viewModel::onPasswordChange)
            Spacer(modifier = Modifier.height(16.dp))
            AuthenticationButton(isLogin) {
                viewModel.onAuthenticate(auth) { authResult ->
                    onSuccess(authResult)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ToggleLoginButton(isLogin, viewModel::onToggleLogin)
            Spacer(modifier = Modifier.height(16.dp))
            ErrorMessage(message)
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
                            .addOnFailureListener {
                                isLoading = false
                            }
                    } else {
                        isLoading = false
                    }
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(favoriteObjects) { obj ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { navController.navigate("objectDetails/${obj.id}") },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Box {
                            val painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(obj.images[0])
                                    .crossfade(true)
                                    .build()
                            )

                            Image(
                                painter = painter,
                                contentDescription = obj.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                contentScale = ContentScale.Crop
                            )

                            IconButton(
                                onClick = {
                                    removeFromFavorites(firestore, userId, obj.id)
                                    favoriteObjects = favoriteObjects.filter { it.id != obj.id }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить из избранного",
                                    tint = Color.White
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = obj.name,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }


    private fun removeFromFavorites(firestore: FirebaseFirestore, userId: String, animeId: String) {
        val userRef = firestore.collection("users").document(userId)
        userRef.update("favorites", FieldValue.arrayRemove(animeId))
            .addOnSuccessListener {
            }
            .addOnFailureListener {
            }
    }

    private fun formatDate(timestamp: String): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        if (timestamp.isNotEmpty()) {
            val date = Date(timestamp.toLong())
            return sdf.format(date)
        }
        return ""
    }

    @Composable
    fun UserProfileScreen(auth: FirebaseAuth) {
        val firestore = Firebase.firestore
        val userId = auth.currentUser?.uid ?: return
        var userProfile by remember { mutableStateOf<UserProfile?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var showDeleteDialog by remember { mutableStateOf(false) }
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
                            userProfile = userProfile?.copy(birthDate = parseDate())
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { auth.signOut() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Выйти из системы")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(Color.Red),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Удалить аккаунт", color = Color.White)
                        }
                    }
                }
            } ?: Text("Профиль не найден", modifier = Modifier.fillMaxSize())
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удаление аккаунта") },
                text = { Text("Вы уверены, что хотите удалить аккаунт? Это действие необратимо.") },
                confirmButton = {
                    Button(
                        onClick = {
                            deleteAccount(auth, firestore, userId) { success ->
                                if (success) {
                                    showDeleteDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(Color.Red)
                    ) {
                        Text("Удалить", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }

    private fun updateUserField(firestore: FirebaseFirestore, userId: String, fieldName: String, value: String) {
        firestore.collection("users").document(userId)
            .update(fieldName, value)
            .addOnSuccessListener { println("$fieldName обновлено успешно") }
            .addOnFailureListener { println("Ошибка обновления $fieldName: ${it.message}") }
    }

    private fun parseDate(): String {
        return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).toString()
    }

    private fun deleteAccount(auth: FirebaseAuth, firestore: FirebaseFirestore, userId: String, onResult: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(userId).delete()
                .addOnSuccessListener {
                    currentUser.delete()
                        .addOnCompleteListener { task ->
                            onResult(task.isSuccessful)
                        }
                }
                .addOnFailureListener {
                    onResult(false)
                }
        } else {
            onResult(false)
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
}
