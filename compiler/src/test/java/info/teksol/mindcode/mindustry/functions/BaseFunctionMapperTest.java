package info.teksol.mindcode.mindustry.functions;

import info.teksol.mindcode.mindustry.AbstractGeneratorTest;
import info.teksol.mindcode.mindustry.instructions.InstructionProcessor;
import info.teksol.mindcode.mindustry.instructions.InstructionProcessorFactory;
import info.teksol.mindcode.mindustry.logic.Opcode;
import info.teksol.mindcode.mindustry.logic.OpcodeVariant;
import info.teksol.mindcode.mindustry.logic.ProcessorEdition;
import info.teksol.mindcode.mindustry.logic.ProcessorVersion;
import java.util.List;
import org.junit.jupiter.api.Test;

import static info.teksol.mindcode.mindustry.logic.MindustryOpcodeVariants.*;
import static info.teksol.mindcode.mindustry.logic.OpcodeVariant.FunctionMapping.FUNC;
import static info.teksol.mindcode.mindustry.logic.ProcessorEdition.*;
import static info.teksol.mindcode.mindustry.logic.ProcessorVersion.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

// This class tests the initialization logic of the BaseFunctionMapper
// Actual mapping of functions to correct instructions is covered by other tests
public class BaseFunctionMapperTest extends AbstractGeneratorTest {

    private static BaseFunctionMapper createFunctionMapper(List<OpcodeVariant> opcodeVariants) {
        InstructionProcessor instructionProcessor = InstructionProcessorFactory.getInstructionProcessor(ProcessorVersion.V6,
                ProcessorEdition.STANDARD_PROCESSOR, opcodeVariants);

        return new BaseFunctionMapper(instructionProcessor, s -> {});
    }

    @Test
    void detectMissingResultArguments() {
        assertThrows(InvalidMetadataException.class, 
                () -> createFunctionMapper(
                        List.of(
                                new OpcodeVariant(V6, V7, S, FUNC, Opcode.GETLINK, out("block"), in("link#"))
                        )
                )
        );
    }

    @Test
    void detectTooManyResultArguments() {
        assertThrows(InvalidMetadataException.class,
                () -> createFunctionMapper(
                        List.of(
                                new OpcodeVariant(V6, V6, S, FUNC, Opcode.UCONTROL,   uctrl("getBlock"),  in("x"), in("y"), res("type"), res("building"))
                        )
                )
        );
    }

    @Test
    void detectFunctionNameCollision1() {
        assertThrows(InvalidMetadataException.class,
                () -> createFunctionMapper(
                        List.of(
                                new OpcodeVariant(V6, V7, S, FUNC, Opcode.PRINTFLUSH, block("to")),
                                new OpcodeVariant(V6, V7, S, FUNC, Opcode.UCONTROL, uctrl("printflush"))
                        )
                )
        );
    }

    @Test
    void detectFunctionNameCollision2() {
        assertThrows(InvalidMetadataException.class,
                () -> createFunctionMapper(
                        List.of(
                                new OpcodeVariant(V6, V7, S, FUNC, Opcode.PRINTFLUSH, unused("0")),
                                new OpcodeVariant(V6, V7, S, FUNC, Opcode.UCONTROL, uctrl("printflush"))
                        )
                )
        );
    }
}