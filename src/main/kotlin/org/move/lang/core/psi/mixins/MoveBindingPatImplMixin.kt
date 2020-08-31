package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveBindingPat
import org.move.lang.core.psi.impl.MoveNamedIdentifierOwnerImpl

abstract class MoveBindingPatImplMixin(node: ASTNode) : MoveNamedIdentifierOwnerImpl(node),
                                                        MoveBindingPat {

}