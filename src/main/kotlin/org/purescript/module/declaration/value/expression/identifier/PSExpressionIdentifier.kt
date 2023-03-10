package org.purescript.module.declaration.value.expression.identifier

import com.intellij.lang.ASTNode
import org.purescript.psi.PSPsiElement
import org.purescript.module.declaration.value.ValueDeclarationGroup
import org.purescript.module.declaration.value.expression.ExpressionAtom
import org.purescript.module.declaration.value.expression.Qualified
import org.purescript.name.PSQualifiedIdentifier

/**
 * A identifier in an expression, e.g.
 * ```
 * add
 * ```
 * in
 * ```
 * f = add 1 3
 * ```
 */
class PSExpressionIdentifier(node: ASTNode) : PSPsiElement(node), ExpressionAtom, Qualified {

    val arguments: Sequence<Argument> get() = when(val parent = parent) {
        is Call -> parent.arguments
        else -> emptySequence()
    }

    /**
     * @return the [PSQualifiedIdentifier] identifying this constructor
     */
    internal val qualifiedIdentifier: PSQualifiedIdentifier
        get() = findNotNullChildByClass(PSQualifiedIdentifier::class.java)

    override val qualifierName: String? get() = qualifiedIdentifier.moduleName?.name

    override fun getName(): String = qualifiedIdentifier.name

    override fun getReference(): ExpressionIdentifierReference =
        ExpressionIdentifierReference(this)

    override fun areSimilarTo(other: org.purescript.module.declaration.value.expression.Expression): Boolean {
        val ref = reference.resolve()
        val otherRef = other.reference?.resolve()
        return when {
            ref == otherRef -> true
            ref is ValueDeclarationGroup && otherRef is ValueDeclarationGroup -> ref.valueDeclarations
                .zip(otherRef.valueDeclarations) { a, b -> a.value.areSimilarTo(b.value) }
                .all { it }

            else -> false
        }
    }
}