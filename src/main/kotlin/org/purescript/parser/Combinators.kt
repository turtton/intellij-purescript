package org.purescript.parser

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

object Combinators {
    fun token(tokenType: IElementType): Parsec = object : Parsec() {
        override fun parse(context: ParserContext): ParserInfo = ParserInfo(
            context.position,
            setOf(this),
            null,
            context.eat(tokenType)
        )


        public override fun calcName() = tokenType.toString()
        override fun calcExpectedName() = setOf(tokenType.toString())
        override val canStartWithSet: TokenSet get() = TokenSet.create(tokenType)
        public override fun calcCanBeEmpty() = false
    }

    fun token(token: String): Parsec = object : Parsec() {
        override fun parse(context: ParserContext): ParserInfo =
            if (context.text() == token) {
                context.advance()
                ParserInfo(context.position, setOf(this), null, true)
            } else {
                ParserInfo(context.position, setOf(this), null, false)
            }

        public override fun calcName() = "\"" + token + "\""
        override fun calcExpectedName() = setOf("\"" + token + "\"")
        override val canStartWithSet: TokenSet get() = TokenSet.ANY
        public override fun calcCanBeEmpty() = false
    }

    fun seq(p1: Parsec, p2: Parsec): Parsec = object : Parsec() {
        override fun parse(context: ParserContext): ParserInfo {
            val info = p1.parse(context)
            return if (info.success) {
                info.merge(p2.parse(context))
            } else {
                info
            }
        }

        public override fun calcName() = "${p1.name} ${p2.name}"
        override fun calcExpectedName() =
            if (p1.canBeEmpty) {
                p1.expectedName + p2.expectedName
            } else {
                p1.expectedName
            }

        override val canStartWithSet: TokenSet
            by lazy {
                if (p1.canBeEmpty) {
                    TokenSet.orSet(p1.canStartWithSet, p2.canStartWithSet)
                } else {
                    p1.canStartWithSet
                }
            }

        public override fun calcCanBeEmpty() = p1.canBeEmpty && p2.canBeEmpty
    }

    fun choice(head: Parsec, vararg tail: Parsec) = object : Parsec() {
        override fun parse(context: ParserContext): ParserInfo {
            val start = context.position
            var info: ParserInfo
            info = head.tryToParse(context)
            if (start < context.position || info.success) {
                return info
            }
            for (p in tail) {
                info = info.merge(p.tryToParse(context))
                if (start < context.position || info.success) {
                    return info
                }
            }
            return info
        }

        public override fun calcName(): String = buildString {
            append("(${head.name})")
            for (parsec in tail) append(" | (${parsec.name})")
        }

        override fun calcExpectedName(): Set<String> =
            tail.fold(head.expectedName) { acc, parsec -> acc + parsec.expectedName }

        override val canStartWithSet: TokenSet
            by lazy {
                TokenSet.orSet(
                    head.canStartWithSet,
                    *tail.map { it.canStartWithSet }.toTypedArray()
                )
            }


        public override fun calcCanBeEmpty(): Boolean {
            if (!head.canBeEmpty) {
                return false
            }
            for (parsec in tail) {
                if (!parsec.canBeEmpty) {
                    return false
                }
            }
            return true
        }
    }

    fun many(p: Parsec) = manyOrEmpty(p)
    fun manyOrEmpty(p: Parsec): Parsec = object : Parsec() {
        override fun parse(context: ParserContext): ParserInfo {
            var info = ParserInfo(context.position, setOf(p), null, true)
            while (!context.eof()) {
                val position = context.position
                info = p.parse(context)
                if (info.success) {
                    if (position == context.position) {
                        // TODO: this should not be allowed.
                        val info2 = ParserInfo(
                            context.position,
                            info.expected,
                            null,
                            false
                        )
                        return info.merge(info2)
                    }
                } else {
                    return if (position == context.position) {
                        val info2 = ParserInfo(
                            context.position,
                            info.expected,
                            null,
                            true
                        )
                        info.merge(info2)
                    } else {
                        info
                    }
                }
            }
            return info
        }

        public override fun calcName() = "(" + p.name + ")*"
        override fun calcExpectedName() = p.expectedName
        override val canStartWithSet: TokenSet get() = p.canStartWithSet
        public override fun calcCanBeEmpty() = true
    }

    fun many1(p: Parsec) = p + manyOrEmpty(p)
    fun optional(p: Parsec) = object : Parsec() {
        override fun parse(context: ParserContext) = try {
            context.enterOptional()
            val position = context.position
            val info1 = p.parse(context)
            if (info1.success) {
                info1
            } else {
                val success = context.position == position
                ParserInfo(info1.position, info1.expected, null, success)
            }
        } finally {
            context.exitOptional()
        }

        public override fun calcName() = "(" + p.name + ")?"
        override fun calcExpectedName() = p.expectedName
        override val canStartWithSet: TokenSet get() = p.canStartWithSet
        public override fun calcCanBeEmpty() = true
    }

    fun attempt(p: Parsec): Parsec = object : Parsec() {
        override fun parse(context: ParserContext) =
            if (!p.canParse(context)) {
                ParserInfo(context.position, setOf(p), null, false)
            } else {
                val start = context.position
                val pack = context.start()
                val inAttempt = context.isInAttempt
                context.isInAttempt = true
                val info = p.parse(context)
                context.isInAttempt = inAttempt
                if (info.success) {
                    pack.drop()
                    info
                } else {
                    pack.rollbackTo()
                    ParserInfo(start, info.expected, null, false)
                }
            }

        public override fun calcName() = "try(" + p.name + ")"
        override fun calcExpectedName() = p.expectedName
        override val canStartWithSet: TokenSet get() = p.canStartWithSet
        public override fun calcCanBeEmpty(): Boolean = p.canBeEmpty
    }

    fun parens(p: Parsec) = token(LPAREN) + p + token(RPAREN)
    fun squares(p: Parsec) = token(LBRACK) + p + token(RBRACK)
    fun braces(p: Parsec) = token(LCURLY) + p + token(RCURLY)
    fun sepBy1(p: Parsec, sep: IElementType) =
        p + attempt(manyOrEmpty(token(sep) + p))

    fun commaSep1(p: Parsec) = sepBy1(p, COMMA)
    fun sepBy(p: Parsec, sep: Parsec) = optional(p + manyOrEmpty(sep + p))
    fun sepBy1(p: Parsec, sep: Parsec) = p + manyOrEmpty(sep + p)
    fun commaSep(p: Parsec) = sepBy(p, token(COMMA))
    fun ref(init: Parsec.() -> Parsec) = ParsecRef(init)
    fun guard(
        p: Parsec,
        errorMessage: String,
        predicate: (String?) -> Boolean
    ) = object : Parsec() {
        override fun parse(context: ParserContext): ParserInfo {
            val pack = context.start()
            val start = context.position
            val info1 = p.parse(context)
            return if (info1.success) {
                val end = context.position
                val text = context.getText(start, end)
                if (!predicate.invoke(text)) {
                    ParserInfo(context.position, setOf(), errorMessage, false)
                } else {
                    pack.drop()
                    info1
                }
            } else {
                pack.rollbackTo()
                info1
            }
        }

        public override fun calcName() = p.name
        override fun calcExpectedName() = p.expectedName
        override val canStartWithSet: TokenSet get() = p.canStartWithSet
        public override fun calcCanBeEmpty() = p.canBeEmpty
    }
}