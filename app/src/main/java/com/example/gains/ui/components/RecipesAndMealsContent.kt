package com.example.gains.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gains.data.FoodRecipeWithDetails
import com.example.gains.theme.InfraredAccent
import com.example.gains.theme.LabelCaps
import com.example.gains.ui.main.MainScreenViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun RecipesAndMealsContent(
    viewModel: MainScreenViewModel,
    repository: com.example.gains.data.DataRepository,
    modifier: Modifier = Modifier
) {
    val filteredRecipes by viewModel.filteredRecipes.collectAsStateWithLifecycle()
    val selectedIngredientFilters by viewModel.selectedIngredientFilters.collectAsStateWithLifecycle()

    var showCreateRecipeDialog by remember { mutableStateOf(false) }
    var selectedRecipeForView by remember { mutableStateOf<FoodRecipeWithDetails?>(null) }
    var ingredientFilterInput by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Multi-Ingredient Search & Filter Header Card
            item {
                GainsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "RECIPE STUDIO",
                                style = LabelCaps.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = { showCreateRecipeDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = InfraredAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("NEW RECIPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Ingredient Filter Input Box
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = ingredientFilterInput,
                                onValueChange = { ingredientFilterInput = it },
                                label = { Text("What's in your kitchen? (e.g. Chicken, Eggs)") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (ingredientFilterInput.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                viewModel.toggleIngredientFilter(ingredientFilterInput)
                                                ingredientFilterInput = ""
                                            }
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add Filter", tint = InfraredAccent)
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (ingredientFilterInput.isNotBlank()) {
                                            viewModel.toggleIngredientFilter(ingredientFilterInput)
                                            ingredientFilterInput = ""
                                        }
                                    }
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        // Active Ingredient Chips Row
                        if (selectedIngredientFilters.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                selectedIngredientFilters.forEach { filter ->
                                    InputChip(
                                        selected = true,
                                        onClick = { viewModel.toggleIngredientFilter(filter) },
                                        label = { Text(filter, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                        },
                                        colors = InputChipDefaults.inputChipColors(
                                            selectedContainerColor = InfraredAccent.copy(alpha = 0.2f),
                                            selectedLabelColor = InfraredAccent
                                        )
                                    )
                                }
                                TextButton(onClick = { viewModel.clearIngredientFilters() }) {
                                    Text("Clear All", fontSize = 11.sp, color = InfraredAccent)
                                }
                            }
                        }
                    }
                }
            }

            // Recipe List Count & Filter Indicator
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedIngredientFilters.isEmpty()) {
                            "ALL RECIPES (${filteredRecipes.size})"
                        } else {
                            "FILTERED RECIPES (${filteredRecipes.size})"
                        },
                        style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedIngredientFilters.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = InfraredAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${selectedIngredientFilters.size} active filters",
                                style = LabelCaps.copy(fontSize = 10.sp),
                                color = InfraredAccent
                            )
                        }
                    }
                }
            }

            // Recipe Cards
            if (filteredRecipes.isEmpty()) {
                item {
                    GainsCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalDining,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (selectedIngredientFilters.isEmpty()) "No recipes created yet" else "No matching recipes found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedIngredientFilters.isEmpty()) {
                                    "Tap 'New Recipe' above to create meal templates from Albert Heijn & NEVO ingredients."
                                } else {
                                    "Try clearing ingredient filters or adding different pantry ingredients."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredRecipes, key = { it.recipe.id }) { recipeDetails ->
                    RecipeCardItem(
                        recipeWithDetails = recipeDetails,
                        onViewDetails = { selectedRecipeForView = recipeDetails },
                        onLogAsMeal = {
                            selectedRecipeForView = recipeDetails
                        },
                        onDelete = {
                            viewModel.deleteRecipe(recipeDetails.recipe.id)
                        }
                    )
                }
            }
        }

        // Dialogs
        if (showCreateRecipeDialog) {
            CreateRecipeDialog(
                onDismiss = { showCreateRecipeDialog = false },
                onSaveRecipe = { recipe, ingredients ->
                    viewModel.saveRecipe(recipe, ingredients)
                },
                searchFoodItemsSync = { query ->
                    repository.searchFoodItems(query).firstOrNull() ?: emptyList()
                }
            )
        }

        selectedRecipeForView?.let { recipeDetails ->
            RecipeDetailViewDialog(
                recipeWithDetails = recipeDetails,
                onDismiss = { selectedRecipeForView = null },
                onLogAsMeal = { mealType, qty ->
                    viewModel.logRecipeAsMeal(recipeDetails.recipe.id, qty, mealType)
                }
            )
        }
    }
}
