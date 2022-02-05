/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2021 Matt
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.triumphteam.cmd.core;

import dev.triumphteam.cmd.core.annotation.Default;
import dev.triumphteam.cmd.core.argument.Argument;
import dev.triumphteam.cmd.core.argument.LimitlessArgument;
import dev.triumphteam.cmd.core.argument.StringArgument;
import dev.triumphteam.cmd.core.exceptions.CommandExecutionException;
import dev.triumphteam.cmd.core.execution.ExecutionProvider;
import dev.triumphteam.cmd.core.message.MessageKey;
import dev.triumphteam.cmd.core.message.MessageRegistry;
import dev.triumphteam.cmd.core.message.context.DefaultMessageContext;
import dev.triumphteam.cmd.core.message.context.InvalidArgumentContext;
import dev.triumphteam.cmd.core.processor.AbstractSubCommandProcessor;
import dev.triumphteam.cmd.core.requirement.Requirement;
import dev.triumphteam.cmd.core.sender.SenderValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SubCommand implementation.
 * Might be better to rename this to something different.
 *
 * @param <S> The sender type.
 */
public abstract class AbstractSubCommand<S> implements SubCommand<S> {

    private final BaseCommand baseCommand;
    private final Method method;

    private final String parentName;
    private final String name;
    private final List<String> alias;
    private final boolean isDefault;
    private final boolean isNamedArguments;

    private final Class<? extends S> senderType;

    private final List<Argument<S, ?>> arguments;
    private final Set<Requirement<S, ?>> requirements;

    private final MessageRegistry<S> messageRegistry;
    private final ExecutionProvider executionProvider;

    private final SenderValidator<S> senderValidator;

    private final boolean containsLimitless;

    public AbstractSubCommand(
            @NotNull final AbstractSubCommandProcessor<S> processor,
            @NotNull final String parentName,
            @NotNull final ExecutionProvider executionProvider
    ) {
        this.baseCommand = processor.getBaseCommand();
        this.method = processor.getMethod();
        this.name = processor.getName();
        this.alias = processor.getAlias();
        this.arguments = processor.getArguments();
        this.requirements = processor.getRequirements();
        this.messageRegistry = processor.getMessageRegistry();
        this.isDefault = processor.isDefault();
        this.isNamedArguments = processor.isNamedArguments();
        this.senderValidator = processor.getSenderValidator();

        this.senderType = processor.getSenderType();

        this.parentName = parentName;

        this.executionProvider = executionProvider;

        this.containsLimitless = arguments.stream().anyMatch(LimitlessArgument.class::isInstance);
    }

    /**
     * Checks if the sub command is default.
     * Can also just check if the name is {@link Default#DEFAULT_CMD_NAME}.
     *
     * @return Whether the sub command is default.
     */
    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public boolean isNamedArguments() {
        return isNamedArguments;
    }

    // TODO: 2/5/2022 comments
    @NotNull
    @Override
    public Class<? extends S> getSenderType() {
        return senderType;
    }

    /**
     * Gets the name of the parent command.
     *
     * @return The name of the parent command.
     */
    @NotNull
    protected String getParentName() {
        return parentName;
    }

    /**
     * Gets the name of the sub command.
     *
     * @return The name of the sub command.
     */
    @NotNull
    protected String getName() {
        return name;
    }

    /**
     * Gets the message registry.
     *
     * @return The message registry.
     */
    @NotNull
    protected MessageRegistry<S> getMessageRegistry() {
        return messageRegistry;
    }

    /**
     * Executes the sub command.
     *
     * @param sender The sender.
     * @param args   The arguments to pass to the executor.
     */
    @Override
    public void execute(@NotNull final S sender, @NotNull final List<String> args) {
        if (!senderValidator.validate(messageRegistry, this, sender)) return;
        if (!meetRequirements(sender)) return;

        // Creates the invoking arguments list
        final List<Object> invokeArguments = new ArrayList<>();
        invokeArguments.add(sender);

        if (!validateAndCollectArguments(sender, invokeArguments, args)) {
            return;
        }

        if ((!containsLimitless) && args.size() >= invokeArguments.size()) {
            messageRegistry.sendMessage(MessageKey.TOO_MANY_ARGUMENTS, sender, new DefaultMessageContext(parentName, name));
            return;
        }

        executionProvider.execute(() -> {
            try {
                method.invoke(baseCommand, invokeArguments.toArray());
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new CommandExecutionException("An error occurred while executing the command", parentName, name)
                        .initCause(exception instanceof InvocationTargetException ? exception.getCause() : exception);
            }
        });
    }

