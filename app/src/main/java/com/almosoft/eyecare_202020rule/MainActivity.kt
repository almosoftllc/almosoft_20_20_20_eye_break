package com.almosoft.eyecare_202020rule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog as AndroidTimePickerDialog
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.almosoft.eyecare_202020rule.ui.theme.EyeCare202020RuleTheme
import com.almosoft.eyecare_202020rule.ui.theme.PrimaryBlue
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.apply {
                    language = Locale.US
                    setSpeechRate(0.8f) // Slower speed for better clarity
                    setPitch(1.0f)
                }
            }
        }

        // Request consent and initialize AdMob
        requestConsentAndInitializeAds()

        val preferenceManager = PreferenceManager(this)
        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(preferenceManager) as T
            }
        }

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = viewModelFactory)
            val isDarkMode by mainViewModel.isDarkModeEnabled.collectAsState(initial = false)

            EyeCare202020RuleTheme(darkTheme = isDarkMode) {
                EyeCareApp(mainViewModel) { text ->
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    private fun requestConsentAndInitializeAds() {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this
                ) { loadAndShowError ->
                    // Consent has been gathered or isn't required.
                    if (consentInformation.canRequestAds()) {
                        MobileAds.initialize(this) {}
                    }
                }
            },
            { _ ->
                // Consent gathering failed.
                if (consentInformation.canRequestAds()) {
                    MobileAds.initialize(this) {}
                }
            }
        )

        // Check if ads can already be requested (e.g. from previous session)
        if (consentInformation.canRequestAds()) {
            MobileAds.initialize(this) {}
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun EyeCareApp(viewModel: MainViewModel, onSpeak: (String) -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isOnboardingCompleted by PreferenceManager(LocalContext.current).isOnboardingCompleted.collectAsState(initial = null)
    val context = LocalContext.current

    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState(initial = true)
    val isNotificationToneEnabled by viewModel.isNotificationToneEnabled.collectAsState(initial = true)
    val isVoiceNotificationEnabled by viewModel.isVoiceNotificationEnabled.collectAsState(initial = true)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled by the system or ignored
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Global Alert Trigger (Works across all tabs and while app is in foreground/background process)
    LaunchedEffect(Unit) {
        viewModel.timerEvents.collect { event ->
            val isBreakEvent = event is TimerEvent.BreakStarted
            
            if (isVibrationEnabled) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }

            if (isVoiceNotificationEnabled) {
                try {
                    val textToSpeak = if (isBreakEvent) "Break time." else "Back to work."
                    onSpeak(textToSpeak)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (isNotificationToneEnabled) {
                try {
                    ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100).startTone(ToneGenerator.TONE_PROP_BEEP)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Show a notification as well for background visibility
            showTimerNotification(context, isBreakEvent)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute !in listOf("splash", "onboarding", "privacy", "terms") && currentRoute != null) {
                BottomNavigationBar(navController)
            }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(navController, viewModel, isOnboardingCompleted)
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController, viewModel: MainViewModel, isOnboardingCompleted: Boolean?) {
    if (isOnboardingCompleted == null) return

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController, isOnboardingCompleted) }
        composable("onboarding") { OnboardingScreen(navController, viewModel) }
        composable("home") { HomeScreen(navController) }
        composable("timer") { TimerScreen(viewModel) }
        composable("exercises") { ExercisesScreen(navController) }
        composable("exercise_detail/{index}") { backStackEntry ->
            val index = backStackEntry.arguments?.getString("index")?.toInt() ?: 0
            ExerciseDetailScreen(index)
        }
        composable("tips") { TipsScreen() }
        composable("facts") { FactsScreen() }
        composable("settings") { SettingsScreen(viewModel, navController) }
        composable("about") { AboutScreen(navController) }
        composable("privacy") { PrivacyPolicyScreen(navController) }
        composable("terms") { TermsOfServiceScreen(navController) }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Home,
        Screen.Timer,
        Screen.Exercises,
        Screen.Tips,
        Screen.Settings
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Text(screen.emoji, fontSize = 24.sp) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

sealed class Screen(val route: String, val title: String, val emoji: String) {
    object Home : Screen("home", "Home", "🏠")
    object Timer : Screen("timer", "Timer", "⏱️")
    object Exercises : Screen("exercises", "Exercises", "👁️")
    object Tips : Screen("tips", "Tips", "💡")
    object Settings : Screen("settings", "Settings", "⚙️")
}

@Composable
fun SplashScreen(navController: NavHostController, isOnboardingCompleted: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "eye_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eye_scale"
    )

    LaunchedEffect(Unit) {
        delay(3000)
        if (isOnboardingCompleted) {
            navController.navigate("home") { popUpTo("splash") { inclusive = true } }
        } else {
            navController.navigate("onboarding") { popUpTo("splash") { inclusive = true } }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Eye Care", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
            Text("Protect Your Eyes", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun OnboardingScreen(navController: NavHostController, viewModel: MainViewModel) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var isChecked by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = when (page) {
                        0 -> "What is 20-20-20 rule?"
                        1 -> "How to use this app?"
                        else -> "Enable notifications for reminders"
                    },
                    fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    when (page) {
                        0 -> "Every 20 minutes, look at something 20 feet away for 20 seconds."
                        1 -> "Start the timer, and we'll remind you when it's time to take a break."
                        else -> "Stay consistent and protect your eyes from digital strain."
                    },
                    textAlign = TextAlign.Center
                )
                if (page == 2) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isChecked, onCheckedChange = { isChecked = it })
                        Text("I agree to Privacy Policy and Terms of Service", fontSize = 12.sp)
                    }
                }
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage < 2) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    viewModel.setOnboardingCompleted()
                    navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                }
            },
            enabled = pagerState.currentPage < 2 || isChecked,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (pagerState.currentPage < 2) "Next" else "Get Started")
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Eye Care", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HomeButton("20-20-20 Timer", "⏱️", PrimaryBlue) { navController.navigate("timer") }
            HomeButton("Eye Exercises", "👁️", Color(0xFF4CAF50)) { navController.navigate("exercises") }
            HomeButton("Eye Tips", "💡", Color(0xFFFFC107)) { navController.navigate("tips") }
            HomeButton("Eye Health Facts", "🧠", Color(0xFF9C27B0)) { navController.navigate("facts") }
        }

        AdMobPlaceholder()
    }
}

