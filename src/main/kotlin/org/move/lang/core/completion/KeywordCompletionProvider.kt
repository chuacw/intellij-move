package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.util.ProcessingContext

class KeywordCompletionProvider(private vararg val keywords: String) : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        for (keyword in keywords) {
            var element = LookupElementBuilder.create(keyword).bold()
            element = element.withInsertHandler { ctx, _ ->
                ctx.document.insertString(ctx.selectionEndOffset, " ")
                EditorModificationUtil.moveCaretRelatively(ctx.editor, 1)
            }
            result.addElement(PrioritizedLookupElement.withPriority(element, 1.0))
        }
    }
}


//private val ALWAYS_NEEDS_SPACE = setOf("use", "let", "mut")

//private fun addInsertionHandler(keyword: String, builder: LookupElementBuilder, parameters: CompletionParameters): LookupElementBuilder {
//    val suffix = when (keyword) {
//        in ALWAYS_NEEDS_SPACE -> " "
//        "return" -> {
//            val fn = parameters.position.ancestorStrict<RsFunction>() ?: return builder
//            if (fn.returnType !is TyUnit) " " else ";"
//        }
//        else -> return builder
//    }
//
//    return builder.withInsertHandler { ctx, _ -> ctx.addSuffix(suffix) }
//}
