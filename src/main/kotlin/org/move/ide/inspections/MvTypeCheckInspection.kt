package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.descendantsOfType
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.ext.structItem
import org.move.lang.core.types.infer.TypeError
import org.move.lang.core.types.infer.inference

class MvTypeCheckInspection : MvLocalInspectionTool() {
    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitItemSpec(o: MvItemSpec) {
                val inference = o.inference(true)
                inference.typeErrors
                    .filter { TypeError.isAllowedTypeError(it) }
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitModuleItemSpec(o: MvModuleItemSpec) {
                val inference = o.inference(true)
                inference.typeErrors
                    .filter { TypeError.isAllowedTypeError(it) }
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitFunction(o: MvFunction) {
                val msl = o.isMsl()
                val inference = o.inference(msl)
                inference.typeErrors
                    .filter { TypeError.isAllowedTypeError(it) }
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitStructField(field: MvStructField) {
                val structItem = field.structItem
                for (innerType in field.type?.descendantsOfType<MvPathType>().orEmpty()) {
                    val typeItem = innerType.path.reference?.resolve() as? MvStruct ?: continue
                    if (typeItem == structItem) {
                        holder.registerTypeError(TypeError.CircularType(innerType, structItem))
                    }
                }
            }
        }

    fun ProblemsHolder.registerTypeError(typeError: TypeError) {
        this.registerProblem(
            typeError.element,
            typeError.message(),
            ProblemHighlightType.GENERIC_ERROR,
        )
    }
}
