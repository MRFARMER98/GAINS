package com.example.gains.data.sync

import com.example.gains.data.Exercise

object CsvParser {
    fun parseExercises(csvContent: String): List<Exercise> {
        val trimmed = csvContent.trim()
        if (trimmed.startsWith("<!DOCTYPE html", ignoreCase = true) || trimmed.startsWith("<html", ignoreCase = true)) {
            throw IllegalArgumentException("Google Sheet is not published to the web. Go to File > Share > Publish to Web.")
        }
        val lines = trimmed.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        // Parse headers
        val headers = parseRow(lines[0])
        val nameIndex = headers.indexOfFirst { it.contains("name", ignoreCase = true) || it.contains("exercise", ignoreCase = true) }
        val muscleIndex = headers.indexOfFirst { it.contains("muscle", ignoreCase = true) || it.contains("group", ignoreCase = true) }

        val finalNameIndex = if (nameIndex != -1) nameIndex else 0
        val finalMuscleIndex = if (muscleIndex != -1) muscleIndex else 1

        return parseRows(lines.drop(1), finalNameIndex, finalMuscleIndex)
    }

    private fun parseRows(rows: List<String>, nameIndex: Int, muscleIndex: Int): List<Exercise> {
        val exercises = mutableListOf<Exercise>()
        for (row in rows) {
            val columns = parseRow(row)
            if (columns.size > nameIndex && columns.size > muscleIndex) {
                val name = columns[nameIndex].trim()
                val muscleGroup = columns[muscleIndex].trim()
                if (name.isNotEmpty() && muscleGroup.isNotEmpty()) {
                    exercises.add(Exercise(name = name, muscleGroup = muscleGroup))
                }
            }
        }
        return exercises
    }

    private fun parseRow(row: String): List<String> {
        val result = mutableListOf<String>()
        var currentToken = StringBuilder()
        var insideQuotes = false
        var i = 0
        while (i < row.length) {
            val c = row[i]
            if (c == '"') {
                insideQuotes = !insideQuotes
            } else if (c == ',' && !insideQuotes) {
                result.add(currentToken.toString().trim().removeSurrounding("\""))
                currentToken = StringBuilder()
            } else {
                currentToken.append(c)
            }
            i++
        }
        result.add(currentToken.toString().trim().removeSurrounding("\""))
        return result
    }
}
