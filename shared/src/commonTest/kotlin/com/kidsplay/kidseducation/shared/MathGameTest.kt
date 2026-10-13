package com.kidsplay.kidseducation.shared

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MathGameTest {

    private fun expected(op: MathOp, a: Int, b: Int) = when (op) {
        MathOp.Addition -> a + b
        MathOp.Subtraction -> a - b
        MathOp.Multiplication -> a * b
        MathOp.Division -> a / b
    }

    @Test
    fun everyQuestionHasFourAnswersAndTheCorrectOneIsWhereItSaysItIs() {
        val random = Random(42)
        MathOp.entries.forEach { op ->
            repeat(500) {
                val q = generateMathQuestion(op, random)
                assertEquals(4, q.answers.size)
                assertTrue(q.correctIndex in 0..3)
                val correct = q.answers[q.correctIndex]
                assertEquals(expected(op, q.a, q.b), correct)
                q.answers.forEachIndexed { i, v -> if (i != q.correctIndex) assertNotEquals(correct, v) }
            }
        }
    }

    @Test
    fun divisionIsAlwaysExactAndNeverByZero() {
        val random = Random(7)
        repeat(1000) {
            val q = generateMathQuestion(MathOp.Division, random)
            assertTrue(q.b in 1..10)
            assertEquals(0, q.a % q.b)
            assertTrue(q.a / q.b in 0..100)
        }
    }

    @Test
    fun questionTextUsesTheLegacyFormats() {
        val random = Random(1)
        val add = generateMathQuestion(MathOp.Addition, random)
        val sub = generateMathQuestion(MathOp.Subtraction, random)
        val mul = generateMathQuestion(MathOp.Multiplication, random)
        val div = generateMathQuestion(MathOp.Division, random)
        assertEquals("${add.a}+${add.b}", add.text)
        assertEquals("${sub.a} - ${sub.b}", sub.text)
        assertEquals("${mul.a} x ${mul.b}", mul.text)
        assertEquals("${div.a} / ${div.b}", div.text)
    }
}
