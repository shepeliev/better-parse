package com.github.h0tk3y.betterParse.lexer

expect fun Regex.patternWithEmbeddedFlags(): String

private fun Regex.countGroups() = "(?:$pattern)?".toRegex().find("")!!.groups.size - 1

/** Tokenizes input character sequences using the [tokens], prioritized by their order in the list,
 * first matched first. */
class DefaultTokenizer(override val tokens: List<Token>) : Tokenizer {
    init {
        require(tokens.isNotEmpty()) { "The tokens list should not be empty" }
    }

    /** Tokenizes the [input] from a [String] into a [TokenizerMatchesSequence]. */
    override fun tokenize(input: String): Sequence<TokenMatch> = tokenize(input as CharSequence)

    /** Tokenizes the [input] from a [Scanner] into a [TokenizerMatchesSequence]. */
    fun tokenize(input: CharSequence): Sequence<TokenMatch> =
        TokenizerMatchesSequence(TokensIterator(tokens, input), this)
}

class TokensIterator(val tokens: List<Token>, private val input: CharSequence) : AbstractIterator<TokenMatch>() {
    private var pos = 0
    private var row = 1
    private var col = 1

    private val relativeInput = object : CharSequence {
        override val length: Int get() = input.length - pos
        override fun get(index: Int): Char = input[index + pos]
        override fun subSequence(startIndex: Int, endIndex: Int) = input.subSequence(startIndex + pos, endIndex + pos)

        override fun toString(): String = "" // Avoids performance penalty in Matcher calling toString
    }

    private var errorState = false

    override fun computeNext() {
        if (relativeInput.isEmpty() || errorState) {
            done()
            return
        }

        for (index in 0 until tokens.size) {
            val token = tokens[index]
            val matchLength = token.match(relativeInput)
            if (matchLength == 0) 
                continue
            
            val result = TokenMatch(token, input, pos, matchLength, row, col)

            updateRowAndColumn(matchLength)

            pos += matchLength

            setNext(result)
            return
        }

        setNext(TokenMatch(noneMatched, input, pos, input.length - pos, row, col))
        errorState = true
    }

    private fun updateRowAndColumn(matchLength: Int) {
        for (i in pos until pos + matchLength) {
            if (input[i] == '\n') {
                row++
                col = 1
            } else {
                col++
            }
        }
    }
}