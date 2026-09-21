package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLog
import com.example.data.model.LendingDocument
import com.example.ui.theme.DeetrendBackground
import com.example.ui.theme.DeetrendOrange
import com.example.ui.theme.DeetrendTeal
import com.example.ui.theme.DeetrendTealContainer
import com.example.ui.theme.DeetrendTealDark
import com.example.ui.theme.DeetrendTextPrimary
import com.example.ui.theme.DeetrendTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditTrailScreen(
    auditLogs: List<AuditLog>,
    documents: List<LendingDocument>,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Security Audit & Documents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeetrendTextPrimary
                        )
                        Text(
                            text = "Immutable financial lifecycle logs & file repository",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeetrendTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_audit_back")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeetrendTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeetrendBackground)
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = DeetrendTeal
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Audit Trail (${auditLogs.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_audit_trail")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Document Vault (${documents.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_document_vault")
                )
            }

            if (selectedTab == 0) {
                // Audit Trail Tab
                if (auditLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No audit events recorded yet.", color = DeetrendTextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(auditLogs) { log ->
                            AuditLogCard(log = log, dateFormatter = dateFormatter)
                        }
                    }
                }
            } else {
                // Document Vault Tab
                if (documents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No lending documents archived yet.", color = DeetrendTextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(documents) { doc ->
                            DocumentRecordCard(doc = doc, dateFormatter = dateFormatter)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogCard(
    log: AuditLog,
    dateFormatter: SimpleDateFormat
) {
    val (actionBg, actionFg) = when {
        log.action.contains("Approved", ignoreCase = true) || log.action.contains("Consented", ignoreCase = true) ->
            Pair(Color(0xFFD1FAE5), Color(0xFF065F46))
        log.action.contains("Rejected", ignoreCase = true) ->
            Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
        log.action.contains("Repayment", ignoreCase = true) ->
            Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
        log.action.contains("Generated", ignoreCase = true) || log.action.contains("Submitted", ignoreCase = true) ->
            Pair(DeetrendTealContainer, DeetrendTealDark)
        else ->
            Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = actionBg
                ) {
                    Text(
                        text = log.action,
                        color = actionFg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = dateFormatter.format(Date(log.timestamp)),
                    fontSize = 11.sp,
                    color = DeetrendTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Actor: ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeetrendTextPrimary
                )
                Text(
                    text = log.user_name,
                    fontSize = 12.sp,
                    color = DeetrendTealDark,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Record: ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeetrendTextPrimary
                )
                Text(
                    text = log.record_affected,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeetrendTextPrimary
                )
            }

            if (log.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF8FAFA),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = log.details,
                        fontSize = 12.sp,
                        color = DeetrendTextSecondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentRecordCard(
    doc: LendingDocument,
    dateFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = DeetrendTealContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (doc.document_type.contains("Receipt", ignoreCase = true)) Icons.Default.Receipt else Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        tint = DeetrendTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DeetrendTextPrimary
                )
                Text(
                    text = "${doc.document_type} • Ref: ${doc.file_reference}",
                    fontSize = 11.sp,
                    color = DeetrendTextSecondary
                )
                Text(
                    text = "Archived: ${dateFormatter.format(Date(doc.created_at))}",
                    fontSize = 10.sp,
                    color = DeetrendTealDark
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFD1FAE5)
            ) {
                Text(
                    text = "Verified",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF065F46),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
