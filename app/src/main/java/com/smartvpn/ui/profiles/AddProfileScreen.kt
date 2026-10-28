package com.smartvpn.ui.profiles

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartvpn.data.local.ProfileType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileScreen(
    viewModel: AddProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure VPN Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Export Button
                    IconButton(onClick = {
                        val exportedJson = viewModel.exportCurrentConfig()
                        val clip = ClipData.newPlainText("SmartVPN Config", exportedJson)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Configuration Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export/Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Import from Clipboard Button
            OutlinedButton(
                onClick = {
                    val clipData = clipboardManager.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                        if (viewModel.importFromJsonOrUri(pastedText)) {
                            Toast.makeText(context, "Successfully Imported Config!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid Configuration String", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📥 Import Configuration from Clipboard")
            }

            HorizontalDivider()

            // Profile Name
            OutlinedTextField(
                value = viewModel.profileName,
                onValueChange = { viewModel.profileName = it },
                label = { Text("Profile Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Protocol Selector Dropdown
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = viewModel.selectedType.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Protocol Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onResponse = {},
                    onDismissRequest = { expanded = false }
                ) {
                    ProfileType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                viewModel.selectedType = type
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Dynamic Fields based on Protocol Type
            if (viewModel.selectedType == ProfileType.RAW_JSON) {
                OutlinedTextField(
                    value = viewModel.rawJsonConfig,
                    onValueChange = { viewModel.rawJsonConfig = it },
                    label = { Text("Raw Xray / Sing-box JSON Config") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 15
                )
            } else {
                OutlinedTextField(
                    value = viewModel.serverAddress,
                    onValueChange = { viewModel.serverAddress = it },
                    label = { Text("Server Address (IP or Domain)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = viewModel.serverPort,
                    onValueChange = { viewModel.serverPort = it },
                    label = { Text("Server Port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (viewModel.selectedType == ProfileType.VLESS || viewModel.selectedType == ProfileType.VMESS || viewModel.selectedType == ProfileType.TROJAN) {
                    OutlinedTextField(
                        value = viewModel.uuid,
                        onValueChange = { viewModel.uuid = it },
                        label = { Text("UUID / Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (viewModel.selectedType == ProfileType.SSH) {
                    OutlinedTextField(
                        value = viewModel.username,
                        onValueChange = { viewModel.username = it },
                        label = { Text("SSH Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.password,
                        onValueChange = { viewModel.password = it },
                        label = { Text("SSH Password / Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.customPayload,
                        onValueChange = { viewModel.customPayload = it },
                        label = { Text("HTTP Injection Payload (Optional)") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }

                OutlinedTextField(
                    value = viewModel.sni,
                    onValueChange = { viewModel.sni = it },
                    label = { Text("SNI / Host Header") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = viewModel.enableTls,
                        onCheckedChange = { viewModel.enableTls = it }
                    )
                    Text("Enable TLS / SSL Tunneling")
                }

                if (viewModel.enableTls) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = viewModel.enableTlsFragmentation,
                            onCheckedChange = { viewModel.enableTlsFragmentation = it }
                        )
                        Text("Bypass DPI via TLS Fragmentation")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.saveProfile(onSuccess = { onNavigateBack() })
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Save Profile & Configuration", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
