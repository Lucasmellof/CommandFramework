package dev.triumphteam.core.command.argument;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Argument<S> {

    @NotNull
    Class<?> getType();

    @Nullable
    Object resolve(@NotNull S sender, @NotNull final Object value);

}