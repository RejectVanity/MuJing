package ui.dialog

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import data.Word
import state.MemoryStrategy
import state.WordScreenState
import state.rememberDictationState

/**
 * 选择章节
 */
@ExperimentalComposeUiApi
@Composable
fun SelectChapterDialog(
    close:() -> Unit,
    wordRequestFocus:() -> Unit,
    wordScreenState: WordScreenState,
    isMultiple:Boolean
) {
    Dialog(
        title = if(isMultiple) "听写测试，可以选择多个章节" else "选择章节",
        onCloseRequest = { close() },
        resizable = true,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(930.dp, 785.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colors.background)
            ) {
                val chapterSize by remember {
                    derivedStateOf {
                        val size = wordScreenState.vocabulary.size
                        var count = size / 20
                        val mod = size % 20
                        if (mod != 0 && size > 20) count += 1
                        if (size < 20) count = 1
                        count
                    }
                }
                val selectedChapters  = remember{
                    if(isMultiple){
                        mutableStateListOf()
                    }else{
                        mutableStateListOf(wordScreenState.chapter)
                    }
                }

                var isSelectAll by remember { mutableStateOf(false) }

                val selectAll = {
                    if(isSelectAll){
                        selectedChapters.clear()
                        val list = (1 until chapterSize + 1).toList()
                        selectedChapters.addAll(list)
                    }else{
                        selectedChapters.clear()
                    }

                }

                val selectedWords = remember(selectedChapters){
                    derivedStateOf {
                        val list = mutableStateListOf<Word>()
                        selectedChapters.forEach { chapter ->
                            val start = chapter * 20 - 20
                            var end = chapter * 20
                            if(end > wordScreenState.vocabulary.wordList.size){
                                end = wordScreenState.vocabulary.wordList.size
                            }
                            val subList = wordScreenState.vocabulary.wordList.subList(start, end)
                            list.addAll(subList)
                        }
                        list
                    }
                }

                if(isMultiple){
                    Column(modifier = Modifier.align(Alignment.TopCenter)) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("选择了${selectedChapters.size}个章节  ", color = MaterialTheme.colors.onBackground)
                            Text("${selectedWords.value.size}", color = MaterialTheme.colors.primary)
                            Text(" 个单词", color = MaterialTheme.colors.onBackground)
                            Checkbox(
                                checked = isSelectAll,
                                onCheckedChange = {
                                    isSelectAll = it
                                    selectAll()
                                },
                                modifier = Modifier.padding(start = 20.dp)
                            )
                            Text("全选")
                        }
                        Divider()
                    }
                }


                Row(modifier = Modifier.align(Alignment.Center).padding(top = 48.dp, bottom = 55.dp)) {
                    Chapters(
                        checkedChapters = selectedChapters,
                        size = wordScreenState.vocabulary.size,
                        isMultiple = isMultiple,
                        onChapterSelected = {
                            if(!isMultiple){
                                selectedChapters.clear()
                                selectedChapters.add(it)
                            }else{
                                if(selectedChapters.contains(it)){
                                    selectedChapters.remove(it)
                                }else{
                                    selectedChapters.add(it)
                                }
                            }
                        },
                    )
                }
                val dictationState = rememberDictationState()
                Footer(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    confirmEnable = selectedChapters.isNotEmpty(),
                    confirm = {
                        // 独立的听写测试，可以选择多个章节
                        if(isMultiple){
                            if(wordScreenState.memoryStrategy != MemoryStrategy.DictationTest && wordScreenState.memoryStrategy != MemoryStrategy.Dictation){
                                wordScreenState.hiddenInfo(dictationState)
                            }
                            wordScreenState.memoryStrategy = MemoryStrategy.DictationTest
                            wordScreenState.wrongWords.clear()
                            wordScreenState.reviewWords.clear()
                            wordScreenState.reviewWords.addAll(selectedWords.value.shuffled())
                            wordScreenState.dictationIndex = 0
                        // 非独立的听写测试，只能选择一个章节
                        }else{
                            val chapter = selectedChapters.first()
                            if (chapter == 0) wordScreenState.chapter = 1
                            wordScreenState.chapter = chapter
                            wordScreenState.index = (chapter - 1) * 20
                            wordScreenState.saveWordScreenState()
                            // 如果是独立的听写测试单词，又重新选择了章节，所以就取消独立的听写测试
                            if(wordScreenState.memoryStrategy == MemoryStrategy.DictationTest){
                                wordScreenState.memoryStrategy = MemoryStrategy.Normal
                                wordScreenState.showInfo()
                            }
                        }
                        // 如果不是独立的听写测试，同时正在听写单词，又重新选择了章节，所以就退出听写
                        if(wordScreenState.memoryStrategy == MemoryStrategy.Dictation && !isMultiple){
                            wordScreenState.showInfo()
                            wordScreenState.memoryStrategy = MemoryStrategy.Normal
                            if( wordScreenState.wrongWords.isNotEmpty()){
                                wordScreenState.wrongWords.clear()
                            }
                            if(wordScreenState.reviewWords.isNotEmpty()){
                                wordScreenState.reviewWords.clear()
                            }
                        }
                        wordScreenState.clearInputtedState()
                        close()

                        wordRequestFocus()

                    },
                    exit = { close() })
            }
        }
    }
}



