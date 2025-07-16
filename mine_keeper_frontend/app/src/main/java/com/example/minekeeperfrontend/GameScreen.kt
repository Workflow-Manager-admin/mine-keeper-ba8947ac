package com.example.minekeeperfrontend

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * Basic config – for future .env variables (you may use .env for configurable settings!)
 */
object Config {
    // Placeholders for environment hooks
    val defaultRows = 9
    val defaultCols = 9
    val defaultMines = 10
}

/** Represents the state of an individual cell in the minesweeper grid. */
data class Cell(
    val row: Int,
    val col: Int,
    var isMine: Boolean = false,
    var revealed: Boolean = false,
    var flagged: Boolean = false,
    var adjacentMines: Int = 0
)

/** The main Composable representing the entire gameplay screen. */
// PUBLIC_INTERFACE
@Composable
fun GameScreen() {
    // Mutable state hooks
    var rows by remember { mutableStateOf(Config.defaultRows) }
    var cols by remember { mutableStateOf(Config.defaultCols) }
    var mines by remember { mutableStateOf(Config.defaultMines) }
    val gridState = remember { mutableStateOf(List(rows) { r ->
        List(cols) { c -> Cell(row = r, col = c) }
    }) }
    var firstMoveMade by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var won by remember { mutableStateOf(false) }
    var lost by remember { mutableStateOf(false) }
    var flagsCount by remember { mutableStateOf(0) }
    var elapsedTime by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    // Timer effect
    LaunchedEffect(playing, won, lost, firstMoveMade) {
        while (playing && !won && !lost && firstMoveMade) {
            delay(1000)
            elapsedTime += 1
        }
    }

    fun startNewGame() {
        val freshGrid = List(rows) { r -> List(cols) { c -> Cell(row = r, col = c) } }
        gridState.value = freshGrid
        score = 0
        playing = true
        won = false
        lost = false
        flagsCount = 0
        elapsedTime = 0
        firstMoveMade = false
    }

    fun revealCell(row: Int, col: Int) {
        val grid = gridState.value.map { it.map { cell -> cell.copy() } }
        val cell = grid[row][col]
        if (!playing || cell.revealed || cell.flagged) return

        if (!firstMoveMade) {
            // Place mines AFTER first click for fair gameplay
            placeMines(grid, row, col, mines)
            updateAdjacents(grid)
            firstMoveMade = true
        }

        // Reveal selected cell
        cell.revealed = true
        score++

        if (cell.isMine) {
            lost = true
            playing = false
            revealAllMines(grid)
        } else if (cell.adjacentMines == 0) {
            // Reveal adjacent empty cells recursively
            revealAdjacentEmpties(grid, row, col)
        }

        // Win condition: all non-mine cells revealed
        if (checkWin(grid, mines)) {
            won = true
            playing = false
        }
        gridState.value = grid
    }

    fun flagCell(row: Int, col: Int) {
        val grid = gridState.value.map { it.map { cell -> cell.copy() } }
        val cell = grid[row][col]
        if (!playing || cell.revealed) return
        cell.flagged = !cell.flagged
        flagsCount = grid.sumOf { row -> row.count { it.flagged } }
        gridState.value = grid
    }

    // Main grid and UI
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Score & Timer
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ScoreTimerBox("Score", "$score")
                    ScoreTimerBox("Flags", "$flagsCount/$mines")
                    ScoreTimerBox("Time", formatSeconds(elapsedTime))
                }
                if (won || lost)
                    Text(
                        text = if (won) "You Win!" else "Game Over",
                        color = if (won) Color(0xFF388E3C) else Color.Red,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
            }

            // Center: Main Game Grid
            Box(
                modifier = Modifier.weight(1f)
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GameGrid(
                    grid = gridState.value,
                    enabled = playing && !won && !lost,
                    onCellClick = { r, c -> revealCell(r, c) },
                    onCellLongPress = { r, c -> flagCell(r, c) }
                )
            }

            // Bottom: Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier.weight(1f), 
                    onClick = { showSettings = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Settings")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    modifier = Modifier.weight(1f), 
                    onClick = { startNewGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Text(if (!playing) "New Game" else "Restart")
                }
            }
        }

        if (showSettings)
            SettingsPanel(
                initialRows = rows,
                initialCols = cols,
                initialMines = mines,
                onClose = { showSettings = false },
                onSave = { newRows, newCols, newMines ->
                    rows = newRows
                    cols = newCols
                    mines = newMines
                    showSettings = false
                    startNewGame()
                }
            )
    }
}

 
// Utility composable for score/timer/flag
@Composable
fun ScoreTimerBox(label: String, value: String) {
    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 18.sp)
    }
}

// Composable for the settings dialog
@Composable
fun SettingsPanel(
    initialRows: Int,
    initialCols: Int,
    initialMines: Int,
    onClose: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var rows by remember { mutableStateOf(initialRows) }
    var cols by remember { mutableStateOf(initialCols) }
    var mines by remember { mutableStateOf(initialMines) }
    AlertDialog(
        onDismissRequest = { onClose() },
        confirmButton = {
            Button(onClick = { 
                onSave(rows, cols, mines) 
            }, 
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = { onClose() }) { Text("Cancel") }
        },
        title = { Text("Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                LabeledNumberField("Rows", rows, 5, 18, { rows = it })
                LabeledNumberField("Columns", cols, 5, 18, { cols = it })
                LabeledNumberField("Mines", mines, 1, rows * cols / 2, { mines = it })
            }
        }
    )
}

