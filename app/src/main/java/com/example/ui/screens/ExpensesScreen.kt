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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SocietyViewModel
import com.example.ui.components.SocietyRed
import com.example.ui.components.getCategoryIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
  viewModel: SocietyViewModel
) {
  val expenses by viewModel.societyExpenses.collectAsState()
  val allMaintenance by viewModel.allMaintenance.collectAsState()
  val isSubmitting by viewModel.isSubmitting.collectAsState()
  val effectiveRole = viewModel.effectiveRole()

  val totalSpent = remember(expenses) { expenses.sumOf { it.amount } }
  val approvedPayments = remember(allMaintenance) { allMaintenance.filter { it.status.equals("approved", ignoreCase = true) } }
  val totalEarnings = remember(approvedPayments) { approvedPayments.sumOf { it.amount } }
  val netBalance = totalEarnings - totalSpent

  var showAddExpenseForm by remember { mutableStateOf(false) }
  var titleInput by remember { mutableStateOf("") }
  var amountInput by remember { mutableStateOf("") }
  var categoryInput by remember { mutableStateOf("Maintenance") }
  var dateInput by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())) }
  var categoryDropdownExpanded by remember { mutableStateOf(false) }

  var selectedCategoryFilter by remember { mutableStateOf("All") }
  var searchQuery by remember { mutableStateOf("") }

  val categories = listOf(
    "Maintenance",
    "Security",
    "Electricity",
    "Water Supply",
    "Sanitation",
    "Garden",
    "Festivals & Events",
    "Office & Legal",
    "General"
  )

  val filteredExpenses = remember(expenses, selectedCategoryFilter, searchQuery) {
    expenses.filter { item ->
      val matchesCategory = selectedCategoryFilter == "All" || item.category.equals(selectedCategoryFilter, ignoreCase = true)
      val matchesSearch = searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true) || item.category.contains(searchQuery, ignoreCase = true)
      matchesCategory && matchesSearch
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Society Transparency Ledger",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SocietyRed
          )
          Text(
            text = "Complete record of society funds spent for community upkeep",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (effectiveRole == "admin") {
          Button(
            onClick = { showAddExpenseForm = !showAddExpenseForm },
            colors = ButtonDefaults.buttonColors(containerColor = if (showAddExpenseForm) MaterialTheme.colorScheme.surfaceVariant else SocietyRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("add_expense_toggle_button")
          ) {
            Icon(
              if (showAddExpenseForm) Icons.Default.Close else Icons.Default.Add,
              contentDescription = null,
              tint = if (showAddExpenseForm) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (showAddExpenseForm) "Close" else "Add",
              color = if (showAddExpenseForm) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
            )
          }
        }
      }
    }

    // Total Spent Summary Card
    item {
      ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(20.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = "Total Society Expenditure",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
              text = "₹${totalSpent.toInt()}",
              fontSize = 28.sp,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
              text = "${expenses.size} verified expense vouchers",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
          }

          Icon(
            Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
          )
        }
      }
    }

    // Total Earnings Summary Card (In Green)
    item {
      ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFE8F5E9)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("total_earnings_card")
      ) {
        Row(
          modifier = Modifier.padding(20.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = "Total Society Earnings (Maintenance Collected)",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF2E7D32)
            )
            Text(
              text = "₹${totalEarnings.toInt()}",
              fontSize = 28.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF1B5E20)
            )
            Text(
              text = "${approvedPayments.size} verified collections • Net Fund: ₹${netBalance.toInt()}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = Color(0xFF2E7D32)
            )
          }

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFC8E6C9),
            modifier = Modifier.size(50.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.CurrencyRupee,
                contentDescription = null,
                tint = Color(0xFF1B5E20),
                modifier = Modifier.size(30.dp)
              )
            }
          }
        }
      }
    }

    // Admin Add Expense Form Card
    if (effectiveRole == "admin" && showAddExpenseForm) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Log New Society Expense",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
              value = titleInput,
              onValueChange = { titleInput = it },
              label = { Text("Expense Title / Purpose *") },
              placeholder = { Text("e.g. Security guard monthly salary") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("expense_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Amount (₹) *") },
                placeholder = { Text("e.g. 5000") },
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, modifier = Modifier.size(18.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("expense_amount_input")
              )

              OutlinedTextField(
                value = dateInput,
                onValueChange = { dateInput = it },
                label = { Text("Date *") },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("expense_date_input")
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            ExposedDropdownMenuBox(
              expanded = categoryDropdownExpanded,
              onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
              modifier = Modifier.fillMaxWidth()
            ) {
              OutlinedTextField(
                value = categoryInput,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = categoryDropdownExpanded,
                onDismissRequest = { categoryDropdownExpanded = false }
              ) {
                categories.forEach { cat ->
                  DropdownMenuItem(
                    text = { Text(cat) },
                    onClick = {
                      categoryInput = cat
                      categoryDropdownExpanded = false
                    }
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = {
                val amt = amountInput.toDoubleOrNull() ?: 0.0
                viewModel.addExpense(titleInput, categoryInput, amt, dateInput)
                titleInput = ""
                amountInput = ""
                showAddExpenseForm = false
              },
              enabled = !isSubmitting && titleInput.isNotBlank() && (amountInput.toDoubleOrNull() ?: 0.0) > 0,
              colors = ButtonDefaults.buttonColors(containerColor = SocietyRed),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_expense_button")
            ) {
              if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Saving...")
              } else {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Expense to Ledger")
              }
            }
          }
        }
      }
    }

    // Search and Category Filters
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search expenses...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("expense_search_input")
      )
    }

    item {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        val allFilters = listOf("All") + categories
        items(allFilters) { filter ->
          FilterChip(
            selected = selectedCategoryFilter == filter,
            onClick = { selectedCategoryFilter = filter },
            label = { Text(filter) }
          )
        }
      }
    }

    if (filteredExpenses.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (searchQuery.isNotBlank() || selectedCategoryFilter != "All") "No matching expenses found." else "No society expenses logged yet.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(filteredExpenses) { item ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier.fillMaxWidth().testTag("expense_card_${item.id}")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.secondaryContainer,
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = getCategoryIcon(item.category),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.size(24.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                  Text(
                    text = item.category,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Date: ${item.date}",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.outline
                )
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "₹${item.amount.toInt()}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = SocietyRed
              )

              if (effectiveRole == "admin") {
                IconButton(
                  onClick = { viewModel.deleteExpense(item.id) },
                  modifier = Modifier.size(28.dp).testTag("delete_expense_${item.id}")
                ) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Expense",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