@Composable
fun Chapters(
    size: Int,
    checkedChapters: List<Int>,
    onChapterSelected: (Int) -> Unit,
    isMultiple:Boolean
) {
    if(size>0){
        Box(
            modifier = Modifier.fillMaxWidth().background(color = MaterialTheme.colors.background)
        ) {
            var count = size / 20
            val mod = size % 20
            if (mod != 0 && size > 20) count += 1
            if (size < 20) count = 1
            val chapters = (1 until count + 1).map { "Chapter $it" }.toList()
            val listState = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(144.dp),
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier.fillMaxWidth(),
                state = listState
            ) {
                itemsIndexed(chapters) { index: Int, item: String ->
                    val chapter = index + 1
                    val checkedState = if(isMultiple){
                        checkedChapters.contains(chapter)
                    }else{
                        chapter == checkedChapters.first()
                    }

                    val onClick:() -> Unit = {
                        if(isMultiple){
                            onChapterSelected(chapter)
                        }else{
                            // 如果已经选择了这个章节，就取消选择这个章节，章节设置为0，
                            // 否则设置选择的章节
                            onChapterSelected(if (checkedState) 0 else chapter)
                        }

                    }

                    Card(
                        modifier = Modifier
                            .padding(7.5.dp)
                            .clickable {
                                onClick()
                            },
                        backgroundColor = MaterialTheme.colors.surface,
                        elevation = 3.dp
                    ) {
                        Box(Modifier.size(width = 144.dp, height = 65.dp)) {
                            Text(
                                text = item,
                                color = MaterialTheme.colors.onBackground,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                            )

                            val words = if (index == count - 1){
                                if(mod==0) 20 else mod
                            } else 20
                            Text(
                                text = "$words 词",
                                color = MaterialTheme.colors.onBackground,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
                            )
                            if(!isMultiple){
                                if (checkedState) {
                                    Checkbox(
                                        checked = true,
                                        onCheckedChange = { onClick()},
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    )
                                }
                            }else{
                                Checkbox(
                                    checked = checkedState,
                                    onCheckedChange = {  onClick()},
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }

                        }
                    }
                }

            }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(
                scrollState = listState
            )
        )

        }
    }

}

@Composable
fun Footer(
    modifier: Modifier,
    confirmEnable:Boolean,
    confirm: () -> Unit,
    exit: () -> Unit
) {
    Box(modifier = modifier) {
        Column {
            Divider(Modifier.fillMaxWidth())
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .background(color = MaterialTheme.colors.background)
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(top = 10.dp, bottom = 10.dp)
            ) {
                OutlinedButton(
                    enabled = confirmEnable,
                    onClick = { confirm() }
                ) {
                    Text(text = "确认")
                }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(onClick = { exit() }) {
                    Text(text = "取消")
                }
                Spacer(Modifier.width(10.dp))
            }
        }
    }


}