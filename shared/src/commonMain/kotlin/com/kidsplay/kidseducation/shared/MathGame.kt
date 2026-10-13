package com.kidsplay.kidseducation.shared

import kotlin.random.Random

/** The four "Maths World" quiz types of the original app (GameMenuActivity). */
enum class MathOp(val title: String) {
    Addition("Addition"),
    Subtraction("Subtraction"),
    Multiplication("Multiplication"),
    Division("Division"),
}

/** One question: [text] to show, four [answers] (one is correct) and where the correct one sits. */
class MathQuestion(
    val op: MathOp,
    val a: Int,
    val b: Int,
    val text: String,
    val answers: List<Int>,
    val correctIndex: Int,
)

/**
 * Ports GameActivity.nextQuestion() 1:1 — same operand ranges, same wrong-answer ranges, same
 * question text format. (As in the original, two wrong answers may be equal to each other.)
 */
fun generateMathQuestion(op: MathOp, random: Random = Random.Default): MathQuestion {
    var a = random.nextInt(10)
    var b = random.nextInt(10)
    val text: String
    val correct: Int
    val wrong: () -> Int

    when (op) {
        MathOp.Addition -> {
            text = "$a+$b"
            correct = a + b
            wrong = { random.nextInt(20) }
        }
        MathOp.Multiplication -> {
            text = "$a x $b"
            correct = a * b
            wrong = { random.nextInt(20) }
        }
        MathOp.Division -> {
            do {
                a = random.nextInt(101)
                b = random.nextInt(10) + 1
            } while (a % b != 0 || a / b < 0 || a / b > 100)
            text = "$a / $b"
            correct = a / b
            wrong = { random.nextInt(101) }
        }
        MathOp.Subtraction -> {
            text = "$a - $b"
            correct = a - b
            wrong = { random.nextInt(40) - 20 }
        }
    }

    val correctIndex = random.nextInt(4)
    val answers = List(4) { i ->
        if (i == correctIndex) correct else {
            var w = wrong()
            while (w == correct) w = wrong()
            w
        }
    }
    return MathQuestion(op, a, b, text, answers, correctIndex)
}
