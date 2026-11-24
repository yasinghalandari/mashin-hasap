package com.example.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class AngleUnit(val label: String) {
    DEG("DEG"),
    RAD("RAD")
}

sealed interface EvalResult {
    data object Empty : EvalResult
    data class Success(val value: Double) : EvalResult
    data class Error(val message: String) : EvalResult
}

object MathEvaluator {

    fun evaluate(rawExpression: String, angleUnit: AngleUnit = AngleUnit.DEG): EvalResult {
        val normalized = PersianFormatter.normalizeToLatin(rawExpression)
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")

        if (normalized.isEmpty()) return EvalResult.Empty

        val completed = autoCloseParentheses(normalized)
        return try {
            val tokens = tokenize(completed)
            if (tokens.isEmpty()) return EvalResult.Empty
            val parser = Parser(tokens, angleUnit)
            val result = parser.parseExpression()
            if (parser.hasRemaining()) {
                EvalResult.Error("عبارت نامعتبر")
            } else if (result.isNaN()) {
                EvalResult.Error("تعریف‌نشده")
            } else if (result.isInfinite()) {
                EvalResult.Error("تقسیم بر صفر یا بی‌نهایت")
            } else {
                EvalResult.Success(result)
            }
        } catch (e: IllegalArgumentException) {
            EvalResult.Error(e.message ?: "عبارت نامعتبر")
        } catch (_: Exception) {
            EvalResult.Error("خطا در محاسبه")
        }
    }

    private fun autoCloseParentheses(expr: String): String {
        var balance = 0
        for (ch in expr) {
            if (ch == '(') balance++
            else if (ch == ')' && balance > 0) balance--
        }
        return if (balance > 0) expr + ")".repeat(balance) else expr
    }

    private sealed interface Token {
        data class Number(val value: Double) : Token
        data class Func(val name: String) : Token
        data class Op(val symbol: Char) : Token
        data object LParen : Token
        data object RParen : Token
        data object Percent : Token
        data object Factorial : Token
    }

    private fun tokenize(expr: String): List<Token> {
        val rawTokens = mutableListOf<Token>()
        var i = 0
        while (i < expr.length) {
            val ch = expr[i]
            when {
                ch.isDigit() || ch == '.' -> {
                    val start = i
                    var dotCount = 0
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        if (expr[i] == '.') dotCount++
                        if (dotCount > 1) throw IllegalArgumentException("نقطه اعشار تکراری")
                        i++
                    }
                    // Handle scientific notation like 1E-5 or 1e5
                    if (i < expr.length && (expr[i] == 'E' || expr[i] == 'e')) {
                        val nextIdx = i + 1
                        if (nextIdx < expr.length && (expr[nextIdx].isDigit() || expr[nextIdx] == '+' || expr[nextIdx] == '-')) {
                            i++
                            if (expr[i] == '+' || expr[i] == '-') i++
                            while (i < expr.length && expr[i].isDigit()) i++
                        }
                    }
                    val numStr = expr.substring(start, i)
                    val num = numStr.toDoubleOrNull()
                        ?: throw IllegalArgumentException("عدد نامعتبر")
                    rawTokens.add(Token.Number(num))
                }
                ch == 'π' -> {
                    rawTokens.add(Token.Number(PI))
                    i++
                }
                ch == '√' -> {
                    rawTokens.add(Token.Func("sqrt"))
                    i++
                }
                ch.isLetter() -> {
                    val start = i
                    while (i < expr.length && expr[i].isLetter()) i++
                    val word = expr.substring(start, i).lowercase()
                    when (word) {
                        "pi" -> rawTokens.add(Token.Number(PI))
                        "e" -> rawTokens.add(Token.Number(kotlin.math.E))
                        "sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "sqrt", "abs" ->
                            rawTokens.add(Token.Func(word))
                        else -> throw IllegalArgumentException("تابع ناشناخته: $word")
                    }
                }
                ch == '(' -> {
                    rawTokens.add(Token.LParen)
                    i++
                }
                ch == ')' -> {
                    rawTokens.add(Token.RParen)
                    i++
                }
                ch == '%' -> {
                    rawTokens.add(Token.Percent)
                    i++
                }
                ch == '!' -> {
                    rawTokens.add(Token.Factorial)
                    i++
                }
                ch in charArrayOf('+', '-', '*', '/', '^') -> {
                    rawTokens.add(Token.Op(ch))
                    i++
                }
                else -> throw IllegalArgumentException("کاراکتر نامعتبر")
            }
        }

