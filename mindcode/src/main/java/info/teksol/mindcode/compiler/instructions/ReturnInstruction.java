package info.teksol.mindcode.compiler.instructions;

import info.teksol.mindcode.logic.LogicArgument;
import info.teksol.mindcode.logic.LogicParameter;
import info.teksol.mindcode.logic.LogicVariable;
import info.teksol.mindcode.logic.Opcode;

import java.util.List;

public class ReturnInstruction extends BaseInstruction {

    ReturnInstruction(List<LogicArgument> args, List<LogicParameter> params) {
        super(Opcode.RETURN, args, params);
    }

    protected ReturnInstruction(BaseInstruction other, String marker) {
        super(other, marker);
    }

    public ReturnInstruction withMarker(String marker) {
        return new ReturnInstruction(this, marker);
    }

    public final LogicVariable getStack() {
        return (LogicVariable) getArg(0);
    }
}