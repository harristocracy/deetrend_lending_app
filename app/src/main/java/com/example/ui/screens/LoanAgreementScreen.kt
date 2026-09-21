package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AmortizationInstallment
import com.example.data.model.LoanApplication
import com.example.data.model.LoanStatus
import com.example.domain.export.LoanAgreementDocument
import com.example.domain.export.LoanAgreementExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoanAgreementScreen(
    loan: LoanApplication,
    installments: List<AmortizationInstallment> = emptyList(),
    onSignAgreement: (loanId: Long, signature: String) -> Unit,
    onBack: () -> Unit,
    onExportFeedback: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var agreedToTerms by remember { mutableStateOf(loan.status == LoanStatus.ACTIVE || loan.status == LoanStatus.COMPLETED) }
    var typedSignature by remember { mutableStateOf(loan.signatureName ?: loan.borrowerName) }
    val pathPoints = remember { mutableStateListOf<Offset>() }
    val isAlreadySigned = loan.status == LoanStatus.ACTIVE || loan.status == LoanStatus.COMPLETED

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showExportDialog by remember { mutableStateOf(false) }

    // Exported document structure
    val agreementDocument = remember(loan, installments) {
        LoanAgreementExporter.toDocument(loan, installments)
    }
    val formattedAgreementText = remember(agreementDocument) {
        LoanAgreementExporter.exportToFormattedText(agreementDocument)
    }

    val dateFormat = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.US) }
    val creationDateStr = remember(loan.createdAtMillis) {
        dateFormat.format(Date(loan.createdAtMillis))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("loan_agreement_scrollable"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("agreement_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = "Loan Agreement Form",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Promissory Note #${loan.id.toString().padStart(5, '0')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Export Agreement Action Button
                Button(
                    onClick = { showExportDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("export_agreement_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Export Agreement",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Formal Agreement Document Canvas
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("agreement_document_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Document Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "MASTER LOAN AGREEMENT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Dated: $creationDateStr",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isAlreadySigned) "EXECUTED CONTRACT" else "PENDING SIGNATURE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Borrower & Lender Information Block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "PARTIES TO THIS AGREEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Borrower:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = loan.borrowerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(text = "ID: ${loan.borrowerIdNumber}", style = MaterialTheme.typography.bodySmall)
                                Text(text = loan.borrowerEmail, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Lender:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "Apex Lending Services Inc.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(text = "Licence: #FL-94028", style = MaterialTheme.typography.bodySmall)
                                Text(text = "support@apexlending.local", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Truth-in-Lending Disclosures
                    Text(
                        text = "FEDERAL TRUTH-IN-LENDING DISCLOSURES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContractMetricBox(
                            title = "ANNUAL RATE",
                            value = "${loan.calculatedInterestRate}%",
                            subtitle = "Calculated APR",
                            modifier = Modifier.weight(1f)
                        )
                        ContractMetricBox(
                            title = "FINANCE CHARGE",
                            value = "$${String.format("%,.2f", loan.totalInterest)}",
                            subtitle = "Total interest cost",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContractMetricBox(
                            title = "AMOUNT FINANCED",
                            value = "$${String.format("%,.2f", loan.principalAmount)}",
                            subtitle = "Principal disbursed",
                            modifier = Modifier.weight(1f)
                        )
                        ContractMetricBox(
                            title = "TOTAL OF PAYMENTS",
                            value = "$${String.format("%,.2f", loan.totalRepayment)}",
                            subtitle = "${loan.termMonths} payments of $${String.format("%.2f", loan.monthlyPayment)}",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Legal Clauses
                    Text(
                        text = "TERMS AND CONDITIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ClauseText(
                        number = "1. PROMISE TO PAY",
                        text = "For value received, the Borrower promises to pay to the order of the Lender the Principal sum of $${String.format("%,.2f", loan.principalAmount)} with interest on the unpaid principal at the annual rate of ${loan.calculatedInterestRate}%. Payment shall be made in ${loan.termMonths} consecutive monthly installments of $${String.format("%.2f", loan.monthlyPayment)} beginning 30 days from execution."
                    )

                    ClauseText(
                        number = "2. PREPAYMENT",
                        text = "The Borrower has the right to prepay the unpaid balance in whole or in part at any time without penalty or prepayment fee. Any partial prepayment shall be credited against principal and interest according to the amortization schedule."
                    )

                    ClauseText(
                        number = "3. LATE CHARGE & DEFAULT",
                        text = "If any installment is not received within 15 calendar days of its due date, Borrower agrees to pay a late fee equal to 5.0% of the overdue payment. Default occurs upon failure to pay any installment when due."
                    )

                    ClauseText(
                        number = "4. PURPOSE OF LOAN",
                        text = "The Borrower certifies that the proceeds of this loan will be applied solely for the stated purpose: ${loan.loanPurpose}."
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Borrower Digital Signature Section
                    Text(
                        text = "BORROWER SIGNATURE & EXECUTION",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (isAlreadySigned) {
                        // Display executed signature
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "Electronically Signed & Executed",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "Signed by: ${loan.signatureName ?: loan.borrowerName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Signature Timestamp: ${dateFormat.format(Date(loan.agreementSignedAtMillis ?: loan.createdAtMillis))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Digital Signature Drawing Pad & Name input
                        OutlinedTextField(
                            value = typedSignature,
                            onValueChange = { typedSignature = it },
                            label = { Text("Legal Signature (Borrower Full Name)") },
                            leadingIcon = { Icon(Icons.Default.Draw, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("typed_signature_input")
                        )

                        // Signature Pad Box
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Draw Digital Signature below (Touch or Stylus):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (pathPoints.isNotEmpty()) {
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { pathPoints.clear() }
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, _ ->
                                            change.consume()
                                            pathPoints.add(change.position)
                                        }
                                    }
                                    .testTag("signature_canvas")
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (pathPoints.size > 1) {
                                        for (i in 0 until pathPoints.size - 1) {
                                            drawLine(
                                                color = Color(0xFF1E3A8A),
                                                start = pathPoints[i],
                                                end = pathPoints[i + 1],
                                                strokeWidth = 4f,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }

                                if (pathPoints.isEmpty()) {
                                    Text(
                                        text = "Sign here with finger",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }

                        // Agreement checkbox
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { agreedToTerms = !agreedToTerms },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = agreedToTerms,
                                onCheckedChange = { agreedToTerms = it },
                                modifier = Modifier.testTag("agree_terms_checkbox")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "I, ${typedSignature.ifEmpty { loan.borrowerName }}, hereby acknowledge reading and agreeing to all terms and conditions of this promissory note.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Sign & Activate Button
                        Button(
                            onClick = {
                                onSignAgreement(loan.id, typedSignature.ifEmpty { loan.borrowerName })
                            },
                            enabled = agreedToTerms && typedSignature.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("sign_and_activate_button")
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign & Disburse Loan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Secondary Export Option inside Card
                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_agreement_card_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Document / Copy Agreement")
                    }
                }
            }
        }
    }

    // Full Exported Loan Agreement Dialog & Preview
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Export Loan Agreement", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Formatted legal document ready for export, archival, or sharing:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 160.dp, max = 280.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = formattedAgreementText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Export actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Copy to Clipboard Button
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(formattedAgreementText))
                                onExportFeedback?.invoke("Agreement document copied to clipboard!")
                                showExportDialog = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_agreement_clipboard_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Text", fontSize = 13.sp)
                        }

                        // Share via Android Intent
                        OutlinedButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TITLE, "Loan Agreement - ${agreementDocument.contractNumber}")
                                    putExtra(Intent.EXTRA_SUBJECT, "Loan Agreement Contract - ${agreementDocument.borrowerName}")
                                    putExtra(Intent.EXTRA_TEXT, formattedAgreementText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Export Loan Agreement")
                                context.startActivity(shareIntent)
                                onExportFeedback?.invoke("Export share sheet opened!")
                                showExportDialog = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_agreement_intent_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Doc", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showExportDialog = false },
                    modifier = Modifier.testTag("dismiss_export_dialog_button")
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ContractMetricBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ClauseText(
    number: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}