        // Insert implicit multiplication where appropriate (e.g. 2π, 2(3), (2)(3), 2sin(30))
        val tokens = mutableListOf<Token>()
        for (idx in rawTokens.indices) {
            val curr = rawTokens[idx]
            if (idx > 0) {
                val prev = rawTokens[idx - 1]
                val prevEndsValue = prev is Token.Number || prev is Token.RParen ||
                    prev is Token.Percent || prev is Token.Factorial
                val currStartsValue = curr is Token.Number || curr is Token.LParen || curr is Token.Func
                if (prevEndsValue && currStartsValue) {
                    tokens.add(Token.Op('*'))
                }
            }
            tokens.add(curr)
        }
        return tokens
    }

    private class Parser(
        private val tokens: List<Token>,
        private val angleUnit: AngleUnit
    ) {
        private var pos = 0

        fun hasRemaining(): Boolean = pos < tokens.size

        private fun peek(): Token? = tokens.getOrNull(pos)

        private fun consume(): Token = tokens[pos++]

        fun parseExpression(): Double {
            var left = parseTerm()
            while (true) {
                val tok = peek()
                if (tok is Token.Op && (tok.symbol == '+' || tok.symbol == '-')) {
                    consume()
                    val right = parseTerm()
                    left = if (tok.symbol == '+') left + right else left - right
                } else {
                    break
                }
            }
            return left
        }

        private fun parseTerm(): Double {
            var left = parseUnary()
            while (true) {
                val tok = peek()
                if (tok is Token.Op && (tok.symbol == '*' || tok.symbol == '/')) {
                    consume()
                    val right = parseUnary()
                    left = if (tok.symbol == '*') {
                        left * right
                    } else {
                        if (right == 0.0) throw IllegalArgumentException("تقسیم بر صفر")
                        left / right
                    }
                } else {
                    break
                }
            }
            return left
        }

        private fun parseUnary(): Double {
            val tok = peek()
            if (tok is Token.Op && (tok.symbol == '+' || tok.symbol == '-')) {
                consume()
                val value = parseUnary()
                return if (tok.symbol == '-') -value else value
            }
            return parsePower()
        }

        private fun parsePower(): Double {
            val base = parsePostfix()
            val tok = peek()
            if (tok is Token.Op && tok.symbol == '^') {
                consume()
                val exponent = parseUnary()
                return base.pow(exponent)
            }
            return base
        }

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            while (true) {
                when (peek()) {
                    is Token.Percent -> {
                        consume()
                        value /= 100.0
                    }
                    is Token.Factorial -> {
                        consume()
                        value = factorial(value)
                    }
                    else -> break
                }
            }
            return value
        }

        private fun parsePrimary(): Double {
            return when (val tok = peek()) {
                is Token.Number -> {
                    consume()
                    tok.value
                }
                is Token.LParen -> {
                    consume()
                    val inside = parseExpression()
                    if (peek() is Token.RParen) {
                        consume()
                    }
                    inside
                }
                is Token.Func -> {
                    consume()
                    val arg = if (peek() is Token.LParen) {
                        consume()
                        val inner = parseExpression()
                        if (peek() is Token.RParen) consume()
                        inner
                    } else {
                        parseUnary()
                    }
                    applyFunction(tok.name, arg)
                }
                else -> throw IllegalArgumentException("عبارت ناقص")
            }
        }

        private fun applyFunction(name: String, arg: Double): Double {
            val rad = if (angleUnit == AngleUnit.DEG) Math.toRadians(arg) else arg
            return when (name) {
                "sin" -> {
                    val res = sin(rad)
                    if (abs(res) < 1e-12) 0.0 else res
                }
                "cos" -> {
                    val res = cos(rad)
                    if (abs(res) < 1e-12) 0.0 else res
                }
                "tan" -> {
                    val c = cos(rad)
                    if (abs(c) < 1e-12) throw IllegalArgumentException("تانژانت تعریف‌نشده")
                    tan(rad)
                }
                "asin" -> {
                    if (arg < -1.0 || arg > 1.0) throw IllegalArgumentException("ورودی خارج از بازه [-1, 1]")
                    val out = asin(arg)
                    if (angleUnit == AngleUnit.DEG) Math.toDegrees(out) else out
                }
                "acos" -> {
                    if (arg < -1.0 || arg > 1.0) throw IllegalArgumentException("ورودی خارج از بازه [-1, 1]")
                    val out = acos(arg)
                    if (angleUnit == AngleUnit.DEG) Math.toDegrees(out) else out
                }
                "atan" -> {
                    val out = atan(arg)
                    if (angleUnit == AngleUnit.DEG) Math.toDegrees(out) else out
                }
                "ln" -> {
                    if (arg <= 0.0) throw IllegalArgumentException("لگاریتم عدد غیرمثبت")
                    ln(arg)
                }
                "log" -> {
                    if (arg <= 0.0) throw IllegalArgumentException("لگاریتم عدد غیرمثبت")
                    log10(arg)
                }
                "sqrt" -> {
                    if (arg < 0.0) throw IllegalArgumentException("جذر عدد منفی")
                    sqrt(arg)
                }
                "abs" -> abs(arg)
                else -> throw IllegalArgumentException("تابع ناشناخته")
            }
        }

        private fun factorial(n: Double): Double {
            if (n < 0 || n != n.toLong().toDouble()) {
                throw IllegalArgumentException("فاکتوریل فقط برای اعداد صحیح نامنفی است")
            }
            val intVal = n.toInt()
            if (intVal > 170) throw IllegalArgumentException("عدد بیش از حد بزرگ")
            var acc = 1.0
            for (k in 2..intVal) acc *= k
            return acc
        }
    }
}
