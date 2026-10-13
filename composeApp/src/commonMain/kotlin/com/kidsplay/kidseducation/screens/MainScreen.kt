package com.kidsplay.kidseducation.screens

import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsplay.kidseducation.AppState
import com.kidsplay.kidseducation.LocalAppState
import com.kidsplay.kidseducation.data.ActivityKind
import com.kidsplay.kidseducation.nav.GridMode
import com.kidsplay.kidseducation.nav.MainTab
import com.kidsplay.kidseducation.nav.Navigator
import com.kidsplay.kidseducation.nav.Screen
import com.kidsplay.kidseducation.rememberAsync
import com.kidsplay.kidseducation.ui.BigButton
import com.kidsplay.kidseducation.ui.CenteredMessage
import com.kidsplay.kidseducation.ui.GameImage
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.RoundIconButton
import com.kidsplay.kidseducation.ui.ScreenFrame
import com.kidsplay.kidseducation.ui.TileCard
import com.kidsplay.kidseducation.platform.LocalDeps
import com.kidsplay.kidseducation.platform.PlatformDeps

/** The five bottom-navigation destinations. */
@Composable
fun MainScreen(nav: Navigator) {
    val app = LocalAppState.current
    var askName by remember { mutableStateOf(!app.nameAsked && app.userName.isBlank()) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (nav.mainTab) {
                MainTab.Home -> HomeTab(nav)
                MainTab.Look -> CategoriesBody(GridMode.Look, nav, title = "Look & Choose!", onBack = null)
                MainTab.Listen -> CategoriesBody(GridMode.Listen, nav, title = "Listen and Guess", onBack = null)
                MainTab.Videos -> VideoCategoriesBody(nav)
                MainTab.Math -> CategoriesBody(GridMode.Math, nav, title = "Mathematics for Kids", onBack = null)
            }
        }
        KidsBottomBar(nav.mainTab) { tab -> app.stopSpeaking(); nav.mainTab = tab }
        // One banner for the whole tabbed area (Home / Look / Listen / Videos / Maths), at the very bottom.
        LocalDeps.current.ads.Banner(Modifier.fillMaxWidth().background(Color.White))
    }

    if (askName) NameDialog(
        onSave = { app.updateName(it); app.nameAsked = true; askName = false },
        onSkip = { app.nameAsked = true; askName = false },
    )
}

private val tabColors = listOf(
    Color(0xFF3A6FD8), Color(0xFFEC407A), Color(0xFF7E57C2), Color(0xFFFF9800), Color(0xFF4CAF50),
)

/**
 * Rounded, floating-style bottom bar: every tab has its own colour; the selected one gets a coloured
 * badge that pops up (spring animation), a soft tinted pill and a darker label.
 */
@Composable
private fun KidsBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    val barShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Row(
        Modifier.fillMaxWidth().shadow(16.dp, barShape).background(Color.White, barShape)
            .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainTab.entries.forEachIndexed { index, tab ->
            val isSelected = tab == selected
            val color = tabColors[index % tabColors.size]
            val scale by animateFloatAsState(
                if (isSelected) 1.18f else 1f,
                spring(dampingRatio = 0.45f, stiffness = 380f), label = "tabScale",
            )
            Column(
                Modifier.weight(1f).padding(horizontal = 3.dp).clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onSelect(tab) }.padding(top = 6.dp, bottom = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(42.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                        .then(if (isSelected) Modifier.shadow(6.dp, CircleShape) else Modifier)
                        .clip(CircleShape).background(if (isSelected) color else Color(0xFFEFF3FB)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tab.emoji, fontSize = 21.sp, lineHeight = 21.sp,
                        style = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
                    )
                }
                Text(
                    tab.label, fontSize = 11.sp, maxLines = 1, style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) lerp(color, Color.Black, 0.2f) else Color(0xFF6B7280),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun NameDialog(onSave: (String) -> Unit, onSkip: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("What's your name?") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it.take(20) }, singleLine = true, label = { Text("Your name") })
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Let's go!") } },
        dismissButton = { TextButton(onClick = onSkip) { Text("Skip") } },
    )
}

// ------------------------------------------------------------------------------------------ Home

private sealed interface TileAction {
    data class Learn(val category: Int) : TileAction
    data class Video(val categoryId: Int) : TileAction
    data class Write(val number: Int) : TileAction
    data class Pattern(val roundSize: Int) : TileAction
    data object Draw : TileAction
    data object LifeSkills : TileAction
}

