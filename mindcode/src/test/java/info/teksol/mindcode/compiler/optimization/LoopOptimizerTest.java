package info.teksol.mindcode.compiler.optimization;

import info.teksol.mindcode.ast.Seq;
import info.teksol.mindcode.compiler.AbstractGeneratorTest;
import info.teksol.mindcode.compiler.CompilerProfile;
import info.teksol.mindcode.compiler.GenerationGoal;
import info.teksol.mindcode.compiler.LogicInstructionPipeline;
import org.junit.jupiter.api.Test;

import java.util.List;

import static info.teksol.mindcode.logic.Opcode.*;

class LoopOptimizerTest extends AbstractGeneratorTest {
    private final LogicInstructionPipeline sut = OptimizationPipeline.createPipelineOf(getInstructionProcessor(),
            terminus,
            getCompilerProfile(),
            Optimization.DEAD_CODE_ELIMINATION,
            Optimization.CONDITIONAL_JUMPS_IMPROVEMENT,
            Optimization.SINGLE_STEP_JUMP_ELIMINATION,
            Optimization.JUMP_OVER_JUMP_ELIMINATION,
            Optimization.INACCESSIBLE_CODE_ELIMINATION,
            Optimization.LOOP_OPTIMIZATION
    );

    protected CompilerProfile getCompilerProfile() {
        CompilerProfile profile = super.getCompilerProfile();
        profile.setGoal(GenerationGoal.SPEED);
        return profile;
    }


    @Test
    void optimizesRangedForLoops() {
        generateInto(sut,
                (Seq) translateToAst("""
                        for i in 1 .. 10
                            print(i)
                        end
                        """
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(SET, "i", "1"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(JUMP, var(1002), "greaterThan", "i", "10"),
                        createInstruction(LABEL, var(1003)),
                        createInstruction(PRINT, "i"),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(OP, "add", "i", "i", "1"),
                        createInstruction(JUMP, var(1003), "lessThanEq", "i", "10"),
                        createInstruction(LABEL, var(1002)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void optimizesWhileLoop() {
        generateInto(sut,
                (Seq) translateToAst("""
                        i = 10
                        while i > 0
                            print(i)
                            i -= 1
                        end
                        """
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(SET, "i", "10"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(JUMP, var(1002), "lessThanEq", "i", "0"),
                        createInstruction(LABEL, var(1003)),
                        createInstruction(PRINT, "i"),
                        createInstruction(OP, "sub", var(1), "i", "1"),
                        createInstruction(SET, "i", var(1)),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(JUMP, var(1003), "greaterThan", "i", "0"),
                        createInstruction(LABEL, var(1002)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void optimizesWhileLoopStrictEqual() {
        generateInto(sut,
                (Seq) translateToAst("""
                        while state === 0
                            print(i)
                            state = @unit.dead
                        end
                        """
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(LABEL, var(1000)),
                        createInstruction(OP, "strictEqual", var(0), "state", "0"),
                        createInstruction(JUMP, var(1002), "equal", var(0), "false"),
                        createInstruction(LABEL, var(1003)),
                        createInstruction(PRINT, "i"),
                        createInstruction(SENSOR, var(1), "@unit", "@dead"),
                        createInstruction(SET, "state", var(1)),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(JUMP, var(1003), "strictEqual", "state", "0"),
                        createInstruction(LABEL, var(1002)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void optimizesWhileLoopWithInitialization() {
        generateInto(sut,
                (Seq) translateToAst("""
                        count = 0
                        while switch1.enabled
                            print(count += 1)
                        end
                        """
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(SET, "count", "0"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(SENSOR, var(0), "switch1", "@enabled"),
                        createInstruction(JUMP, var(1002), "equal", var(0), "false"),
                        createInstruction(LABEL, var(1003)),
                        createInstruction(OP, "add", var(1), "count", "1"),
                        createInstruction(SET, "count", var(1)),
                        createInstruction(PRINT, var(1)),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(SENSOR, var(0), "switch1", "@enabled"),
                        createInstruction(JUMP, var(1003), "notEqual", var(0), "false"),
                        createInstruction(LABEL, var(1002)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void optimizesWhileLoopWithInitializationAndStrictEqual() {
        generateInto(sut,
                (Seq) translateToAst("""
                        while @unit.dead === 0
                            print("Got unit!")
                        end
                        """
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(LABEL, var(1000)),
                        createInstruction(SENSOR, var(0), "@unit", "@dead"),
                        createInstruction(OP, "strictEqual", var(1), var(0), "0"),
                        createInstruction(JUMP, var(1002), "equal", var(1), "false"),
                        createInstruction(LABEL, var(1003)),
                        createInstruction(PRINT, q("Got unit!")),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(SENSOR, var(0), "@unit", "@dead"),
                        createInstruction(JUMP, var(1003), "strictEqual", var(0), "0"),
                        createInstruction(LABEL, var(1002)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void optimizesRangedForLoopsWithBreak() {
        generateInto(sut,
                (Seq) translateToAst("""
                        for i in 1 .. 10
                            print(i)
                            if i > 5
                                break
                            end
                        end
                        """
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(SET, "i", "1"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(JUMP, var(1002), "greaterThan", "i", "10"),
                        createInstruction(LABEL, var(1005)),
                        createInstruction(PRINT, "i"),
                        createInstruction(JUMP, var(1002), "greaterThan", "i", "5"),
                        createInstruction(LABEL, var(1003)),
                        createInstruction(LABEL, var(1004)),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(OP, "add", "i", "i", "1"),
                        createInstruction(JUMP, var(1005), "lessThanEq", "i", "10"),
                        createInstruction(LABEL, var(1002)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }

    @Test
    void optimizesWhileLoopWithContinue() {
        generateInto(sut,
                (Seq) translateToAst("""
                        i = 10
                        while i > 0
                            i -= 1
                            if i == 4
                                continue
                            end
                            print(i)
                        end
                        """
                )
        );

        assertLogicInstructionsMatch(
                List.of(
                        createInstruction(SET, "i", "10"),
                        createInstruction(LABEL, var(1000)),
                        createInstruction(JUMP, var(1002), "lessThanEq", "i", "0"),
                        createInstruction(LABEL, var(1005)),
                        createInstruction(OP, "sub", var(1), "i", "1"),
                        createInstruction(SET, "i", var(1)),
                        createInstruction(JUMP, var(1001), "equal", "i", "4"),
                        createInstruction(LABEL, var(1003)),
                        createInstruction(LABEL, var(1004)),
                        createInstruction(PRINT, "i"),
                        createInstruction(LABEL, var(1001)),
                        createInstruction(JUMP, var(1005), "greaterThan", "i", "0"),
                        createInstruction(LABEL, var(1002)),
                        createInstruction(END)
                ),
                terminus.getResult()
        );
    }
}