package org.purescript.psi.binder

import com.intellij.lang.ASTNode
import org.purescript.psi.PSPsiElement

class PSBinder(node: ASTNode?) : PSPsiElement(node!!)