private class HomeTile(val title: String, val image: String, val action: TileAction)
private class HomeSection(
    val title: String,
    val tiles: List<HomeTile>,
    /** Draw section: opens the real colouring editor on Android instead of pushing a screen. */
    val isDraw: Boolean = false,
    val viewAll: (Navigator) -> Unit,
)

private fun learnTiles(vararg entries: Pair<String, Pair<Int, String>>) =
    entries.map { (title, v) -> HomeTile(title, v.second, TileAction.Learn(v.first)) }

private val homeSections: List<HomeSection> = listOf(
    HomeSection(
        "English & Alphabets",
        learnTiles(
            "Alphabets" to (0 to "home_alphabet"), "Body Parts" to (10 to "home_body_parts"),
            "Clothes" to (11 to "home_clothes"), "Jobs" to (16 to "home_jobs"), "School" to (17 to "home_school"),
        ),
    ) { it.push(Screen.Categories(GridMode.Learn)) },
    HomeSection(
        "Mathematics",
        learnTiles(
            "Numbers" to (1 to "home_number"), "Shapes" to (3 to "home_shape"),
            "Geometry" to (14 to "home_geometry"), "Month" to (8 to "home_month"),
        ),
    ) { it.mainTab = MainTab.Math },
    HomeSection(
        "Animals & Nature",
        learnTiles(
            "Animals" to (4 to "home_animal"), "Birds" to (5 to "home_birds"), "Flowers" to (6 to "home_flower"),
            "Fruits" to (7 to "home_fruits"), "Vegetable" to (9 to "home_vegetable"),
        ),
    ) { it.push(Screen.Categories(GridMode.Learn)) },
    HomeSection(
        "Colors, Food & More",
        learnTiles(
            "Colors" to (2 to "home_color"), "Food" to (13 to "home_food"), "Country" to (12 to "home_country"),
            "House" to (15 to "home_house"), "Sports" to (18 to "home_sports"), "Vehicle" to (19 to "home_vehicle"),
        ),
    ) { it.push(Screen.Categories(GridMode.Learn)) },
    HomeSection(
        "Golden Words",
        listOf(
            HomeTile("Please", "please", TileAction.Learn(20)),
            HomeTile("Thank You", "thank_you", TileAction.Learn(20)),
            HomeTile("Sorry", "sorry", TileAction.Learn(20)),
        ),
    ) { it.push(Screen.LearnList(20)) },
    HomeSection(
        "Learn Writing",
        listOf(1 to "one_1", 2 to "two_2", 3 to "three_3", 4 to "four_4").map { (n, img) ->
            HomeTile("Write $n", img, TileAction.Write(n))
        },
    ) { it.push(Screen.NumberList) },
    HomeSection(
        "Match Pattern",
        listOf(3 to "a_apple", 4 to "b_ball", 5 to "c_cat", 6 to "d_dog", 7 to "e_elephant").mapIndexed { i, (size, img) ->
            HomeTile("Level ${i + 1}", img, TileAction.Pattern(size))
        },
    ) { it.push(Screen.PatternLevels) },
    HomeSection(
        "Draw",
        listOf(
            HomeTile("Colouring", "painter", TileAction.Draw),
            HomeTile("Brush", "brush", TileAction.Draw),
            HomeTile("Pencil", "pencil", TileAction.Draw),
        ),
        isDraw = true,
    ) { it.push(Screen.Draw) },
    HomeSection(
        "About Me & Life Skills",
        listOf(
            "About Me" to "ic_lifeskills_aboutme", "Family" to "ic_lifeskills_family", "School" to "ic_lifeskills_school",
            "Manners" to "ic_lifeskills_manners", "Safety" to "ic_lifeskills_safety",
        ).map { (t, img) -> HomeTile(t, img, TileAction.LifeSkills) },
    ) { it.push(Screen.LifeLevels) },
    HomeSection(
        "Lines",
        listOf(
            "Standing Line" to "line_standing", "Sleeping Line" to "line_sleeping", "Slanting Line" to "line_slanting",
            "Zig Zag Line" to "line_zigzag", "Curved Line" to "line_curved", "Wavy Line" to "line_wavy",
        ).map { (t, img) -> HomeTile(t, img, TileAction.Learn(21)) },
    ) { it.push(Screen.LearnList(21)) },
    HomeSection(
        "Top Videos",
        listOf(
            Triple("Rhymes", "vt_abc", 11), Triple("ABC Songs", "vt_abc", 0), Triple("Number Songs", "vt_number", 1),
            Triple("Animal Songs", "vt_animal", 3), Triple("Shape Songs", "vt_shape", 4), Triple("Color Songs", "vt_color", 2),
        ).map { (t, img, id) -> HomeTile(t, img, TileAction.Video(id)) },
    ) { it.mainTab = MainTab.Videos },
)