@Composable
fun HomeButton(text: String, emoji: String, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 40.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun TimerScreen(viewModel: MainViewModel) {
    val secondsLeft by viewModel.timerSeconds.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val isBreak by viewModel.isBreak.collectAsState()
    val interval by viewModel.timerInterval.collectAsState(initial = 20)
    val startTime by viewModel.startTime.collectAsState(initial = "09:00")
    val endTime by viewModel.endTime.collectAsState(initial = "17:00")

    val totalSeconds = if (isBreak) 20 else interval * 60
    val progress = secondsLeft.toFloat() / totalSeconds

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { time -> viewModel.setStartTime(time); showStartTimePicker = false },
            initialTime = startTime
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { time -> viewModel.setEndTime(time); showEndTimePicker = false },
            initialTime = endTime
        )
    }


    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (isBreak) "Break Time! 🌿" else "Work Time 👨‍💻", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            WorkTimeChip("Start: $startTime", Modifier.weight(1f)) { showStartTimePicker = true }
            WorkTimeChip("End: $endTime", Modifier.weight(1f)) { showEndTimePicker = true }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
            Canvas(modifier = Modifier.size(280.dp)) {
                drawCircle(color = PrimaryBlue.copy(alpha = 0.1f), style = Stroke(width = 20.dp.toPx()))
                drawArc(
                    color = PrimaryBlue,
                    startAngle = -90f,
                    sweepAngle = 360 * progress,
                    useCenter = false,
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format(Locale.getDefault(), "%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                    fontSize = 48.sp, fontWeight = FontWeight.Bold
                )
                Text(if (isBreak) "Focus 20ft away" else "Stay focused", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { if (isRunning) viewModel.pauseTimer() else viewModel.resumeTimer() },
                modifier = Modifier.weight(1f).height(60.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (isRunning) "Pause" else if (secondsLeft == interval * 60) "Start" else "Resume",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = { viewModel.resetTimer() },
                modifier = Modifier.weight(1f).height(60.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, PrimaryBlue)
            ) {
                Text("Reset", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Change Interval", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { viewModel.setTimerInterval(20) },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Reset to 20m", fontSize = 12.sp)
                    }
                }
                Text("$interval min", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
            }
            Slider(
                value = interval.toFloat(),
                onValueChange = { viewModel.setTimerInterval(it.toInt()) },
                valueRange = 1f..60f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryBlue,
                    activeTrackColor = PrimaryBlue,
                    inactiveTrackColor = PrimaryBlue.copy(alpha = 0.2f)
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        AdMobPlaceholder()
    }
}

@Composable
fun WorkTimeChip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(16.dp),
        color = PrimaryBlue.copy(alpha = 0.05f),
        border = BorderStroke(1.5.dp, PrimaryBlue.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit,
    initialTime: String
) {
    val context = LocalContext.current
    val parts = initialTime.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    DisposableEffect(Unit) {
        val picker = AndroidTimePickerDialog(
            context,
            { _, h, m ->
                onTimeSelected(String.format(Locale.getDefault(), "%02d:%02d", h, m))
            },
            hour,
            minute,
            true
        )
        picker.setOnDismissListener { onDismiss() }
        picker.show()
        onDispose { picker.dismiss() }
    }
}

@Composable
fun ExercisesScreen(navController: NavHostController) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Eye Exercises", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp)) }
        
        exercises.indices.forEach { index ->
            if (index > 0 && index % 3 == 0) {
                item { 
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        AdMobPlaceholder()
                    }
                }
            }
            
            item {
                val exercise = exercises[index]
                Card(
                    onClick = { navController.navigate("exercise_detail/$index") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(exercise.emoji, fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${exercise.durationSeconds} seconds", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("▶️", fontSize = 20.sp)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)); AdMobPlaceholder() }
    }
}

@Composable
fun ExerciseDetailScreen(initialIndex: Int) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { exercises.size })
    
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            ExerciseContent(exercises[page])
        }
        
        // Pager Indicators
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(exercises.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
fun ExerciseContent(exercise: Exercise) {
    var timeLeft by remember(exercise) { mutableIntStateOf(exercise.durationSeconds) }
    var isRunning by remember(exercise) { mutableStateOf(false) }

    LaunchedEffect(isRunning, timeLeft, exercise) {
        if (isRunning && timeLeft > 0) {
            delay(1000)
            timeLeft -= 1
        } else if (timeLeft == 0) {
            isRunning = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(exercise.emoji, fontSize = 64.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(exercise.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text(exercise.description, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Steps:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        exercise.steps.forEachIndexed { i, step ->
            Text("${i + 1}. $step", modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            CircularProgressIndicator(
                progress = { timeLeft.toFloat() / exercise.durationSeconds },
                modifier = Modifier.size(150.dp),
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text("$timeLeft", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = { if (timeLeft == 0) timeLeft = exercise.durationSeconds; isRunning = !isRunning },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isRunning) "Pause" else if (timeLeft == 0) "Restart" else "Start Exercise")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        AdMobPlaceholder()
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TipsScreen() {
    val categories = eyeTips.groupBy { it.category }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Eye Care Tips", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        
        var globalIndex = 0
        categories.forEach { (category, tips) ->
            item {
                Text(category, fontWeight = FontWeight.Bold, color = PrimaryBlue, modifier = Modifier.padding(top = 8.dp))
            }
            
            tips.forEach { tip ->
                if (globalIndex > 0 && globalIndex % 3 == 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            AdMobPlaceholder()
                        }
                    }
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Text(tip.emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(tip.title, fontWeight = FontWeight.Bold)
                                Text(tip.description, fontSize = 14.sp)
                            }
                        }
                    }
                }
                globalIndex++
            }
        }
        item { AdMobPlaceholder() }
    }
}

@Composable
fun FactsScreen() {
    val pagerState = rememberPagerState(pageCount = { eyeFacts.size })
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Did You Know? 👁️", fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(40.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { page ->
            Card(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f))
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        eyeFacts[page].text,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Swipe for more", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))
        AdMobPlaceholder()
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel, navController: NavHostController) {
    val interval by viewModel.timerInterval.collectAsState(initial = 20)
    val vibration by viewModel.isVibrationEnabled.collectAsState(initial = true)
    val notificationTone by viewModel.isNotificationToneEnabled.collectAsState(initial = true)
    val voiceNotification by viewModel.isVoiceNotificationEnabled.collectAsState(initial = true)
    val darkMode by viewModel.isDarkModeEnabled.collectAsState(initial = false)
    val automaticWorkHours by viewModel.isAutomaticWorkHours.collectAsState(initial = false)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp)) }
        
        item { SettingItem("Timer Interval", "$interval minutes") { /* Open dialog? */ } }
        item { SettingToggle("Notification Tone", notificationTone) { viewModel.setNotificationToneEnabled(it) } }
        item { SettingToggle("Voice Notification", voiceNotification) { viewModel.setVoiceNotificationEnabled(it) } }
        item { SettingToggle("Vibration", vibration) { viewModel.setVibrationEnabled(it) } }
        item { SettingToggle("Dark Mode", darkMode) { viewModel.setDarkModeEnabled(it) } }
        item { SettingToggle("Automatic Work Hours", automaticWorkHours) { viewModel.setAutomaticWorkHours(it) } }
        item { SettingItem("About App", "") { navController.navigate("about") } }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.resetAllSettings() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Reset All Settings")
            }
        }
    }
}

