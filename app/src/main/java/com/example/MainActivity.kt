package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entity.AppSettings
import com.example.data.entity.SmsLog
import com.example.ui.SmsViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.ComponentName
import android.provider.Settings
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

fun isNotificationListenerEnabled(context: Context): Boolean {
    val cn = ComponentName(context, "com.example.data.BankNotificationListenerService")
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(cn.flattenToString())
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalContentColor provides Color.Black) {
                    PayGateApp()
                }
            }
        }
    }
}

data class DrawerNavigationItem(
    val index: Int,
    val icon: String,
    val title: String,
    val subtitle: String
)

@Composable
fun PayGateApp() {
    val context = LocalContext.current
    val viewModel: SmsViewModel = viewModel()
    val settings by viewModel.settings.collectAsState()
    val logs by viewModel.allLogs.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Dynamic Permission Checklist
    var hasSmsPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var hasNotificationAccess by remember {
        mutableStateOf(isNotificationListenerEnabled(context))
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = isNotificationListenerEnabled(context)
                hasSmsPermissions = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermissions = permissions[Manifest.permission.RECEIVE_SMS] == true &&
                permissions[Manifest.permission.READ_SMS] == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        }
    }

    // Auto request permissions on first start
    LaunchedEffect(Unit) {
        val list = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        launcher.launch(list.toTypedArray())
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFFDF8F6),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(310.dp)
            ) {
                // Header Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEADDFF))
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛡️", fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "PayGate Forwarder",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D)
                    )
                    Text(
                        text = "ส่งต่อ SMS & การแจ้งเตือนปลอดภัย",
                        fontSize = 12.sp,
                        color = Color(0xFF21005D).copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isAnyServiceActive = settings.isSmsActive || settings.isNotificationActive
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isAnyServiceActive) Color(0xFF4CAF50) else Color(0xFFFF9800))
                        )
                        Text(
                            text = if (isAnyServiceActive) "ระบบส่งต่อเปิดอยู่" else "ระบบทั้งหมดปิดอยู่",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAnyServiceActive) Color(0xFF1B5E20) else Color(0xFFE65100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Drawer Items
                Text(
                    text = "เมนูควบคุมแอปพลิเคชัน",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                val menuItems = listOf(
                    DrawerNavigationItem(0, "🏠", "หน้าจอ Monitor", "สถานะและการทำงานล่าสุด"),
                    DrawerNavigationItem(1, "📝", "ประวัติการส่งต่อ", "ข้อมูล SMS และการเรียกใช้ Webhook"),
                    DrawerNavigationItem(2, "📖", "คู่มือการเชื่อมต่อ Webhook", "รายละเอียด Payload และความปลอดภัย"),
                    DrawerNavigationItem(3, "⚙️", "ตั้งค่าระบบ (Settings)", "กำหนดค่า URL และ HTTP Token")
                )

                menuItems.forEach { item ->
                    val isSelected = selectedTab == item.index
                    NavigationDrawerItem(
                        icon = { Text(item.icon, fontSize = 20.sp) },
                        label = {
                            Column {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color(0xFF21005D) else Color.Black
                                )
                                Text(
                                    text = item.subtitle,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color(0xFF21005D).copy(alpha = 0.7f) else Color.Gray
                                )
                            }
                        },
                        selected = isSelected,
                        onClick = {
                            selectedTab = item.index
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFEADDFF),
                            unselectedContainerColor = Color.Transparent,
                            selectedIconColor = Color.Black,
                            unselectedIconColor = Color.Black,
                            selectedTextColor = Color.Black,
                            unselectedTextColor = Color.Black
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = Color.LightGray.copy(alpha = 0.5f))

                // Footer version info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PayGate Forwarder v1.1.0 🛡️",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ระบบส่งต่อและเชื่อมต่อ Webhook อัจฉริยะ",
                        fontSize = 8.sp,
                        color = Color.Gray.copy(alpha = 0.8f)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFFDF8F6),
            topBar = {
                Column(
                    modifier = Modifier
                        .background(Color(0xFFFDF8F6))
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // ปุ่มเมนู 3 ขีดสำหรับเปิด Drawer
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEADDFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "เปิดเมนูด้านข้าง",
                                    tint = Color(0xFF21005D)
                                )
                            }

                            Column {
                                Text(
                                    text = "SYSTEM SECURE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "PayGate Forwarder",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEADDFF))
                                .clickable {
                                    Toast.makeText(context, "ระบบส่งต่อปลอดภัย 🛡️ ผ่านการตรวจสอบแล้ว", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val isAnyServiceActive = settings.isSmsActive || settings.isNotificationActive
                            Text(
                                text = if (isAnyServiceActive) "🛡️" else "⚠️",
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            color = Color(0xFFFDF8F6)
        ) {
            when (selectedTab) {
                0 -> LogsScreen(
                    logs = logs,
                    settings = settings,
                    hasSmsPermissions = hasSmsPermissions,
                    hasNotificationAccess = hasNotificationAccess,
                    onResend = { log ->
                        viewModel.resendSms(log) { success, message ->
                            val text = if (success) "ส่งต่อ SMS อีกครั้งสำเร็จ!" else "ส่งต่อล้มเหลว: $message"
                            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClearAll = {
                        viewModel.clearLogs()
                    },
                    onToggleSmsService = { checked ->
                        viewModel.saveSettings(settings.copy(isSmsActive = checked))
                    },
                    onToggleNotificationService = { checked ->
                        viewModel.saveSettings(settings.copy(isNotificationActive = checked))
                    },
                    onNavigateToSettings = {
                        selectedTab = 3
                    },
                    onNavigateToHistory = {
                        selectedTab = 1
                    },
                    onRequestPermissions = {
                        val list = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            list.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        launcher.launch(list.toTypedArray())
                    },
                    onRequestNotificationAccess = {
                        try {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (ex: Exception) {
                                Toast.makeText(context, "ไม่สามารถเปิดตั้งค่าได้ กรุณาเปิดสิทธิ์ด้วยตนเองในการตั้งค่ามือถือ", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
                1 -> HistoryScreen(
                    logs = logs,
                    onResend = { log ->
                        viewModel.resendSms(log) { success, message ->
                            val text = if (success) "ส่งต่อ SMS อีกครั้งสำเร็จ!" else "ส่งต่อล้มเหลว: $message"
                            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClearAll = {
                        viewModel.clearLogs()
                    }
                )
                2 -> DocsScreen(
                    settings = settings
                )
                3 -> SettingsScreen(
                    settings = settings,
                    onSaveSettings = { updatedSettings ->
                        viewModel.saveSettings(updatedSettings)
                        Toast.makeText(context, "บันทึกข้อมูลตั้งค่าเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
}

@Composable
fun LogsScreen(
    logs: List<SmsLog>,
    settings: AppSettings,
    hasSmsPermissions: Boolean,
    hasNotificationAccess: Boolean,
    onResend: (SmsLog) -> Unit,
    onClearAll: () -> Unit,
    onToggleSmsService: (Boolean) -> Unit,
    onToggleNotificationService: (Boolean) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestNotificationAccess: () -> Unit
) {
    var selectedLogForDetail by remember { mutableStateOf<SmsLog?>(null) }
    val successCount = remember(logs) { logs.count { it.status == "SUCCESS" } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Permission warning card as a bento item
        if (!hasSmsPermissions) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2B8B5)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF410002).copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "คำเตือนสิทธิ์",
                            tint = Color(0xFF410002),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ต้องการสิทธิ์การอ่าน SMS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF410002)
                            )
                            Text(
                                text = "แอปจะไม่สามารถทำงานเบื้องหลังหรือส่งต่อข้อความได้จนกว่าคุณจะอนุญาต",
                                fontSize = 11.sp,
                                color = Color(0xFF410002).copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF410002)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("grant_permission_button")
                        ) {
                            Text("อนุญาต", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Notification Listener permission warning card as a bento item
        if (!hasNotificationAccess) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE082)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "เตือนสิทธิ์การเข้าถึงการแจ้งเตือน",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ต้องการสิทธิ์การเข้าถึงการแจ้งเตือน",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "กรุณาเปิดสิทธิ์ 'PayGate Notification Listener' เพื่อดักจับแจ้งเตือนเงินเข้าจากแอปธนาคารทุกธนาคารโดยตรงอัตโนมัติ",
                                fontSize = 11.sp,
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestNotificationAccess,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("grant_notification_permission_button"),
                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.3f))
                        ) {
                            Text("เปิดสิทธิ์", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Row 1: Main Status Card (Spans full width)
        item {
            val isAnyActive = settings.isSmsActive || settings.isNotificationActive
            MainStatusCard(
                isServiceActive = isAnyActive,
                successCount = successCount
            )
        }

        // Row 2: 2 columns side-by-side
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DestinationCard(
                        webhookUrl = settings.webhookUrl,
                        onClick = onNavigateToSettings
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SystemStatusCard(
                        isSmsActive = settings.isSmsActive,
                        isNotificationActive = settings.isNotificationActive,
                        hasPermissions = hasSmsPermissions,
                        hasNotificationAccess = hasNotificationAccess,
                        onToggleSmsService = onToggleSmsService,
                        onToggleNotificationService = onToggleNotificationService,
                        onRequestPermissions = onRequestPermissions,
                        onRequestNotificationAccess = onRequestNotificationAccess
                    )
                }
            }
        }

        // Row 3: Recent Activity list header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RECENT ACTIVITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val isAnyActive = settings.isSmsActive || settings.isNotificationActive
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isAnyActive) Color(0xFF34A853) else Color(0xFFCAC4D0))
                    )
                }
                if (logs.isNotEmpty()) {
                    TextButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("view_all_history_button")
                    ) {
                        Text(
                            text = "ดูทั้งหมด ➔",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6750A4)
                        )
                    }
                }
            }
        }

        // Empty state or top 3 list of logs
        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = "ไม่มีข้อมูล",
                            tint = Color(0xFF49454F).copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ไม่มีประวัติการส่งต่อ SMS",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SMS ที่ได้รับและผ่านตัวกรองจะแสดงขึ้นเมื่อจัดส่งเสร็จสิ้น",
                            textAlign = TextAlign.Center,
                            color = Color(0xFF49454F).copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            val recentLogs = logs.take(3)
            items(recentLogs, key = { it.id }) { log ->
                LogItemCard(
                    log = log,
                    onClick = { selectedLogForDetail = log },
                    onResend = { onResend(log) }
                )
            }
        }
    }

    selectedLogForDetail?.let { log ->
        LogDetailDialog(
            log = log,
            onDismiss = { selectedLogForDetail = null },
            onResend = {
                onResend(log)
                selectedLogForDetail = null
            }
        )
    }
}

@Composable
fun MainStatusCard(
    isServiceActive: Boolean,
    successCount: Int
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD1E4FF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡", fontSize = 20.sp)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isServiceActive) Color.White else Color.White.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isServiceActive) Color(0xFF34A853) else Color(0xFFCAC4D0))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isServiceActive) "LIVE MONITORING" else "PAUSED",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Column {
                Text(
                    text = "ส่งต่อสำเร็จในวันนี้ (SMS)",
                    color = Color.Black.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$successCount",
                    color = Color.Black,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
            }
        }
    }
}

