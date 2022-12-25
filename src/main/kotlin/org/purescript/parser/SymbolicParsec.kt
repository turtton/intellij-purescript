package org.purescript.parser

import com.intellij.psi.tree.IElementType
import org.purescript.parser.Info.Failure

class SymbolicParsec(private val ref: Parsec, private val node: IElementType) :
    Parsec() {
    override fun parse(context: ParserContext): Info {
        val pack = context.start()
        val info = ref.parse(context)
        if (info !is Failure) {
            pack.done(node)
        } else {
            pack.drop()
        }
        return info
    }

}