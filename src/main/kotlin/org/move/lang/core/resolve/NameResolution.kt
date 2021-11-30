package org.move.lang.core.resolve

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.descendantsOfType
import org.move.cli.metadata
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.types.HasType
import org.move.lang.core.types.RefType
import org.move.lang.core.types.StructType
import java.nio.file.Paths

fun processItems(
    element: MoveReferenceElement,
    namespace: Namespace,
    processor: MatchingProcessor,
): Boolean {
    return walkUpThroughScopes(
        element,
        stopAfter = { it is MoveModuleDef || it is MoveScriptDef }
    ) { cameFrom, scope ->
        processLexicalDeclarations(
            scope, cameFrom, namespace, processor
        )
    }
}


fun resolveItem(element: MoveReferenceElement, namespace: Namespace): MoveNamedElement? {
    var resolved: MoveNamedElement? = null
    processItems(element, namespace) {
        if (it.name == element.referenceName && it.element != null) {
            resolved = it.element
            return@processItems true
        }
        return@processItems false
    }
    return resolved
}

fun resolveModuleRefIntoQual(moduleRef: MoveModuleRef): MoveFullyQualifiedModuleRef? {
    if (moduleRef is MoveFullyQualifiedModuleRef) {
        return moduleRef
    }
    // module refers to ModuleImport
    val resolved = resolveItem(moduleRef, Namespace.MODULE)
    if (resolved is MoveImportAlias) {
        return (resolved.parent as MoveModuleImport).fullyQualifiedModuleRef
    }
    if (resolved is MoveItemImport && resolved.text == "Self") {
        return resolved.parentImport().fullyQualifiedModuleRef
    }
    if (resolved !is MoveModuleImport) return null

    return resolved.fullyQualifiedModuleRef
}

private fun resolveQualModuleRefInFile(
    qualModuleRef: MoveFullyQualifiedModuleRef,
    file: MoveFile,
    processor: MatchingProcessor,
): Boolean {
    val sourceNormalizedAddress = qualModuleRef.addressRef.address()?.normalized()

    var resolved = false
    val visitor = object : MoveVisitor() {
        override fun visitModuleDef(o: MoveModuleDef) {
            if (resolved) return
            val normalizedAddress = o.definedAddressRef()?.address()?.normalized()
            if (normalizedAddress == sourceNormalizedAddress) {
                resolved = processor.match(o)
            }
        }
    }
    val moduleDefs = file.descendantsOfType<MoveModuleDef>()
    for (moduleDef in moduleDefs) {
        moduleDef.accept(visitor)
    }
    return resolved
}

fun processQualModuleRef(
    qualModuleRef: MoveFullyQualifiedModuleRef,
    processor: MatchingProcessor,
): Boolean {
    val project = qualModuleRef.project

    // first search modules in the current file
    val containingFile = qualModuleRef.containingFile as? MoveFile ?: return false
    var isResolved = resolveQualModuleRefInFile(qualModuleRef, containingFile, processor)
    if (isResolved) return true

    val currentPathPathS = containingFile.virtualFile?.canonicalPath ?: return false
    val currentFilePath = Paths.get(currentPathPathS)

    val metadata = project.metadata(currentFilePath) ?: return false
    metadata.iterOverMoveModuleFiles { moduleFile ->
        isResolved = resolveQualModuleRefInFile(qualModuleRef, moduleFile.file, processor)
        !isResolved
    }
    return isResolved
}

fun processNestedScopesUpwards(
    startElement: MoveElement,
    namespace: Namespace,
    processor: MatchingProcessor,
) {
    walkUpThroughScopes(
        startElement,
        stopAfter = { it is MoveModuleDef || it is MoveScriptDef }
    ) { cameFrom, scope ->
        processLexicalDeclarations(
            scope, cameFrom, namespace, processor
        )
    }
}

