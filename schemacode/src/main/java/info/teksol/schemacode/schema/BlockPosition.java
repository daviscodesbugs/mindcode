package info.teksol.schemacode.schema;

import info.teksol.schemacode.config.Configuration;
import info.teksol.schemacode.mimex.BlockType;
import info.teksol.schemacode.mindustry.ConfigurationType;
import info.teksol.schemacode.mindustry.Implementation;
import info.teksol.schemacode.mindustry.Position;

public interface BlockPosition {
    int index();
    BlockType blockType();
    Position position();
    
    default String name() {
        return blockType().name();
    }

    default int size() {
        return blockType().size();
    }

    default Implementation implementation() {
        return blockType().implementation();
    }

    default Class<? extends Configuration> configurationClass() {
        return blockType().implementation().configurationClass();
    }

    default ConfigurationType configurationType() {
        return blockType().configurationType();
    }

    default int x() {
        return position().x();
    }

    default int y() {
        return position().y();
    }

    default int xMax() {
        return position().x() + size()  - 1;
    }

    default int yMax() {
        return position().y() + size() - 1;
    }

    default String area() {
        return size() == 1 ? "(%d, %d)".formatted(x(), y()) : "(%d, %d) - (%d, %d)".formatted(x(), y(), xMax(), yMax());
    }
}