    /**
     * Gets the arguments of the sub command.
     *
     * @return The arguments of the sub command.
     */
    @NotNull
    protected List<Argument<S, ?>> getArguments() {
        return arguments;
    }

    @Nullable
    protected Argument<S, ?> getArgument(@NotNull final String name) {
        final List<Argument<S, ?>> foundArgs = arguments.stream()
                .filter(argument -> argument.getName().toLowerCase().startsWith(name))
                .collect(Collectors.toList());

        if (foundArgs.size() != 1) return null;
        return foundArgs.get(0);
    }

    // TODO: 2/1/2022 Comments
    public List<@Nullable String> mapArguments(@NotNull final Map<String, String> args) {
        final List<String> arguments = getArguments().stream().map(Argument::getName).collect(Collectors.toList());
        return arguments.stream().map(it -> {
            final String value = args.get(it);
            return value == null ? "" : value;
        }).collect(Collectors.toList());
    }

    /**
     * Used for checking if the arguments are valid and adding them to the `invokeArguments`.
     *
     * @param sender          The sender of the command.
     * @param invokeArguments A list with the arguments that'll be used on the `invoke` of the command method.
     * @param commandArgs     The command arguments type.
     * @return False if any argument fails to pass.
     */
    @SuppressWarnings("unchecked")
    private boolean validateAndCollectArguments(
            @NotNull final S sender,
            @NotNull final List<Object> invokeArguments,
            @NotNull final List<String> commandArgs
    ) {
        for (int i = 0; i < arguments.size(); i++) {
            final Argument<S, ?> argument = arguments.get(i);

            if (argument instanceof LimitlessArgument) {
                final LimitlessArgument<S> limitlessArgument = (LimitlessArgument<S>) argument;
                final List<String> leftOvers = leftOvers(commandArgs, i);

                final Object result = limitlessArgument.resolve(sender, leftOvers);

                if (result == null) {
                    return false;
                }

                invokeArguments.add(result);
                return true;
            }

            if (!(argument instanceof StringArgument)) {
                throw new CommandExecutionException("Found unsupported argument", parentName, name);
            }

            final StringArgument<S> stringArgument = (StringArgument<S>) argument;
            final String arg = valueOrNull(commandArgs, i);

            if (arg == null || arg.isEmpty()) {
                if (argument.isOptional()) {
                    invokeArguments.add(null);
                    continue;
                }

                messageRegistry.sendMessage(MessageKey.NOT_ENOUGH_ARGUMENTS, sender, new DefaultMessageContext(parentName, name));
                return false;
            }

            final Object result = stringArgument.resolve(sender, arg);
            if (result == null) {
                messageRegistry.sendMessage(
                        MessageKey.INVALID_ARGUMENT,
                        sender,
                        new InvalidArgumentContext(parentName, name, arg, argument.getName(), argument.getType())
                );
                return false;
            }

            invokeArguments.add(result);
        }

        return true;
    }

    /**
     * Checks if the requirements to run the command are met.
     *
     * @param sender The sender of the command.
     * @return Whether all requirements are met.
     */
    private boolean meetRequirements(@NotNull final S sender) {
        for (final Requirement<S, ?> requirement : requirements) {
            if (!requirement.isMet(sender)) {
                requirement.sendMessage(messageRegistry, sender, parentName, name);
                return false;
            }
        }

        return true;
    }

    /**
     * Gets an argument value or null.
     *
     * @param list  The list to check from.
     * @param index The current index of the argument.
     * @return The argument name or null.
     */
    @Nullable
    private String valueOrNull(@NotNull final List<String> list, final int index) {
        if (index >= list.size()) return null;
        return list.get(index);
    }

    /**
     * Gets the left over of the arguments.
     *
     * @param list The list with all the arguments.
     * @param from The index from which should start removing.
     * @return A list with the leftover arguments.
     */
    @NotNull
    private List<String> leftOvers(@NotNull final List<String> list, final int from) {
        if (from > list.size()) return Collections.emptyList();
        return list.subList(from, list.size());
    }

    @NotNull
    @Override
    public String toString() {
        return "SimpleSubCommand{" +
                "baseCommand=" + baseCommand +
                ", method=" + method +
                ", name='" + name + '\'' +
                ", alias=" + alias +
                ", isDefault=" + isDefault +
                ", arguments=" + arguments +
                ", requirements=" + requirements +
                ", messageRegistry=" + messageRegistry +
                ", containsLimitlessArgument=" + containsLimitless +
                '}';
    }
}