@Composable
fun LabeledNumberField(label: String, value: Int, min: Int, max: Int, onValueChange: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                enabled = value > min, 
                onClick = { onValueChange(value - 1) },
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.size(36.dp, 36.dp)
            ) { Text("-") }
            Spacer(modifier = Modifier.width(14.dp))
            Text("$value", fontWeight = FontWeight.Medium, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Button(
                enabled = value < max,
                onClick = { onValueChange(value + 1) },
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.size(36.dp, 36.dp)
            ) { Text("+") }
        }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameGrid(
    grid: List<List<Cell>>,
    enabled: Boolean,
    onCellClick: (Int, Int) -> Unit,
    onCellLongPress: (Int, Int) -> Unit
) {
    // Responsive cell size calculation
    val rows = grid.size
    val cols = if (rows > 0) grid[0].size else 0
    val cellSize = if (rows > 0 && cols > 0) (250 / maxOf(rows, cols)).coerceAtLeast(26) else 32

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .border(2.dp, Color(0xFF388E3C), RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        for (row in 0 until rows) {
            Row {
                for (col in 0 until cols) {
                    val cell = grid[row][col]
                    Box(
                        modifier = Modifier
                            .size(cellSize.dp)
                            .padding(2.dp)
                            .background(
                                color = when {
                                    !cell.revealed -> Color(0xFFEEEEEE)
                                    cell.isMine && cell.revealed -> Color(0xFFFFCDD2)
                                    else -> Color.White
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(4.dp))
                            .combinedClickable(
                                enabled = enabled,
                                onClick = { onCellClick(row, col) },
                                onLongClick = { onCellLongPress(row, col) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CellContent(cell)
                    }
                }
            }
        }
    }
}

@Composable
fun CellContent(cell: Cell) {
    when {
        !cell.revealed && cell.flagged -> {
            Text("\uD83D\uDEA9", // flag emoji
                fontSize = 17.sp,
                color = Color(0xFF1976D2),
                textAlign = TextAlign.Center)
        }
        !cell.revealed -> {
            Text("", fontSize = 16.sp)
        }
        cell.isMine -> {
            Text("\uD83D\uDCA3", // mine emoji
                fontSize = 17.sp,
                color = Color.Red,
                textAlign = TextAlign.Center)
        }
        cell.adjacentMines > 0 -> {
            Text(cell.adjacentMines.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = adjacentMineColor(cell.adjacentMines),
                textAlign = TextAlign.Center)
        }
        else -> {
            Text("", fontSize = 16.sp)
        }
    }
}

// Utility logic

fun formatSeconds(sec: Int): String = "%02d:%02d".format(sec/60, sec%60)

/** Place mines randomly, not on or around initial tap */
fun placeMines(grid: List<List<Cell>>, initRow: Int, initCol: Int, mineCount: Int) {
    val rows = grid.size
    val cols = if (rows > 0) grid[0].size else 0
    val safeZone = mutableSetOf<Pair<Int, Int>>()
    for (r in (initRow-1)..(initRow+1)) for (c in (initCol-1)..(initCol+1)) {
        if (r in 0 until rows && c in 0 until cols) safeZone.add(r to c)
    }
    var placed = 0
    val positions = (0 until rows).flatMap { r -> (0 until cols).map { c -> r to c } }.shuffled()
    for ((r, c) in positions) {
        if (placed >= mineCount) break
        if ((r to c) in safeZone) continue
        val cell = grid[r][c]
        if (!cell.isMine) {
            cell.isMine = true
            placed++
        }
    }
}

fun updateAdjacents(grid: List<List<Cell>>) {
    val rows = grid.size
    val cols = if (rows > 0) grid[0].size else 0
    for (r in 0 until rows) for (c in 0 until cols) {
        val cell = grid[r][c]
        if (cell.isMine) continue
        cell.adjacentMines = getNeighbors(r,c,rows,cols).count { grid[it.first][it.second].isMine }
    }
}

fun revealAdjacentEmpties(grid: List<List<Cell>>, r: Int, c: Int) {
    val rows = grid.size
    val cols = grid[0].size
    val stack = ArrayDeque<Pair<Int,Int>>()
    stack.add(r to c)
    while (stack.isNotEmpty()) {
        val (cr, cc) = stack.removeLast()
        val cell = grid[cr][cc]
        if (cell.revealed || cell.isMine) continue
        cell.revealed = true
        if (cell.adjacentMines == 0) {
            for ((nr, nc) in getNeighbors(cr, cc, rows, cols))
                if (!grid[nr][nc].revealed) stack.add(nr to nc)
        }
    }
}

fun revealAllMines(grid: List<List<Cell>>) {
    for (row in grid) for (cell in row) {
        if (cell.isMine) cell.revealed = true
    }
}

fun checkWin(grid: List<List<Cell>>, mineCount: Int): Boolean {
    val total = grid.sumOf { it.size }
    val nonMinesRevealed = grid.flatten().count { !it.isMine && it.revealed }
    return nonMinesRevealed == total - mineCount
}

fun getNeighbors(r:Int, c:Int, numRows:Int, numCols:Int): List<Pair<Int,Int>> =
    ((r-1)..(r+1)).flatMap { nr -> (c-1)..(c+1) }
        .filter { nr -> nr in 0 until numRows }
        .flatMap { nr -> ((c-1)..(c+1)).filter { nc -> nc in 0 until numCols }.map { nc -> nr to nc } }
        .filter { (nr,nc) -> !(nr==r && nc==c) }

fun adjacentMineColor(n: Int): Color =
    when(n) {
        1 -> Color(0xFF1976D2)
        2 -> Color(0xFF388E3C)
        3 -> Color(0xFFFFC107)
        4 -> Color(0xFF7B1FA2)
        5 -> Color.Red
        6 -> Color(0xFF00BCD4)
        7 -> Color(0xFF607D8B)
        else -> Color.Black
    }