private fun Navigator.openDraw(deps: PlatformDeps, app: AppState) {
    val open = deps.openDrawing
    if (open != null) {
        app.log(ActivityKind.Draw, "Colouring & drawing")
        open()
    } else push(Screen.Draw)
}

private fun Navigator.go(action: TileAction, deps: PlatformDeps, app: AppState) = when (action) {
    is TileAction.Learn -> push(Screen.LearnList(action.category))
    is TileAction.Video -> push(Screen.VideoList(action.categoryId))
    is TileAction.Write -> push(Screen.TraceNumber(action.number))
    is TileAction.Pattern -> push(Screen.MatchGame(action.roundSize))
    TileAction.Draw -> openDraw(deps, app)
    TileAction.LifeSkills -> push(Screen.LifeLevels)
}

@Composable
private fun HomeTab(nav: Navigator) {
    val app = LocalAppState.current
    val deps = LocalDeps.current
    val ads = deps.ads
    Box(Modifier.fillMaxSize().background(KidsColors.PageBrush)) {
        Column(Modifier.fillMaxSize()) {
            // ---- header: avatar + greeting + stars + settings
            Column(Modifier.fillMaxWidth().background(KidsColors.HeaderBottom)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(Color.White).border(2.dp, KidsColors.Sun, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            app.userName.firstOrNull()?.uppercase() ?: "😊",
                            color = KidsColors.HeaderBottom, fontSize = 18.sp, style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (app.userName.isBlank()) "Hello, Friend!" else "Hi, ${app.userName}!",
                            color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text("Let's learn and play today! 🎈", color = Color.White.copy(alpha = 0.95f), fontSize = 11.sp)
                    }
                    Row(
                        Modifier.clip(RoundedCornerShape(20.dp)).background(KidsColors.Sun)
                            .clickable { nav.push(Screen.LifeLevels) }.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⭐ ${app.store.totalStars}", color = KidsColors.Ink, fontSize = 12.sp, style = MaterialTheme.typography.titleMedium)
                    }
                    RoundIconButton("⚙️", { nav.push(Screen.Settings) }, size = 34.dp)
                }
                GameImage("header_scallop", Modifier.fillMaxWidth().height(10.dp), ContentScale.FillBounds)
            }
            LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp)) {
                item { TopCategories(nav) }
                itemsIndexed(homeSections) { index, section ->
                    val accent = KidsColors.Rainbow[index % KidsColors.Rainbow.size]
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(6.dp).height(20.dp).clip(RoundedCornerShape(4.dp)).background(accent))
                        Text(
                            section.title, fontSize = 16.sp, color = KidsColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(start = 10.dp),
                        )
                        Text(
                            "View All ›",
                            color = Color.White, fontSize = 11.sp,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(KidsColors.HeaderBottom)
                                .clickable { ads.onViewAllTap { if (section.isDraw) nav.openDraw(deps, app) else section.viewAll(nav) } }.padding(horizontal = 11.dp, vertical = 4.dp),
                        )
                    }
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(section.tiles) { tile -> HomeTileView(tile, accent) { nav.go(tile.action, deps, app) } }
                    }
                }
            }
        }
        // Floating "Kids Reels" button: the old app's reel_logo on a white circle with a yellow ring.
        val pulse by rememberInfiniteTransition(label = "reelPulse").animateFloat(
            initialValue = 1f, targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "reelPulseScale",
        )
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .size(60.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                .background(Color.White).border(2.dp, KidsColors.Sun, CircleShape)
                .clickable { nav.push(Screen.Reels) }.padding(3.dp),
            contentAlignment = Alignment.Center,
        ) { GameImage("reel_logo", Modifier.fillMaxSize()) }
    }
}

