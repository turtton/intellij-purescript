package org.purescript.module.declaration.type.type

import com.intellij.lang.ASTNode
import org.purescript.inference.InferType
import org.purescript.inference.InferType.Companion.function
import org.purescript.psi.PSPsiElement

class TypeApp(node: ASTNode) : PSPsiElement(node), PSType {
    private val types get() = findChildrenByClass(PSType::class.java)

    /**
     * this is probably wrong but what this rule is doing
     * 
     * an expression like this in a type
     * f a
     * unifies f with a function that takes an a and return this
     * f := a -> this
     * 
     * Record is defined as  a -> Record a
     * which results in  f := a -> this := a -> Record a
     * 
     * Union is defined as  a -> b -> (a + b) -> Union a b (a + b)
     * 
     */
    override fun unify() {
        val (kind, argument) = types
        val kindType = kind?.inferType() ?: return
        val argumentType = argument?.inferType() ?: return
        if (kindType is InferType.App) {
            val returnType = substitutedType
            val callType = function(argumentType, returnType)
            unify(kindType, callType)
        } else {
            unify(kindType.app(argumentType))
        }
    }
}