fun processLexicalDeclarations(
    scope: MoveElement,
    cameFrom: MoveElement,
    namespace: Namespace,
    processor: MatchingProcessor,
): Boolean {
    check(cameFrom.parent == scope)

    return when (namespace) {
        Namespace.DOT_ACCESSED_FIELD -> {
            val dotExpr = scope as? MoveDotExpr ?: return false
            val refExpr = dotExpr.refExpr ?: return false

            val referred = refExpr.reference?.resolve()
            if (referred !is HasType) return false

            val resolvedType = referred.resolvedType(emptyMap())
            val structDef = when (resolvedType) {
                is StructType -> resolvedType.structDef()
                is RefType -> resolvedType.referredStructDef()
                else -> null
            }
            return processor.matchAll(structDef?.fields.orEmpty())
        }
        Namespace.STRUCT_FIELD -> {
            val structDef = (scope as? MoveQualTypeReferenceElement)?.referredStructDef
            if (structDef != null) {
                return processor.matchAll(structDef.fields)
            }
            false
        }
        Namespace.NAME -> when (scope) {
            is MoveFunctionDef -> processor.matchAll(scope.functionSignature?.parameters.orEmpty())
            is MoveCodeBlock -> {
                val precedingLetDecls = scope.letStatements
                    // drops all let-statements after the current position
                    .filter { PsiUtilCore.compareElementsByPosition(it, cameFrom) <= 0 }
                    // drops let-statement that is ancestors of ref (on the same statement, at most one)
                    .filter { cameFrom != it && !PsiTreeUtil.isAncestor(cameFrom, it, true) }

                // shadowing support (look at latest first)
                val namedElements = precedingLetDecls
                    .asReversed()
                    .flatMap { it.pat?.boundElements.orEmpty() }

                // skip shadowed (already visited) elements
                val visited = mutableSetOf<String>()
                val processorWithShadowing = MatchingProcessor { entry ->
                    ((entry.name !in visited)
                            && processor.match(entry).also { visited += entry.name })
                }
                return processorWithShadowing.matchAll(namedElements)
            }
//            is MoveSpecBlock -> {
//                processor.matchAll(scope.defineFunctionList)
//            }
            is MoveModuleDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.allFnSignatures(),
                    scope.builtinFnSignatures(),
                    scope.structSignatures(),
                    scope.consts(),
                ).flatten()
            )
            is MoveScriptDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
//                    scope.builtinFunctions(),
                    scope.consts()
                ).flatten(),
            )
            else -> false
        }
        Namespace.TYPE -> when (scope) {
            is MoveFunctionDef -> processor.matchAll(scope.functionSignature?.typeParameters.orEmpty())
            is MoveNativeFunctionDef -> processor.matchAll(scope.functionSignature?.typeParameters.orEmpty())
            is MoveStructDef -> processor.matchAll(scope.structSignature.typeParameters)
            is MoveSchemaSpecDef -> processor.matchAll(scope.typeParams)
            is MoveModuleDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.structSignatures(),
                ).flatten(),
            )
            is MoveScriptDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                ).flatten(),
            )
            else -> false
        }
        Namespace.SCHEMA -> when {
//            is MoveModuleDef -> processor.matchAll(scope.schemas())
            else -> false
        }
        Namespace.MODULE -> when (scope) {
            is MoveImportStatementsOwner -> processor.matchAll(
                listOf(
                    scope.moduleImports(),
                    scope.moduleImportAliases(),
                    scope.selfItemImports(),
                ).flatten(),
            )
            else -> false
        }
    }
}

fun walkUpThroughScopes(
    start: MoveElement,
    stopAfter: (MoveElement) -> Boolean,
    handleScope: (cameFrom: MoveElement, scope: MoveElement) -> Boolean,
): Boolean {

    var cameFrom = start
    var scope = start.parent as MoveElement?
    while (scope != null) {
        if (handleScope(cameFrom, scope)) return true
        if (stopAfter(scope)) break

        cameFrom = scope
        scope = scope.parent as MoveElement?
    }

    return false
}
