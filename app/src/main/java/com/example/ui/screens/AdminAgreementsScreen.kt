package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConsentStatus
import com.example.data.model.LoanAgreement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminAgreementsScreen(
    agreements: List<LoanAgreement>,
    onOpenBorrowerLink: (token: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var viewingAgreement by remember { mutableStateOf<LoanAgreement?>(null) }
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US) }

    val filteredAgreements = agreements.filter { ag ->
        searchQuery.isBlank() ||
                ag.agreement_id.contains(searchQuery, ignoreCase = true) ||
                ag.loan_id.contains(searchQuery, ignoreCase = true) ||
                ag.borrower_name.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("admin_agreements_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_agreements_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Loan Agreements",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Legal promissory notes and borrower consent links",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Agreement ID, Loan ID, Borrower...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_agreements_field")
                )
            }

            // Empty State
            if (filteredAgreements.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No agreements generated yet. Open a loan in Portfolio to generate its agreement.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Agreement Cards
            items(filteredAgreements, key = { it.id }) { ag ->
                val simulatedUrl = "https://deetrend.enterprise.ng/agreements/${ag.secure_token}"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewingAgreement = ag }
                        .testTag("agreement_card_${ag.agreement_id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = ag.agreement_id,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            ConsentStatusBadge(ag.consent_status)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Borrower: ${ag.borrower_name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Loan Reference: ${ag.loan_id} • Created: ${dateFormatter.format(Date(ag.created_at))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (ag.consent_status == ConsentStatus.Consented && ag.consent_timestamp != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Color(0xFFD1FAE5),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF065F46), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Signed by: ${ag.signature_consent_data} on ${dateFormatter.format(Date(ag.consent_timestamp))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF065F46),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions: View Document, Open Public Link, Copy Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewingAgreement = ag },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Document")
                            }

                            Button(
                                onClick = { onOpenBorrowerLink(ag.secure_token) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Borrower Link")
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Deetrend Agreement Link", simulatedUrl)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Agreement Link Copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("copy_link_${ag.agreement_id}")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link")
                            }
                        }
                    }
                }
            }
        }

        // View Agreement Legal Document Modal
        viewingAgreement?.let { ag ->
            AlertDialog(
                onDismissRequest = { viewingAgreement = null },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Agreement: ${ag.agreement_id}")
                        ConsentStatusBadge(ag.consent_status)
                    }
                },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = ag.agreement_content,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = {
                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                                val printDocName = "Deetrend_Agreement_${ag.agreement_id}"
                                val webView = WebView(context)
                                val htmlContent = """
                                    <html>
                                    <head>
                                        <style>
                                            body { font-family: 'Courier New', monospace; font-size: 12px; margin: 24px; color: #111; line-height: 1.4; }
                                            pre { white-space: pre-wrap; word-wrap: break-word; }
                                        </style>
                                    </head>
                                    <body>
                                        <pre>${ag.agreement_content}</pre>
                                    </body>
                                    </html>
                                """.trimIndent()
                                webView.webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        val printAdapter = webView.createPrintDocumentAdapter(printDocName)
                                        printManager?.print(printDocName, printAdapter, PrintAttributes.Builder().build())
                                    }
                                }
                                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                            }) {
                                Icon(Icons.Default.Print, contentDescription = "Print / Export PDF", tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Deetrend Loan Agreement: ${ag.agreement_id}")
                                    putExtra(Intent.EXTRA_TEXT, ag.agreement_content)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Agreement & Promissory Note"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share Document", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val token = ag.secure_token
                                viewingAgreement = null
                                onOpenBorrowerLink(token)
                            }) {
                                Text("Borrower Link")
                            }
                            TextButton(onClick = { viewingAgreement = null }) {
                                Text("Close")
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ConsentStatusBadge(status: ConsentStatus) {
    val (bgColor, txtColor, label) = when (status) {
        ConsentStatus.Pending -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Pending Consent")
        ConsentStatus.Consented -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "Consented / Signed")
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = txtColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
