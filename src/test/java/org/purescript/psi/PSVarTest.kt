package org.purescript.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.findDescendantOfType
import junit.framework.TestCase
import org.purescript.file.PSFile
import org.purescript.parser.PSLanguageParserTestBase

class PSVarTest : PSLanguageParserTestBase() {

    fun `test var can resolve to top level`() {
        val file = createFile(
            "Main.purs",
            """
            module Main where
            x = y
            y = 1
            """.trimIndent()
        ) as PSFile
        val psVar = file.getVarByName("y")!!
        val valueReference = psVar.referenceOfType(ValueReference::class.java)
        val valueDeclaration = valueReference.resolve()!!
        TestCase.assertEquals("y", valueDeclaration.name)
    }

    fun `test var can see all value declarations`() {
        val file = createFile(
            "Main.purs",
            """
            module Main where
            x = y
            y = 1
            """.trimIndent()
        ) as PSFile
        val psVar = file.getVarByName("y")!!
        val valueReference = psVar.referenceOfType(ValueReference::class.java)
        val names = valueReference.variants.map { it.name }
        assertContainsElements(names, "x", "y")
    }

    fun `test var can resolve to parameter`() {
        val file = createFile(
            "Main.purs",
            """
            module Main where
            x y = y
            """.trimIndent()
        ) as PSFile
        val psVar = file.getVarByName("y")!!
        val parameterReference = psVar.referenceOfType(ParameterReference::class.java)
        val identifier = parameterReference.resolve()!!
        TestCase.assertEquals("y", identifier.name)
    }

    fun `test var see all parameters`() {
        val file = createFile(
            "Main.purs",
            """
            module Main where
            x y z = y
            """.trimIndent()
        ) as PSFile
        val psVar = file.getVarByName("y")!!
        val parameterReference = psVar.referenceOfType(ParameterReference::class.java)
        val names = parameterReference.variants.map { it?.name }
        assertContainsElements(names, "z", "y")
    }
}

private fun PsiElement.getVarByName(
    name: String
): PSVar? {
    return this.findDescendantOfType({ true }, { it.text.trim() == name})
}

private fun <T> PsiElement.referenceOfType(
    referenceType: Class<T>
): T {
    return this
        .references
        .filterIsInstance(referenceType)
        .first()
}