@Composable
private fun HomeTileView(tile: HomeTile, accent: Color, onClick: () -> Unit) {
    TileCard(Modifier.width(112.dp), borderColor = accent, onClick = onClick) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GameImage(tile.image, Modifier.size(92.dp), ContentScale.Fit)
            Text(
                tile.title, style = MaterialTheme.typography.labelLarge, fontSize = 12.sp, color = KidsColors.Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// ------------------------------------------------------------------------------------ Categories

/** Full category grid. Used as a bottom tab (Look / Listen / Math) and as the "View All" page. */
@Composable
fun CategoriesScreen(mode: GridMode, nav: Navigator, onBack: () -> Unit) {
    val title = when (mode) {
        GridMode.Learn -> "Kids Learning"
        GridMode.Look -> "Look & Choose!"
        GridMode.Listen -> "Listen and Guess"
        GridMode.Math -> "Mathematics for Kids"
    }
    CategoriesBody(mode, nav, title, onBack)
}

private val mathCategories = listOf(1, 3, 14, 8) // Numbers, Shapes, Geometry, Month

@Composable
fun CategoriesBody(mode: GridMode, nav: Navigator, title: String, onBack: (() -> Unit)?) {
    val app = LocalAppState.current
    val categories by rememberAsync { app.content.learningCategories() }
    ScreenFrame(title, onBack, showBanner = onBack != null) {
        val all = categories
        if (all == null) { CenteredMessage("Loading…"); return@ScreenFrame }
        val shown = if (mode == GridMode.Math) mathCategories.mapNotNull { idx -> all.firstOrNull { it.index == idx } } else all
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(shown) { category ->
                TileCard(onClick = {
                    nav.push(
                        when (mode) {
                            GridMode.Learn, GridMode.Math -> Screen.LearnList(category.index)
                            GridMode.Look -> Screen.LookChoose(category.index)
                            GridMode.Listen -> Screen.ListenGuess(category.index)
                        },
                    )
                }) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        GameImage(category.icon, Modifier.size(110.dp), ContentScale.Fit)
                        Text(category.title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------- Video categories

private val videoCategoryIcons = mapOf(
    0 to "vt_abc", 1 to "vt_number", 2 to "vt_color", 3 to "vt_animal", 4 to "vt_shape", 6 to "vt_fruit",
    7 to "vt_vegetable", 8 to "vt_day", 9 to "vt_month", 10 to "vt_clothes", 11 to "vt_abc",
)

@Composable
fun VideoCategoriesBody(nav: Navigator) {
    val app = LocalAppState.current
    val categories by rememberAsync { app.content.videoCategories() }
    ScreenFrame("Video Song", onBack = null, showBanner = false) {
        val list = categories
        if (list == null) { CenteredMessage("Loading…"); return@ScreenFrame }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(list) { category ->
                TileCard(onClick = { nav.push(Screen.VideoList(category.id)) }) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                            val icon = videoCategoryIcons[category.id]
                            if (icon != null) GameImage(icon, Modifier.size(100.dp)) else Text("🚗", fontSize = 56.sp)
                        }
                        Text(category.title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Top categories (old home 2x2 grid)

private class TopCategory(val label: String, val image: String, val color: Color, val go: (Navigator) -> Unit)

private val topCategories = listOf(
    TopCategory("Alpha & Numb", "blocks", Color(0xFFA8E6A1)) { it.push(Screen.Categories(GridMode.Learn)) },
    TopCategory("Video", "play", Color(0xFFC9B6F5)) { it.mainTab = MainTab.Videos },
    TopCategory("Quiz", "quiz", Color(0xFFFBC4E0)) { it.mainTab = MainTab.Look },
    TopCategory("Mathematics", "math", Color(0xFFFFD59E)) { it.push(Screen.MathMenu) },
)

@Composable
private fun TopCategories(nav: Navigator) {
    val app = LocalAppState.current
    val ads = LocalDeps.current.ads
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        topCategories.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { c ->
                    Column(
                        Modifier.weight(1f).shadow(6.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp))
                            .background(Brush.verticalGradient(listOf(lerp(c.color, Color.White, 0.55f), c.color)))
                            .border(3.dp, Color.White, RoundedCornerShape(26.dp))
                            .clickable { app.stopSpeaking(); ads.onHomeCategoryTap { c.go(nav) } }.padding(vertical = 16.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GameImage(c.image, Modifier.size(80.dp))
                        Text(
                            c.label, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp, color = KidsColors.Ink,
                            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
