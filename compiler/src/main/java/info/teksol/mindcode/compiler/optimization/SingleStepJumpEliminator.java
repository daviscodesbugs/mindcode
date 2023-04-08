package info.teksol.mindcode.compiler.optimization;

import info.teksol.mindcode.compiler.instructions.LogicInstruction;
import info.teksol.mindcode.compiler.LogicInstructionPipeline;
import info.teksol.mindcode.compiler.instructions.InstructionProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Remove jumps (both conditional and unconditional) that target the next instruction.
 * Technically, if we have a sequence
 * <pre>{@code
 * 0: jump 2 ...
 * 1: jump 2 ...
 * 2: ...
 * }</pre>
 * we could eliminate both jumps. This class will only remove the second jump, because before that removal the first
 * one doesn't target the next instruction. However, such sequences aren't typically generated by the compiler.
 */
class SingleStepJumpEliminator extends PipelinedOptimizer {
    SingleStepJumpEliminator(InstructionProcessor instructionProcessor, LogicInstructionPipeline next) {
        super(instructionProcessor, next);
    }

    @Override
    protected State initialState() {
        return new EmptyState();
    }

    private final class EmptyState implements State {
        @Override
        public State emit(LogicInstruction instruction) {
            if (instruction.isJump()) {
                return new ExpectLabel(instruction);
            } else {
                emitToNext(instruction);
                return this;
            }
        }

        @Override
        public State flush() {
            return this;
        }
    }

    private final class ExpectLabel implements State {
        private final LogicInstruction jump;
        private final String targetLabel;
        private final List<LogicInstruction> labels = new ArrayList<>();
        private boolean isJumpToNext = false;

        ExpectLabel(LogicInstruction jump) {
            this.jump = jump;
            this.targetLabel = jump.getArgs().get(0);
        }

        @Override
        public State emit(LogicInstruction instruction) {
            if (instruction.isLabel()) {
                if (instruction.getArgs().get(0).equals(targetLabel)) {
                    isJumpToNext = true;
                }
                labels.add(instruction);
                return this;
            }

            if (!isJumpToNext) {
                emitToNext(jump);
            }
            labels.forEach(SingleStepJumpEliminator.this::emitToNext);
            return new EmptyState().emit(instruction);
        }

        @Override
        public State flush() {
            emitToNext(jump);
            labels.forEach(SingleStepJumpEliminator.this::emitToNext);
            return new EmptyState();
        }
    }
}