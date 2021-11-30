package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MoveElementTypes.R_BRACE
import org.move.lang.core.psi.MoveCodeBlock
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveExpr
import org.move.lang.core.psi.MoveFunctionDef
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.TypeVarsMap

fun MoveExpr.isLastExprInFunctionCodeBlock(): Boolean {
    val codeBlock = this.parent
    if (codeBlock !is MoveCodeBlock) return false
    if (codeBlock.parent !is MoveFunctionDef) return false
    return this.getNextNonCommentSibling()?.elementType == R_BRACE
}

abstract class MoveExprMixin(node: ASTNode) : MoveElementImpl(node), MoveExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? = null

}