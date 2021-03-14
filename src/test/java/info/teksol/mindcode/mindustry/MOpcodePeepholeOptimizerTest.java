package info.teksol.mindcode.mindustry;

import info.teksol.mindcode.AbstractAstTest;
import info.teksol.mindcode.ast.Seq;
import info.teksol.mindcode.mindustry.MOpcode;
import info.teksol.mindcode.mindustry.MOpcodeGenerator;
import info.teksol.mindcode.mindustry.MOpcodePeepholeOptimizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MOpcodePeepholeOptimizerTest extends AbstractAstTest {
    @Test
    void collapsesSetThenPrint() {
        assertEquals(
                List.of(
                        new MOpcode("print", "\"n: \""),
                        new MOpcode("print", "n"),
                        new MOpcode("end")
                ),
                MOpcodePeepholeOptimizer.optimize(
                        MOpcodeGenerator.generateFrom(
                                (Seq) translateToAst(
                                        "print(\"n: \", n)"
                                )
                        )
                )
        );
    }

    @Test
    void collapsesJumps() {
        assertEquals(
                List.of(
                        new MOpcode("label", "label0"),
                        new MOpcode("jump", "label1", "lessThanEq", "n", "0"),
                        new MOpcode("op", "add", "n", "n", "1"),
                        new MOpcode("jump", "label0", "always"),
                        new MOpcode("label", "label1"),

                        new MOpcode("label", "label2"),
                        new MOpcode("jump", "label3", "notEqual", "n", "null"),
                        new MOpcode("op", "add", "n", "n", "1"),
                        new MOpcode("jump", "label2", "always"),
                        new MOpcode("label", "label3"),
                        new MOpcode("end")
                ),
                MOpcodePeepholeOptimizer.optimize(
                        MOpcodeGenerator.generateFrom(
                                (Seq) translateToAst(
                                        "while n > 0 {\nn += 1\n}\nwhile n === null {\nn += 1\n}\n"
                                )
                        )
                )
        );
    }

    @Test
    void collapsesPlusAssignments() {
        assertEquals(
                List.of(
                        new MOpcode("op", "add", "n", "n", "1"),
                        new MOpcode("end")
                ),
                MOpcodePeepholeOptimizer.optimize(
                        MOpcodeGenerator.generateFrom(
                                (Seq) translateToAst(
                                        "n += 1"
                                )
                        )
                )
        );
    }


    @Test
    void collapsesAdjacentAssignments() {
        assertEquals(
                List.of(
                        new MOpcode("op", List.of("add", "result", "1", "2")),
                        new MOpcode("end")
                ),
                MOpcodePeepholeOptimizer.optimize(
                        MOpcodeGenerator.generateFrom(
                                (Seq) translateToAst(
                                        "result = 1 + 2"
                                )
                        )
                )
        );
    }

    @Test
    void collapsesSimpleAssignments() {
        assertEquals(
                List.of(
                        new MOpcode("set", List.of("n", "0")),
                        new MOpcode("end")
                ),
                MOpcodePeepholeOptimizer.optimize(
                        MOpcodeGenerator.generateFrom(
                                (Seq) translateToAst(
                                        "n = 0\n"
                                )
                        )
                )
        );
    }

    @Test
    void collapsesSetWrite() {
        assertEquals(
                List.of(
                        new MOpcode("write", "4", "cell1", "2"),
                        new MOpcode("end")
                ),
                MOpcodePeepholeOptimizer.optimize(
                        MOpcodeGenerator.generateFrom(
                                (Seq) translateToAst(
                                        "cell1[2] = 4"
                                )
                        )
                )
        );
    }


    @Test
    void collapsesConditionals() {
        assertEquals(
                List.of(
                        new MOpcode("read", "tmp0", "HEAP", "4"),
                        new MOpcode("jump", "label0", "notEqual", "tmp0", "0"),
                        new MOpcode("set", "tmp5", "false"),
                        new MOpcode("jump", "label1", "always"),
                        new MOpcode("label", "label0"),
                        new MOpcode("write", "true", "HEAP", "4"),
                        new MOpcode("op", "add", "n", "n", "1"),
                        new MOpcode("set", "tmp5", "n"),
                        new MOpcode("label", "label1"),
                        new MOpcode("set", "value", "tmp5"),
                        new MOpcode("end")
                ),
                MOpcodePeepholeOptimizer.optimize(
                        MOpcodeGenerator.generateFrom(
                                (Seq) translateToAst(
                                        "value = if HEAP[4] == 0 { false\n} else { HEAP[4] = true\nn += 1\n}"
                                )
                        )
                )
        );
    }
}