package org.purescript.psi.declaration.fixity

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReferenceBase

class FixityReference(fixity: FixityDeclaration) :
    PsiReferenceBase<FixityDeclaration>(
        fixity,
        fixity.qualifiedIdentifier?.textRangeInParent,
        false
    ) {

    override fun getVariants(): Array<Any> =
        candidates.toList().toTypedArray()

    override fun resolve(): PsiNamedElement? {
        val name = element.qualifiedIdentifier?.identifier?.name
        return candidates.firstOrNull { it.name == name }
    }

    private val candidates: Sequence<PsiNamedElement>
        get() {
            val module = element.module ?: return emptySequence()
            val qualifyingName = element.qualifiedIdentifier?.moduleName?.name
            return sequence {
                if (qualifyingName == null) {
                    // TODO Support values defined in the expression
                    yieldAll(module.cache.valueDeclarationGroups.toList())
                    yieldAll(module.cache.foreignValueDeclarations.toList())
                    val localClassMembers = module
                        .cache.classes
                        .asSequence()
                        .flatMap { it.classMembers.asSequence() }
                    yieldAll(localClassMembers)
                }
                val importDeclarations =
                    module.cache.imports.filter { it.importAlias?.name == qualifyingName }
                yieldAll(importDeclarations.flatMap { it.importedValueDeclarationGroups })
                yieldAll(importDeclarations.flatMap { it.importedForeignValueDeclarations })
                yieldAll(importDeclarations.flatMap { it.importedClassMembers })
                val importedClassMembers =
                    importDeclarations
                        .asSequence()
                        .flatMap { it.importedClassDeclarations.asSequence() }
                        .flatMap { it.classMembers.asSequence() }
                yieldAll(importedClassMembers)
            }
        }
}