@Composable
fun DestinationCard(
    webhookUrl: String,
    onClick: () -> Unit
) {
    val displayUrl = if (webhookUrl.isBlank()) "ไม่มี URL ปลายทาง" else webhookUrl
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8DEF8)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🔗", fontSize = 18.sp)
                Text(
                    text = "จุดหมายปลายทาง",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Text(
                text = displayUrl,
                fontSize = 11.sp,
                color = Color.Black.copy(alpha = 0.8f),
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SystemStatusCard(
    isSmsActive: Boolean,
    isNotificationActive: Boolean,
    hasPermissions: Boolean,
    hasNotificationAccess: Boolean,
    onToggleSmsService: (Boolean) -> Unit,
    onToggleNotificationService: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestNotificationAccess: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2B8B5)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🛡️", fontSize = 16.sp)
                Text(
                    text = "เปิด/ปิดระบบแยกส่วน",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // SMS Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ดักจับ SMS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (!hasPermissions) {
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f))
                        ) {
                            Text("สิทธิ์", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Switch(
                            checked = isSmsActive,
                            onCheckedChange = onToggleSmsService,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFF2B8B5),
                                checkedTrackColor = Color(0xFF410002),
                                uncheckedThumbColor = Color(0xFF410002),
                                uncheckedTrackColor = Color(0xFFF2B8B5).copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("sms_active_switch")
                        )
                    }
                }

                // Notification Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ดักแจ้งเตือน",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (!hasNotificationAccess) {
                        Button(
                            onClick = onRequestNotificationAccess,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f))
                        ) {
                            Text("สิทธิ์", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Switch(
                            checked = isNotificationActive,
                            onCheckedChange = onToggleNotificationService,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFF2B8B5),
                                checkedTrackColor = Color(0xFF410002),
                                uncheckedThumbColor = Color(0xFF410002),
                                uncheckedTrackColor = Color(0xFFF2B8B5).copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("notification_active_switch")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogItemCard(
    log: SmsLog,
    onClick: () -> Unit,
    onResend: () -> Unit
) {
    val statusColor = when (log.status) {
        "SUCCESS" -> Color(0xFF34A853)
        "FAILED" -> Color(0xFFBA1A1A)
        "PENDING" -> Color(0xFFFF9800)
        "FILTERED" -> Color(0xFF79747E)
        "SERVICE_INACTIVE" -> Color(0xFF607D8B)
        else -> Color(0xFF79747E)
    }

    val statusText = when (log.status) {
        "SUCCESS" -> "สำเร็จ"
        "FAILED" -> "ล้มเหลว"
        "PENDING" -> "กำลังส่ง..."
        "FILTERED" -> "ตัวกรองข้าม"
        "SERVICE_INACTIVE" -> "ระบบปิดอยู่"
        else -> log.status
    }

    val statusIcon = when (log.status) {
        "SUCCESS" -> Icons.Default.CheckCircle
        "FAILED" -> Icons.Default.Error
        "PENDING" -> Icons.Default.Refresh
        "FILTERED" -> Icons.Default.FilterList
        "SERVICE_INACTIVE" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }

    val bgEmoji = when {
        log.sender.contains("KBANK", ignoreCase = true) || log.sender.contains("K-BANK", ignoreCase = true) -> "🏦"
        log.sender.contains("SCB", ignoreCase = true) || log.sender.contains("SCB", ignoreCase = true) -> "💳"
        else -> "✉️"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("log_item_card_${log.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFDF8F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = bgEmoji, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = log.sender,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1C1B1F)
                        )
                        Text(
                            text = formatTimestamp(log.timestamp),
                            fontSize = 10.sp,
                            color = Color(0xFF49454F).copy(alpha = 0.7f)
                        )
                    }
                }

                // Status tag
                Text(
                    text = statusText.uppercase(),
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = log.message,
                fontSize = 12.sp,
                color = Color(0xFF1C1B1F).copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            if (log.status == "FAILED" || log.status == "SUCCESS") {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (log.responseCode != null) "Response: ${log.responseCode}" else "No response body",
                        fontSize = 10.sp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                    
                    Text(
                        text = "ดูรายละเอียด",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSaveSettings: (AppSettings) -> Unit
) {
    var webhookUrl by remember { mutableStateOf(settings.webhookUrl) }
    var authHeaderName by remember { mutableStateOf(settings.authHeaderName) }
    var authHeaderValue by remember { mutableStateOf(settings.authHeaderValue) }
    var senderFilter by remember { mutableStateOf(settings.senderFilter) }
    var keywordFilter by remember { mutableStateOf(settings.keywordFilter) }
    var trackedBanks by remember { mutableStateOf(settings.trackedBanks) }
    var onlyForwardTrackedBanks by remember { mutableStateOf(settings.onlyForwardTrackedBanks) }

    val bankAppsList = remember {
        listOf(
            Triple("com.kasikorn.kplus", "ธนาคารกสิกรไทย (K PLUS)", Color(0xFF00A34F)),
            Triple("com.scb.phone", "ธนาคารไทยพาณิชย์ (SCB EASY)", Color(0xFF4E2A84)),
            Triple("th.co.krungthaibank.next", "ธนาคารกรุงไทย (Krungthai NEXT)", Color(0xFF00A1F1)),
            Triple("com.bualuang.mbanking", "ธนาคารกรุงเทพ (Bualuang mBanking)", Color(0xFF0038A8)),
            Triple("kr.co.krungsri.kma", "ธนาคารกรุงศรีอยุธยา (KMA)", Color(0xFF7A6B58)),
            Triple("com.ttbbank.oneapp", "ธนาคารทหารไทยธนชาต (ttb touch)", Color(0xFFFF5000)),
            Triple("th.or.gsb.mymo", "ธนาคารออมสิน (MyMo)", Color(0xFFEC008C)),
            Triple("th.co.truemoney.wallet", "ทรูมันนี่ วอลเล็ท (TrueMoney)", Color(0xFFFF8200))
        )
    }

    val selectedBanksList = remember(trackedBanks) {
        trackedBanks.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    }

    val onToggleBank: (String) -> Unit = { pkg ->
        val currentSet = trackedBanks.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toMutableSet()
        if (currentSet.contains(pkg.lowercase())) {
            currentSet.remove(pkg.lowercase())
        } else {
            currentSet.add(pkg.lowercase())
        }
        trackedBanks = currentSet.joinToString(",")
    }

    // Update state when settings updates from DB
    LaunchedEffect(settings) {
        webhookUrl = settings.webhookUrl
        authHeaderName = settings.authHeaderName
        authHeaderValue = settings.authHeaderValue
        senderFilter = settings.senderFilter
        keywordFilter = settings.keywordFilter
        trackedBanks = settings.trackedBanks
        onlyForwardTrackedBanks = settings.onlyForwardTrackedBanks
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    text = "SERVER WEBHOOK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        label = { Text("Webhook URL ปลายทาง") },
                        placeholder = { Text("https://api.domain.com/sms") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("webhook_url_input"),
                        leadingIcon = { Icon(imageVector = Icons.Default.Link, contentDescription = "URL", tint = Color(0xFF6750A4)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ระบบจะส่งข้อมูล SMS แบบ HTTP POST ไปยัง URL นี้ในรูปแบบ JSON Payload",
                        fontSize = 10.sp,
                        color = Color(0xFF49454F).copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = authHeaderName,
                        onValueChange = { authHeaderName = it },
                        label = { Text("ชื่อ Header สำหรับรหัสผ่าน (Auth Header)") },
                        placeholder = { Text("Authorization หรือ X-Api-Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = "Header Name", tint = Color(0xFF6750A4)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = authHeaderValue,
                        onValueChange = { authHeaderValue = it },
                        label = { Text("รหัสผ่านความปลอดภัย (Header Value)") },
                        placeholder = { Text("Bearer secret_token_xxx") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = "Header Value", tint = Color(0xFF6750A4)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F)
                        )
                    )
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    text = "BANK NOTIFICATION TRACKER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ดักรับเฉพาะแจ้งเตือนของแอปธนาคารจริงๆ เท่านั้น",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "เมื่อเปิดใช้งาน ระบบจะกรองและอนุญาตเฉพาะแอปพลิเคชันการเงินที่ผ่านการเลือกในรายการด้านล่างเพื่อป้องกันการดักจับข้อความไม่พึงประสงค์",
                                fontSize = 11.sp,
                                color = Color(0xFF49454F).copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = onlyForwardTrackedBanks,
                            onCheckedChange = { onlyForwardTrackedBanks = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6750A4),
                                uncheckedThumbColor = Color(0xFF6750A4),
                                uncheckedTrackColor = Color(0xFFCAC4D0).copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "เลือกธนาคารในเครื่องที่คุณต้องการติดตาม:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    bankAppsList.forEach { (pkg, name, color) ->
                        val isSelected = selectedBanksList.contains(pkg.lowercase())
                        val initial = name.replace("ธนาคาร", "").trim().take(1)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleBank(pkg) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initial,
                                        color = color,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = pkg,
                                        fontSize = 10.sp,
                                        color = Color(0xFF49454F).copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Switch(
                                checked = isSelected,
                                onCheckedChange = { onToggleBank(pkg) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = color,
                                    uncheckedThumbColor = color.copy(alpha = 0.6f),
                                    uncheckedTrackColor = color.copy(alpha = 0.1f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = trackedBanks,
                        onValueChange = { trackedBanks = it },
                        label = { Text("รายการแอปธนาคารที่ติดตาม (คั่นด้วยจุลภาค ,)") },
                        placeholder = { Text("com.kasikorn.kplus,com.scb.phone") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F)
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "รายการชื่อแพ็กเกจของแอปธนาคารหรือแอปกระเป๋าเงินอิเล็กทรอนิกส์ทั้งหมดที่คุณยินยอมให้ติดตามแจ้งเตือน",
                        fontSize = 10.sp,
                        color = Color(0xFF49454F).copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    text = "MESSAGE FILTER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = senderFilter,
                        onValueChange = { senderFilter = it },
                        label = { Text("กรองเบอร์ผู้ส่ง (Sender)") },
                        placeholder = { Text("SCB, KBANK, 021234567") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ระบุชื่อหรือเบอร์ผู้ส่งที่ต้องการรับ เช่น SCB,KBANK (ปล่อยว่างเพื่อส่งต่อทั้งหมด)",
                        fontSize = 10.sp,
                        color = Color(0xFF49454F).copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = keywordFilter,
                        onValueChange = { keywordFilter = it },
                        label = { Text("กรองคีย์เวิร์ดในข้อความ (Message Keywords)") },
                        placeholder = { Text("ได้รับเงิน, ยอดเงินเข้า, โอนเงิน, OTP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ต้องมีคำสำคัญเหล่านี้อย่างน้อยหนึ่งคำในข้อความ จึงจะทำการส่งต่อ (ปล่อยว่างเพื่อส่งต่อทั้งหมด)",
                        fontSize = 10.sp,
                        color = Color(0xFF49454F).copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    onSaveSettings(
                        settings.copy(
                            webhookUrl = webhookUrl.trim(),
                            authHeaderName = authHeaderName.trim(),
                            authHeaderValue = authHeaderValue.trim(),
                            senderFilter = senderFilter.trim(),
                            keywordFilter = keywordFilter.trim(),
                            trackedBanks = trackedBanks.trim(),
                            onlyForwardTrackedBanks = onlyForwardTrackedBanks
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_settings_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8DEF8), contentColor = Color.Black),
                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f))
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "บันทึก")
                Spacer(modifier = Modifier.width(8.dp))
                Text("บันทึกข้อมูลตั้งค่าระบบ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun LogDetailDialog(
    log: SmsLog,
    onDismiss: () -> Unit,
    onResend: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val jsonPayload = remember(log) {
        """
        {
          "sender": "${log.sender}",
          "message": "${log.message.replace("\"", "\\\"").replace("\n", "\\n")}",
          "timestamp": ${log.timestamp},
          "device": "Simulated or Real (Android OS)"
        }
        """.trimIndent()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "รายละเอียดการส่งต่อ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Error, contentDescription = "ปิด", tint = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Sms, contentDescription = "ผู้ส่ง", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "ผู้ส่ง: ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = log.sender, fontSize = 13.sp)
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.History, contentDescription = "เวลา", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "วันที่และเวลา: ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = formatTimestamp(log.timestamp), fontSize = 13.sp)
                        }
                    }

                    item {
                        Column {
                            Text(text = "ข้อความ SMS เต็ม:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = log.message,
                                    modifier = Modifier.padding(10.dp),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "JSON Payload ที่ถูกส่งไป:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(jsonPayload))
                                        Toast.makeText(context, "คัดลอก JSON แล้ว", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "คัดลอก", modifier = Modifier.size(14.dp))
                                }
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = jsonPayload,
                                    modifier = Modifier.padding(10.dp),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    item {
                        Column {
                            Text(
                                text = "สถานะตอบกลับเซิร์ฟเวอร์ (Response):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (log.status == "SUCCESS") Color(0xFF4CAF50) else Color(0xFFF44336))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "HTTP Code: ${log.responseCode ?: "ไม่มีข้อมูล"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = log.responseBody ?: "(ไม่มีข้อมูลการตอบกลับจากเซิร์ฟเวอร์)",
                                    modifier = Modifier.padding(10.dp),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ปิดหน้าต่าง", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    if (log.status == "FAILED" || log.status == "SUCCESS") {
                        Button(
                            onClick = onResend,
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "ส่งอีกครั้ง", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ลองส่งต่อใหม่", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    logs: List<SmsLog>,
    onResend: (SmsLog) -> Unit,
    onClearAll: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, SUCCESS, FAILED, FILTERED
    var selectedLogForDetail by remember { mutableStateOf<SmsLog?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, searchQuery, selectedFilter) {
        logs.filter { log ->
            // Match search
            val matchesSearch = log.sender.contains(searchQuery, ignoreCase = true) ||
                    log.message.contains(searchQuery, ignoreCase = true)
            // Match filter
            val matchesFilter = when (selectedFilter) {
                "SUCCESS" -> log.status == "SUCCESS"
                "FAILED" -> log.status == "FAILED"
                "FILTERED" -> log.status == "FILTERED" || log.status == "SERVICE_INACTIVE"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val successCount = remember(logs) { logs.count { it.status == "SUCCESS" } }
    val failedCount = remember(logs) { logs.count { it.status == "FAILED" } }
    val filteredCount = remember(logs) { logs.count { it.status == "FILTERED" || it.status == "SERVICE_INACTIVE" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(
                text = "ประวัติการส่งต่อ SMS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("ค้นหาชื่อผู้ส่ง หรือข้อความ") },
            placeholder = { Text("เช่น KBank, SCB, ได้รับโอน...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("history_search_input"),
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "ค้นหา", tint = Color.Black.copy(alpha = 0.5f)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "ล้าง", tint = Color.Black.copy(alpha = 0.5f))
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color(0xFFCAC4D0),
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color(0xFF49454F)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filters pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                Pair("ALL", "ทั้งหมด (${logs.size})"),
                Pair("SUCCESS", "สำเร็จ ($successCount)"),
                Pair("FAILED", "ล้มเหลว ($failedCount)"),
                Pair("FILTERED", "ข้ามตัวกรอง ($filteredCount)")
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { item ->
                    val isSelected = selectedFilter == item.first
                    val bg = if (isSelected) Color.Black else Color.White
                    val tc = if (isSelected) Color.White else Color.Black
                    val border = if (isSelected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, Color(0xFFCAC4D0))

                    Card(
                        shape = RoundedCornerShape(50),
                        border = border,
                        colors = CardDefaults.cardColors(containerColor = bg),
                        modifier = Modifier
                            .clickable { selectedFilter = item.first }
                            .padding(vertical = 4.dp)
                            .testTag("filter_chip_${item.first}")
                    ) {
                        Text(
                            text = item.second,
                            color = tc,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Clear Logs Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "รายการบันทึก (${filteredLogs.size} รายการ)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.6f)
            )
            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = { showClearConfirmation = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBA1A1A)),
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "ล้างทั้งหมด", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ล้างประวัติทั้งหมด", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = "ไม่มีข้อมูล",
                                tint = Color(0xFF49454F).copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ไม่พบบันทึกตามเงื่อนไข",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ลองค้นหาด้วยคำอื่นหรือเปลี่ยนแท็บตัวกรองด้านบน",
                                textAlign = TextAlign.Center,
                                color = Color(0xFF49454F).copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    LogItemCard(
                        log = log,
                        onClick = { selectedLogForDetail = log },
                        onResend = { onResend(log) }
                    )
                }
            }
        }
    }

    if (showClearConfirmation) {
        Dialog(onDismissRequest = { showClearConfirmation = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ล้างประวัติการส่งต่อ?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "คุณแน่ใจหรือไม่ว่าต้องการลบข้อมูลประวัติการรับส่ง SMS ทั้งหมดในระบบ? การกระทำนี้ไม่สามารถย้อนกลับได้",
                        fontSize = 13.sp,
                        color = Color.Black.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showClearConfirmation = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3EDF7), contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ยกเลิก", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = {
                                onClearAll()
                                showClearConfirmation = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A), contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ลบทั้งหมด", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    selectedLogForDetail?.let { log ->
        LogDetailDialog(
            log = log,
            onDismiss = { selectedLogForDetail = null },
            onResend = {
                onResend(log)
                selectedLogForDetail = null
            }
        )
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale("th", "TH"))
    return sdf.format(Date(timestamp))
}

@Composable
fun DocsScreen(settings: AppSettings) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val exampleJson = """{
  "sender": "KBank",
  "message": "คุณได้รับโอนเงินจำนวน 3,500.00 บาท จาก นายสมชาย เพื่อจ่ายค่าบริการอาหารและเครื่องดื่ม"
}"""

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "📖 คู่มือการเชื่อมต่อ Webhook & API",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "รายละเอียดข้อมูลทางเทคนิคและโครงสร้างข้อมูลในการรับข้อมูลแจ้งเตือน SMS หรือแจ้งเตือนจากธนาคารต่างๆ ส่งต่อไปยัง API หลังบ้านของคุณ",
                        fontSize = 12.sp,
                        color = Color(0xFF21005D).copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Section 1: Endpoint Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🔗 Webhook URL ปลายทาง",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF4F4F4))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = settings.webhookUrl.ifBlank { "(ยังไม่ได้ตั้งค่า)" },
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (settings.webhookUrl.isBlank()) Color.Gray else Color(0xFF007A5A)
                        )
                    }

                    if (settings.webhookUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(settings.webhookUrl))
                                    Toast.makeText(context, "คัดลอก URL เรียบร้อยแล้ว!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Black,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "คัดลอก URL", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("คัดลอก URL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Headers Config
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🔑 ความปลอดภัย (HTTP Header)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "แอปจะส่งพารามิเตอร์ Token ยืนยันตัวตนในส่วนหัว HTTP Header ทุกครั้ง เพื่อความปลอดภัยของระบบปลายทาง",
                        fontSize = 11.sp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Header Name", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF4F4F4))
                                    .padding(8.dp)
                            ) {
                                Text(settings.authHeaderName.ifBlank { "X-SMS-Token" }, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }
                        }

                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("Header Value / Token", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF4F4F4))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = settings.authHeaderValue.ifBlank { "(ไม่มี)" },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (settings.authHeaderValue.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(settings.authHeaderValue))
                                    Toast.makeText(context, "คัดลอก Token เรียบร้อยแล้ว!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Black,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "คัดลอก Token", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("คัดลอก Token", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Payload Format
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📦 รูปแบบ Payload (HTTP POST JSON)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE8DEF8))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("POST", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ข้อมูลจะถูกส่งเป็นเนื้อหาแบบ JSON โดยมี Key สำคัญ 2 ตัว ดังนี้:",
                        fontSize = 11.sp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Code block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E1E))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("JSON Payload", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text("UTF-8", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = exampleJson,
                                color = Color(0xFF9CDCFE),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(exampleJson))
                                Toast.makeText(context, "คัดลอกรูปแบบ JSON เรียบร้อยแล้ว!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "คัดลอก JSON", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("คัดลอก JSON ตัวอย่าง", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 4: Workflow
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🛠️ คำแนะนำในการพัฒนาและทดสอบ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val bullets = listOf(
                        "• เพื่อผลลัพธ์ที่ดีที่สุด: Webhook Server ควรตอบกลับรหัส HTTP Status Code ในช่วง 200 - 299 เพื่อยืนยันว่าได้รับข้อมูลแล้ว",
                        "• บันทึกส่งซ้ำ (Resend): หากเว็บเซิร์ฟเวอร์ขัดข้องชั่วคราว คุณสามารถมาที่แท็บ \"ประวัติ\" แล้วกดคลิกการบันทึกเพื่อกดปุ่ม \"ส่งอีกครั้ง\" แมนนวลได้เลย",
                        "• ตัวจำลอง (Simulator): คุณสามารถใช้แท็บ ⚡ Simulator ด้านขวาสุดในการใส่ข้อมูลตัวอย่าง เพื่อทดสอบส่งสัญญาณเข้าสู่ API ของคุณโดยไม่ต้องโอนเงินจริง"
                    )

                    bullets.forEach { text ->
                        Text(
                            text = text,
                            fontSize = 11.5.sp,
                            color = Color.Black.copy(alpha = 0.75f),
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