@Composable
fun SettingItem(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Text("➡️", fontSize = 18.sp)
        }
    }
}

@Composable
fun SettingToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun AboutScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_icon),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Eye Care", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Version 1.4", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Keep your eyes healthy in the digital age with our 20-20-20 rule reminders and eye exercises.",
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = { navController.navigate("privacy") }) { Text("Privacy Policy") }
        TextButton(onClick = { navController.navigate("terms") }) { Text("Terms of Service") }
        TextButton(onClick = { /* Rate Us Intent */ }) { Text("Rate Us on Play Store") }
        
        Spacer(modifier = Modifier.weight(1f))
        Text("Developed by Almosoft", fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun PrivacyPolicyScreen(navController: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⬅️", modifier = Modifier.clickable { navController.popBackStack() }, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text("Privacy Policy", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("1. Information We Collect\nThis app does not collect any personal data. All data stays on your device only.")
        Text("\n2. Data Storage\nWe use localStorage (DataStore) to save app preferences. No data is sent to any server.")
        Text("\n3. Third-Party Services - AdMob\nWe use Google AdMob for ads. AdMob may collect device info as per Google Privacy Policy.")
        Text("\n4. Permissions Used\nINTERNET - AdMob ads only\nVIBRATE - Timer break alerts\nPOST_NOTIFICATIONS - Break reminders")
    }
}

@Composable
fun TermsOfServiceScreen(navController: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⬅️", modifier = Modifier.clickable { navController.popBackStack() }, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text("Terms of Service", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("1. Acceptance of Terms\nBy using this app, you agree to these terms.")
        Text("\n2. Medical Disclaimer\nThis app is for informational purposes only. It is NOT a substitute for professional medical advice. Consult a licensed eye doctor for medical conditions.")
    }
}

fun showTimerNotification(context: Context, isBreak: Boolean) {
    val channelId = "eye_care_reminders"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Eye Care Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders to take 20-20-20 breaks"
        }
        notificationManager.createNotificationChannel(channel)
    }

    val title = if (isBreak) "Break Time! 🌿" else "Back to Work 👨‍💻"
    val content = if (isBreak) "Look 20 feet away for 20 seconds." else "Focus on your tasks."

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(content)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(2, notification) // Use different ID from ReminderWorker
}

@Composable
fun AdMobPlaceholder() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-6221230907726435/1965819402" // Production Banner ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
