package info.teksol.mindcode.compiler.optimization;

import info.teksol.mindcode.ast.Seq;
import info.teksol.mindcode.compiler.AbstractGeneratorTest;
import info.teksol.mindcode.compiler.LogicInstructionPipeline;
import org.junit.jupiter.api.Test;

import java.util.List;

import static info.teksol.mindcode.logic.Opcode.*;

class DeadCodeEliminatorTest extends AbstractGeneratorTest {
    private final LogicInstructionPipeline sut = OptimizationPipeline.createPipelineOf(getInstructionProcessor(),
            terminus,
            Optimization.DEAD_CODE_ELIMINATION);

    @Test
    void removesDeadSetsInIfExpression() {
        generateInto(sut,
                (Seq) translateToAst("if x == 3\n  1\nelse\n  end()\nend")
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(OP, "equal", var(0), "x", "3"),
                        createInstruction(JUMP, var(1000), "equal", var(0), "false"),
                        createInstruction(JUMP, var(1001), "always"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(END),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void keepsUsefulIfAssignments() {
        generateInto(sut,
                (Seq) translateToAst("n = if x == 3\n  1\nelse\n  41\nend\nmove(73, n)\n")
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(OP, "equal", var(0), "x", "3"),
                        createInstruction(JUMP, var(1000), "equal", var(0), "false"),
                        createInstruction(SET, var(1), "1"),
                        createInstruction(JUMP, var(1001), "always"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(SET, var(1), "41"),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(SET, "n", var(1)),
                        createInstruction(UCONTROL, "move", "73", "n"),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void preventsEliminationOfUradarUsages() {
        generateInto(sut,
                (Seq) translateToAst("" +
                        "target = uradar(enemy, ground, any, health, MIN_TO_MAX)\n" +
                        "if target != null\n" +
                        "  approach(target.x, target.y, 10)\n" +
                        "  if within(target.x, target.y, 10)\n" +
                        "    target(target.x, target.y, SHOOT)\n" +
                        "  end\n" +
                        "end\n"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(URADAR, "enemy", "ground", "any", "health", "0", "MIN_TO_MAX", var(0)),
                        createInstruction(SET, "target", var(0)),
                        createInstruction(OP, "notEqual", var(1), "target", "null"),
                        createInstruction(JUMP, var(1000), "equal", var(1), "false"),
                        createInstruction(SENSOR, var(3), "target", "@x"),
                        createInstruction(SENSOR, var(4), "target", "@y"),
                        createInstruction(UCONTROL, "approach", var(3), var(4), "10"),
                        createInstruction(SENSOR, var(5), "target", "@x"),
                        createInstruction(SENSOR, var(6), "target", "@y"),
                        createInstruction(UCONTROL, "within", var(5), var(6), "10", var(7)),
                        createInstruction(JUMP, var(1002), "equal", var(7), "false"),
                        createInstruction(SENSOR, var(9), "target", "@x"),
                        createInstruction(SENSOR, var(10), "target", "@y"),
                        createInstruction(UCONTROL, "target", var(9), var(10), "SHOOT"),
                        createInstruction(JUMP, var(1003), "always"),
                        createInstruction(LABEL, var(1002)),
                        createInstruction(LABEL, var(1003)),
                        createInstruction(JUMP, var(1001), "always"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void preventsEliminationOfUlocateUsages() {
        generateInto(sut,
                (Seq) translateToAst("" +
                        "ulocate(ore, @surge-alloy, outx, outy)\n" +
                        "approach(outx, outy, 4)\n" +
                        "ulocate(building, core, ENEMY, outx, outy, outbuilding)\n" +
                        "approach(outx, outy, 4)\n" +
                        "ulocate(spawn, outx, outy, outbuilding)\n" +
                        "approach(outx, outy, 4)\n" +
                        "ulocate(damaged, outx, outy, outbuilding)\n" +
                        "approach(outx, outy, 4)\n"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(ULOCATE, "ore", "core", "true", "@surge-alloy", "outx", "outy", var(0), var(1)),
                        createInstruction(UCONTROL, "approach", "outx", "outy", "4"),
                        createInstruction(ULOCATE, "building", "core", "ENEMY", "@copper", "outx", "outy", var(2), "outbuilding"),
                        createInstruction(UCONTROL, "approach", "outx", "outy", "4"),
                        createInstruction(ULOCATE, "spawn", "core", "true", "@copper", "outx", "outy", var(3), "outbuilding"),
                        createInstruction(UCONTROL, "approach", "outx", "outy", "4"),
                        createInstruction(ULOCATE, "damaged", "core", "true", "@copper", "outx", "outy", var(4), "outbuilding"),
                        createInstruction(UCONTROL, "approach", "outx", "outy", "4"),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void completelyRemovesDeadcode() {
        generateInto(sut,
                (Seq) translateToAst("" +
                        "n = 1\nn = 1\n"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void removesUnusedUlocate() {
        generateInto(sut,
                (Seq) translateToAst("" +
                        "ulocate(ore, @surge-alloy, outx, outy)\n" +
                        "ulocate(ore, @surge-alloy, x, y)\n" +
                        "approach(outx, outy, 4)"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(ULOCATE, "ore", "core", "true", "@surge-alloy", "outx", "outy", var(0), var(1)),
                        createInstruction(UCONTROL, "approach", "outx", "outy", "4"),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void preventsEliminationOfPartiallyUsedUlocate() {
        generateInto(sut,
                (Seq) translateToAst("" +
                        "found = ulocate(building, core, ENEMY, outx, outy, outbuilding)\n" +
                        "print(found)"
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(ULOCATE, "building", "core", "ENEMY", "@copper", "outx", "outy", var(0), "outbuilding"),
                        createInstruction(SET, "found", var(0)),
                        createInstruction(PRINT, "found"),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }
}