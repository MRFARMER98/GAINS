package com.example.gains.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gains.data.DailyNutrientSummary
import com.example.gains.data.FoodItem
import com.example.gains.data.FoodNutrient
import com.example.gains.data.FoodServing
import com.example.gains.theme.*
import com.example.gains.R
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodDialog(
    initialMealType: String,
    searchQuery: String,
    searchResults: List<FoodItem>,
    onQueryChange: (String) -> Unit,
    onLogFood: (FoodItem, FoodNutrient?, FoodServing?, Float, String) -> Unit,
    onDismiss: () -> Unit,
    getNutrientForFoodSync: suspend (String) -> FoodNutrient?,
    getServingsForFoodSync: suspend (String) -> List<FoodServing>
) {
    var selectedMealType by remember { mutableStateOf(initialMealType) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var selectedNutrient by remember { mutableStateOf<FoodNutrient?>(null) }
    var selectedServing by remember { mutableStateOf<FoodServing?>(null) }
    var servingsList by remember { mutableStateOf<List<FoodServing>>(emptyList()) }
    var gramsText by remember { mutableStateOf("100") }
    var isLoadingDetails by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (selectedFood == null) "LOG FOOD ITEM" else "ENTER PORTION (${if (selectedFood?.perUnit == "100ml") "ML" else "GRAMS"})",
                                style = LabelCaps.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (selectedFood != null) selectedFood = null else onDismiss()
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectedFood != null) Icons.Default.ArrowBack else Icons.Default.Close,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    // Horizontal Scrollable Meal Category Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val meals = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
                        meals.forEach { meal ->
                            FilterChip(
                                selected = selectedMealType == meal,
                                onClick = { selectedMealType = meal },
                                label = { Text(meal, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InfraredAccent,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    if (selectedFood == null) {
                        // Standout Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            placeholder = { Text("Search Albert Heijn or NEVO (e.g. kwark, brood, banaan)...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = InfraredAccent, modifier = Modifier.size(22.dp)) },
                            trailingIcon = {
                                IconButton(onClick = { /* Barcode scanner stub */ }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = InfraredAccent)
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                focusedBorderColor = InfraredAccent,
                                unfocusedBorderColor = InfraredAccent.copy(alpha = 0.35f)
                            )
                        )

                        // Results List
                        if (searchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isBlank()) "Type to search Dutch foods..." else "No foods found matching '$searchQuery'",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(searchResults) { food ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedFood = food
                                                isLoadingDetails = true
                                                coroutineScope.launch {
                                                    val nut = getNutrientForFoodSync(food.id)
                                                    val servList = getServingsForFoodSync(food.id)
                                                    selectedNutrient = nut
                                                    servingsList = servList
                                                    val defaultServ = servList.firstOrNull { it.isDefault } ?: servList.firstOrNull()
                                                    selectedServing = defaultServ
                                                    gramsText = (defaultServ?.gramWeight ?: 100f).toInt().toString()
                                                    isLoadingDetails = false
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Food Image Thumbnail on Left
                                            Surface(
                                                modifier = Modifier.size(48.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                color = PrimarySoftBg
                                            ) {
                                                if (!food.imageUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = food.imageUrl,
                                                        contentDescription = food.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.RestaurantMenu,
                                                            contentDescription = null,
                                                            tint = InfraredAccent,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = food.name,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f, fill = false),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    SourceLogoBadge(source = food.source)
                                                    Text(
                                                        text = "• per ${food.perUnit}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(imageVector = Icons.Default.Add, contentDescription = "Select", tint = InfraredAccent)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Selected Food Direct Grams/ML Editor View
                        val food = selectedFood!!
                        val isLiquid = food.perUnit == "100ml"
                        val unitLabel = if (isLiquid) "ml" else "g"

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                color = PrimarySoftBg,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(food.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    if (food.brand != null) {
                                        Text(food.brand, style = MaterialTheme.typography.bodyMedium, color = InfraredAccent)
                                    }
                                    if (selectedNutrient != null) {
                                        val n = selectedNutrient!!
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Per 100$unitLabel: ${n.caloriesKcal.toInt()} kcal | P: ${n.proteinG}g | C: ${n.carbsG}g | F: ${n.fatG}g | Fiber: ${n.fiberG}g | Salt: ${n.saltG}g",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Direct Grams / ML Input Box
                            Column {
                                Text(
                                    text = "Amount in ${if (isLiquid) "Milliliters (ml)" else "Grams (g)"}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = gramsText,
                                    onValueChange = { gramsText = it },
                                    label = { Text("Weight ($unitLabel)") },
                                    suffix = { Text(unitLabel, fontWeight = FontWeight.Bold, color = InfraredAccent) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Portion Preset Chips
                                Text("Quick Presets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(50, 100, 150, 200, 250, 350, 500).forEach { presetGrams ->
                                        FilterChip(
                                            selected = gramsText == presetGrams.toString(),
                                            onClick = { gramsText = presetGrams.toString() },
                                            label = { Text("$presetGrams$unitLabel", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = InfraredAccent,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            // Portion Serving Chips if defined (e.g. 1 Snede 35g vs 1 Bak 500g)
                            if (servingsList.isNotEmpty()) {
                                Column {
                                    Text("Standard Servings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        servingsList.forEach { s ->
                                            FilterChip(
                                                selected = gramsText == s.gramWeight.toInt().toString(),
                                                onClick = {
                                                    selectedServing = s
                                                    gramsText = s.gramWeight.toInt().toString()
                                                },
                                                label = { Text("${s.description} (${s.gramWeight.toInt()}$unitLabel)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = InfraredAccent,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Live Calculated Macro Preview Box
                            val enteredGrams = gramsText.toFloatOrNull() ?: 100f
                            val mult = enteredGrams / 100f
                            val nut = selectedNutrient

                            GainsCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("CALCULATED NUTRITION", style = LabelCaps.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("WEIGHT", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${enteredGrams.toInt()}$unitLabel", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("CALORIES", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${((nut?.caloriesKcal ?: 0f) * mult).toInt()} kcal", fontWeight = FontWeight.Bold, color = InfraredAccent, fontSize = 15.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("PROTEIN", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${String.format("%.1f", (nut?.proteinG ?: 0f) * mult)}g", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("CARBS", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${String.format("%.1f", (nut?.carbsG ?: 0f) * mult)}g", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    onLogFood(food, selectedNutrient, selectedServing, enteredGrams, selectedMealType)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = InfraredAccent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("LOG ${enteredGrams.toInt()}$unitLabel TO $selectedMealType", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NutrientBreakdownDialog(
    summary: DailyNutrientSummary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DUTCH ADH / RDI BREAKDOWN",
                    style = LabelCaps.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("MACRONUTRIENTS", style = LabelCaps, color = InfraredAccent, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    NutrientRow("Energy", "${summary.caloriesKcal.toInt()} kcal", 2400f, summary.caloriesKcal)
                    NutrientRow("Protein", "${String.format("%.1f", summary.proteinG)}g", 160f, summary.proteinG)
                    NutrientRow("Carbohydrates", "${String.format("%.1f", summary.carbsG)}g", 250f, summary.carbsG)
                    NutrientRow("Fats", "${String.format("%.1f", summary.fatG)}g", 70f, summary.fatG)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("KEY HEALTH METRICS (DUTCH VOEDINGSCENTRUM)", style = LabelCaps, color = InfraredAccent, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    NutrientRow("Dietary Fiber", "${String.format("%.1f", summary.fiberG)}g", 30f, summary.fiberG)
                    NutrientRow("Saturated Fat", "${String.format("%.1f", summary.saturatedFatG)}g (Max 22g)", 22f, summary.saturatedFatG, isMaxConstraint = true)
                    NutrientRow("Sugars", "${String.format("%.1f", summary.sugarsG)}g (Max 50g)", 50f, summary.sugarsG, isMaxConstraint = true)
                    NutrientRow("Salt", "${String.format("%.1f", summary.saltG)}g (Max 6.0g)", 6.0f, summary.saltG, isMaxConstraint = true)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("VITAMINS & MINERALS (ESTIMATED)", style = LabelCaps, color = InfraredAccent, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    NutrientRow("Calcium", "450 mg", 1000f, 450f)
                    NutrientRow("Iron", "8.2 mg", 11f, 8.2f)
                    NutrientRow("Vitamin C", "45 mg", 75f, 45f)
                    NutrientRow("Vitamin D", "4.0 µg", 10f, 4.0f)
                    NutrientRow("Vitamin B12", "1.8 µg", 2.8f, 1.8f)
                    NutrientRow("Magnesium", "220 mg", 350f, 220f)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = InfraredAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CLOSE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun NutrientRow(
    label: String,
    valueText: String,
    target: Float,
    current: Float,
    isMaxConstraint: Boolean = false
) {
    val progress = (current / target).coerceIn(0f, 1f)
    val isWarning = isMaxConstraint && current > target

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                valueText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isWarning) SystemRed else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (isWarning) SystemRed else if (progress >= 1f && !isMaxConstraint) Color(0xFF10B981) else InfraredAccent,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeCardItem(
    recipeWithDetails: com.example.gains.data.FoodRecipeWithDetails,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier,
    onLogAsMeal: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val recipe = recipeWithDetails.recipe

    GainsCard(
        onClick = onViewDetails,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .heightIn(min = 135.dp)
        ) {
            // Big Recipe Image spanning entire left side of the card
            Surface(
                modifier = Modifier
                    .width(135.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (!recipe.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = recipe.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = InfraredAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Right side details: Title, Category, Time/Persons, Macros
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Category Badge (if present)
                    if (!recipe.category.isNullOrBlank()) {
                        Surface(
                            color = InfraredAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = recipe.category.orEmpty().uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = InfraredAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Recipe Title (overflows cleanly to lines underneath)
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Time with Clock Icon & Servings with Person Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val timeText = if (recipeWithDetails.totalTimeMinutes > 0) {
                            "${recipeWithDetails.totalTimeMinutes} min"
                        } else {
                            "Quick"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Time",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Servings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${recipe.servingsCount} ${if (recipe.servingsCount == 1) "pers." else "pers."}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Macros Per Serving Summary Pills
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MacroPill(label = "${recipeWithDetails.caloriesPerServing.toInt()} kcal", color = InfraredAccent)
                        MacroPill(label = "P: ${String.format(java.util.Locale.US, "%.0f", recipeWithDetails.proteinPerServing)}g", color = Color(0xFF10B981))
                        MacroPill(label = "C: ${String.format(java.util.Locale.US, "%.0f", recipeWithDetails.carbsPerServing)}g", color = Color(0xFF0EA5E9))
                        MacroPill(label = "F: ${String.format(java.util.Locale.US, "%.0f", recipeWithDetails.fatPerServing)}g", color = Color(0xFFF59E0B))
                    }
                }
            }
        }
    }
}

fun scaleIngredientText(rawText: String?, baseGrams: Float, multiplier: Float): String {
    if (rawText.isNullOrBlank()) {
        val scaledGrams = (baseGrams * multiplier).roundToInt()
        return if (scaledGrams > 0) "${scaledGrams}g" else ""
    }
    val trimmed = rawText.trim()
    if (multiplier == 1f) return trimmed

    // Pattern matching leading fraction: "1/2 citroen", "1/4 tl"
    val fractionRegex = Regex("""^(\d+)/(\d+)\s*(.*)""")
    val fractionMatch = fractionRegex.matchEntire(trimmed)
    if (fractionMatch != null) {
        val num = fractionMatch.groupValues[1].toFloatOrNull() ?: 1f
        val den = fractionMatch.groupValues[2].toFloatOrNull() ?: 1f
        val scaledVal = (num / den) * multiplier
        val rest = fractionMatch.groupValues[3]
        val formattedVal = formatQuantityNumber(scaledVal)
        return if (rest.isNotBlank()) "$formattedVal $rest" else formattedVal
    }

    // Pattern matching leading number: "500 g gehakt", "1.5 tl", "2 el"
    val numberRegex = Regex("""^([\d.,]+)\s*(.*)""")
    val match = numberRegex.matchEntire(trimmed)
    if (match != null) {
        val numStr = match.groupValues[1].replace(',', '.')
        val num = numStr.toFloatOrNull()
        if (num != null) {
            val scaledVal = num * multiplier
            val rest = match.groupValues[2]
            val formattedVal = formatQuantityNumber(scaledVal)
            return if (rest.isNotBlank()) "$formattedVal $rest" else formattedVal
        }
    }

    // Fallback if no leading number
    if (baseGrams > 0f) {
        val scaledGrams = (baseGrams * multiplier).roundToInt()
        return "$trimmed (~${scaledGrams}g)"
    }

    return trimmed
}

private fun formatQuantityNumber(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else if (kotlin.math.abs(value - kotlin.math.round(value * 10f) / 10f) < 0.05f) {
        String.format(java.util.Locale.US, "%.1f", value)
    } else {
        String.format(java.util.Locale.US, "%.2f", value)
    }
}

@Composable
fun MacroPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

@Composable
fun CollapsibleLoggedRecipeCard(
    recipeName: String,
    entries: List<com.example.gains.data.LoggedFoodEntry>,
    onDeleteEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val totalCalories = entries.sumOf { it.caloriesKcal.toDouble() }.toInt()
    val totalProtein = entries.sumOf { it.proteinG.toDouble() }
    val totalCarbs = entries.sumOf { it.carbsG.toDouble() }
    val totalFat = entries.sumOf { it.fatG.toDouble() }

    GainsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.RestaurantMenu,
                        contentDescription = null,
                        tint = InfraredAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = recipeName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${entries.size} ingredients • P:${String.format("%.1f", totalProtein)}g C:${String.format("%.1f", totalCarbs)}g F:${String.format("%.1f", totalFat)}g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$totalCalories kcal",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) androidx.compose.material.icons.Icons.Default.ExpandLess else androidx.compose.material.icons.Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))

                entries.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.foodName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${entry.caloriesKcal.toInt()} kcal",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = InfraredAccent
                                )
                                Text(
                                    text = "• ${entry.servingDescription ?: "${entry.gramWeightTotal.toInt()}g"}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteEntry(entry.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete ingredient",
                                tint = SystemRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleNutrientRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailViewDialog(
    recipeWithDetails: com.example.gains.data.FoodRecipeWithDetails,
    onDismiss: () -> Unit,
    onLogAsMeal: (mealType: String, servingsToLog: Float) -> Unit
) {
    val recipe = recipeWithDetails.recipe
    val baseServings = if (recipe.servingsCount > 0) recipe.servingsCount else 4
    var targetPersons by remember { mutableStateOf(baseServings) }
    val scalingMultiplier = targetPersons.toFloat() / baseServings.toFloat()
    var viewingIngredientFood by remember { mutableStateOf<com.example.gains.data.RecipeIngredientWithFood?>(null) }

    var selectedMealType by remember { mutableStateOf("DINNER") }
    var servingsToLogText by remember { mutableStateOf("1") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = recipe.name.uppercase(),
                                style = LabelCaps.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Recipe Image Banner (if available)
                    if (!recipe.imageUrl.isNullOrBlank()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                AsyncImage(
                                    model = recipe.imageUrl,
                                    contentDescription = recipe.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Header Overview Card
                    item {
                        GainsCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = InfraredAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${recipeWithDetails.totalTimeMinutes} min total",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    if (!recipe.category.isNullOrBlank()) {
                                        Surface(
                                            color = InfraredAccent.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = recipe.category.orEmpty().uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = InfraredAccent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Macros per Serving
                                Text(
                                    text = "NUTRITION PER SERVING",
                                    style = LabelCaps.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MacroPill(label = "${recipeWithDetails.caloriesPerServing.toInt()} kcal", color = InfraredAccent)
                                    MacroPill(label = "P: ${String.format("%.1f", recipeWithDetails.proteinPerServing)}g", color = Color(0xFF10B981))
                                    MacroPill(label = "C: ${String.format("%.1f", recipeWithDetails.carbsPerServing)}g", color = Color(0xFF0EA5E9))
                                    MacroPill(label = "F: ${String.format("%.1f", recipeWithDetails.fatPerServing)}g", color = Color(0xFFF59E0B))
                                }
                            }
                        }
                    }

                    // Ingredients List Section with Persons Stepper Calculator
                    item {
                        GainsCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = "INGREDIENTS (${recipeWithDetails.ingredients.size})",
                                            style = LabelCaps.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (targetPersons != baseServings) {
                                            Text(
                                                text = "Scaled for $targetPersons pers. (base: $baseServings)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                color = InfraredAccent
                                            )
                                        }
                                    }

                                    // Stepper for Person Count
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clickable(enabled = targetPersons > 1) {
                                                    if (targetPersons > 1) targetPersons--
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Remove,
                                                    contentDescription = "Decrease persons",
                                                    tint = if (targetPersons > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "$targetPersons ${if (targetPersons == 1) "pers." else "pers."}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clickable(enabled = targetPersons < 99) {
                                                    targetPersons++
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                                    contentDescription = "Increase persons",
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // List of Ingredients
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    recipeWithDetails.ingredients.forEach { ing ->
                                        val scaledText = scaleIngredientText(ing.servingDescription, ing.quantityGrams, scalingMultiplier)
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            // Primary: Scaled Recipe Ingredient Text in Orange
                                            Text(
                                                text = "• $scaledText",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = InfraredAccent
                                            )

                                            // Sub-item: Matched Database Food (compact, tappable, styled like food search item)
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 12.dp, top = 4.dp)
                                                    .clickable { viewingIngredientFood = ing }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = ing.foodName,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${ing.caloriesPer100g.toInt()} kcal / 100g • P:${String.format("%.1f", ing.proteinPer100g)}g C:${String.format("%.1f", ing.carbsPer100g)}g F:${String.format("%.1f", ing.fatPer100g)}g",
                                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                                                        contentDescription = "Details",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Instructions Section (if present)
                    if (!recipe.instructions.isNullOrBlank()) {
                        item {
                            GainsCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "INSTRUCTIONS & PREPARATION",
                                        style = LabelCaps.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = recipe.instructions.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }

                    // Quick Log Section
                    item {
                        GainsCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "LOG AS MEAL TO TODAY'S DIARY",
                                    style = LabelCaps.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Meal Type Selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK").forEach { type ->
                                        val isSel = selectedMealType == type
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { selectedMealType = type },
                                            label = { Text(type, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = InfraredAccent,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = servingsToLogText,
                                    onValueChange = { servingsToLogText = it },
                                    label = { Text("Servings to Log") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        val qty = servingsToLogText.toFloatOrNull() ?: 1.0f
                                        onLogAsMeal(selectedMealType, qty)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = InfraredAccent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("LOG MEAL NOW", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Sub-card Dialog: Ingredient Food Nutrition Details
            if (viewingIngredientFood != null) {
                val ing = viewingIngredientFood!!
                val scaledGrams = (ing.quantityGrams * scalingMultiplier).roundToInt()
                AlertDialog(
                    onDismissRequest = { viewingIngredientFood = null },
                    title = {
                        Column {
                            Text(
                                text = ing.foodName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!ing.brandName.isNullOrBlank()) {
                                Text(
                                    text = ing.brandName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("For $targetPersons ${if (targetPersons == 1) "person" else "persons"}:", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = if (scaledGrams > 0) "~$scaledGrams g" else "As specified",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = InfraredAccent
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "NUTRITION PER 100G",
                                style = LabelCaps.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MacroPill(label = "${ing.caloriesPer100g.toInt()} kcal", color = InfraredAccent)
                                MacroPill(label = "P: ${String.format("%.1f", ing.proteinPer100g)}g", color = Color(0xFF10B981))
                                MacroPill(label = "C: ${String.format("%.1f", ing.carbsPer100g)}g", color = Color(0xFF0EA5E9))
                                MacroPill(label = "F: ${String.format("%.1f", ing.fatPer100g)}g", color = Color(0xFFF59E0B))
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SimpleNutrientRow("Sugars", "${String.format("%.1f", ing.sugarsPer100g)} g")
                                SimpleNutrientRow("Saturated Fat", "${String.format("%.1f", ing.saturatedFatPer100g)} g")
                                SimpleNutrientRow("Fiber", "${String.format("%.1f", ing.fiberPer100g)} g")
                                SimpleNutrientRow("Salt", "${String.format("%.2f", ing.saltPer100g)} g")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewingIngredientFood = null }) {
                            Text("Close", color = InfraredAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeDialog(
    onDismiss: () -> Unit,
    onSaveRecipe: (com.example.gains.data.FoodRecipe, List<com.example.gains.data.FoodRecipeIngredient>) -> Unit,
    searchFoodItemsSync: suspend (String) -> List<FoodItem>
) {
    var name by remember { mutableStateOf("") }
    var servingsText by remember { mutableStateOf("2") }
    var prepTimeText by remember { mutableStateOf("15") }
    var cookTimeText by remember { mutableStateOf("10") }
    var categoryText by remember { mutableStateOf("Dinner") }
    var instructionsText by remember { mutableStateOf("") }

    var ingredientSearchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var selectedIngredients by remember { mutableStateOf<List<Pair<FoodItem, Float>>>(emptyList()) }
    var ingredientGramsText by remember { mutableStateOf("100") }
    var activeItemToAdd by remember { mutableStateOf<FoodItem?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "CREATE RECIPE / MEAL",
                                style = LabelCaps.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            Button(
                                onClick = {
                                    if (name.isNotBlank() && selectedIngredients.isNotEmpty()) {
                                        val recipe = com.example.gains.data.FoodRecipe(
                                            name = name.trim(),
                                            servingsCount = servingsText.toIntOrNull() ?: 1,
                                            prepTimeMinutes = prepTimeText.toIntOrNull(),
                                            cookTimeMinutes = cookTimeText.toIntOrNull(),
                                            category = categoryText.trim().ifBlank { null },
                                            instructions = instructionsText.trim().ifBlank { null },
                                            createdAt = System.currentTimeMillis()
                                        )
                                        val ingredients = selectedIngredients.map { (food, grams) ->
                                            com.example.gains.data.FoodRecipeIngredient(
                                                recipeId = 0,
                                                foodId = food.id,
                                                quantityGrams = grams,
                                                servingDescription = "${grams.toInt()}g"
                                            )
                                        }
                                        onSaveRecipe(recipe, ingredients)
                                        onDismiss()
                                    }
                                },
                                enabled = name.isNotBlank() && selectedIngredients.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = InfraredAccent)
                            ) {
                                Text("SAVE", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Recipe Basic Info
                    item {
                        GainsCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("RECIPE DETAILS", style = LabelCaps.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface)

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Recipe Title / Name *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = servingsText,
                                        onValueChange = { servingsText = it },
                                        label = { Text("Servings (Persons)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = categoryText,
                                        onValueChange = { categoryText = it },
                                        label = { Text("Category") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = prepTimeText,
                                        onValueChange = { prepTimeText = it },
                                        label = { Text("Prep Time (min)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = cookTimeText,
                                        onValueChange = { cookTimeText = it },
                                        label = { Text("Cook Time (min)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                OutlinedTextField(
                                    value = instructionsText,
                                    onValueChange = { instructionsText = it },
                                    label = { Text("Step-by-Step Instructions (Optional)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    maxLines = 5
                                )
                            }
                        }
                    }

                    // Selected Ingredients List
                    item {
                        GainsCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "INGREDIENTS (${selectedIngredients.size})",
                                    style = LabelCaps.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (selectedIngredients.isEmpty()) {
                                    Text(
                                        text = "No ingredients added yet. Search below to add food items.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    selectedIngredients.forEachIndexed { idx, (item, grams) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${item.name} (${grams.toInt()}g)",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(
                                                onClick = {
                                                    selectedIngredients = selectedIngredients.toMutableList().also { it.removeAt(idx) }
                                                },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = SystemRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add Ingredient Search Section
                    item {
                        GainsCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("ADD INGREDIENTS FROM DATABASE", style = LabelCaps.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = ingredientSearchQuery,
                                    onValueChange = { q ->
                                        ingredientSearchQuery = q
                                        coroutineScope.launch {
                                            if (q.trim().length >= 2) {
                                                searchResults = searchFoodItemsSync(q.trim())
                                            } else {
                                                searchResults = emptyList()
                                            }
                                        }
                                    },
                                    label = { Text("Search ingredient...") },
                                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                if (searchResults.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    searchResults.take(5).forEach { food ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { activeItemToAdd = food }
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(food.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                                if (food.brand != null) {
                                                    Text(food.brand, fontSize = 11.sp, color = InfraredAccent)
                                                }
                                            }
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                                contentDescription = "Add",
                                                tint = InfraredAccent
                                            )
                                        }
                                    }
                                }

                                if (activeItemToAdd != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "Add: ${activeItemToAdd?.name}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                OutlinedTextField(
                                                    value = ingredientGramsText,
                                                    onValueChange = { ingredientGramsText = it },
                                                    label = { Text("Grams") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = {
                                                        val grams = ingredientGramsText.toFloatOrNull() ?: 100f
                                                        activeItemToAdd?.let { item ->
                                                            selectedIngredients = selectedIngredients + (item to grams)
                                                        }
                                                        activeItemToAdd = null
                                                        ingredientSearchQuery = ""
                                                        searchResults = emptyList()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = InfraredAccent)
                                                ) {
                                                    Text("ADD", color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceLogoBadge(source: String, modifier: Modifier = Modifier) {
    val isAh = source.equals("AH", ignoreCase = true) || source.contains("AH", ignoreCase = true)
    val isNevo = source.equals("NEVO", ignoreCase = true)

    if (isAh) {
        Icon(
            painter = painterResource(id = R.drawable.ic_albert_heijn_logo),
            contentDescription = "Albert Heijn",
            tint = Color.Unspecified,
            modifier = modifier.size(18.dp)
        )
    } else if (isNevo) {
        Surface(
            color = Color(0xFF10B981),
            shape = RoundedCornerShape(4.dp),
            modifier = modifier
        ) {
            Text(
                text = "NEVO",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    } else {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(4.dp),
            modifier = modifier
        ) {
            Text(
                text = source,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}



