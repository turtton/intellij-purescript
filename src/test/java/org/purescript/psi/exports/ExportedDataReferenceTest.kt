package org.purescript.psi.exports

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.purescript.getModule

class ExportedDataReferenceTest : BasePlatformTestCase() {

    fun `test resolves to data declaration`() {
        val module = myFixture.configureByText(
            "Foo.purs",
            """
                module Foo (Bar) where
                data Bar = Qux
            """.trimIndent()
        ).getModule()
        val exportedData = module.exportList!!.exportedItems.single() as PSExportedData
        val dataDeclaration = module.dataDeclarations.single()

        TestCase.assertEquals(dataDeclaration, exportedData.reference.resolve())
    }

    fun `test resolve fails when no data declaration exists`() {
        val module = myFixture.configureByText(
            "Foo.purs",
            """
                module Foo (Bar) where
            """.trimIndent()
        ).getModule()
        val exportedData = module.exportList!!.exportedItems.single() as PSExportedData

        TestCase.assertNull(exportedData.reference.resolve())
    }

    fun `test completes data declarations`() {
        myFixture.configureByText(
            "Foo.purs",
            """
                module Foo (B<caret>) where
                data Bar = Qux
                data Baz = Qux
            """.trimIndent()
        )
        myFixture.testCompletionVariants("Foo.purs", "Bar", "Baz")
    }

    fun `test finds usage of data declaration`() {
        val module = myFixture.configureByText(
            "Foo.purs",
            """
                module Foo (Bar) where
                data <caret>Bar = Qux
            """.trimIndent()
        ).getModule()
        val exportedData = module.exportList!!.exportedItems.single() as PSExportedData
        val usageInfo = myFixture.testFindUsages("Foo.purs").single()

        TestCase.assertEquals(exportedData, usageInfo.element)
    }
}
